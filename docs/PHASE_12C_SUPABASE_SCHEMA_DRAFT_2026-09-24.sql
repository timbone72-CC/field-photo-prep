-- Phase 12C identity foundation schema.
-- Applied to dedicated FPP Supabase project vtyiktvqhbgabawotkrj as migration 20260925012939.
-- No Team project may receive this SQL.

create schema if not exists private;
revoke all on schema private from public;

create table public.fpp_organizations (
    id uuid primary key default gen_random_uuid(),
    name text not null check (btrim(name) <> ''),
    status text not null default 'ACTIVE'
        check (status in ('ACTIVE', 'CLOSED')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table public.fpp_memberships (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null
        references public.fpp_organizations(id) on delete restrict,
    user_id uuid not null
        references auth.users(id) on delete restrict,
    role text not null
        check (role in ('OWNER', 'MEMBER')),
    status text not null
        check (status in ('INVITED', 'ACTIVE', 'REVOKED')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (organization_id, user_id)
);

create table public.fpp_invitations (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null
        references public.fpp_organizations(id) on delete restrict,
    email text not null
        check (email = lower(btrim(email)) and length(email) >= 3),
    intended_role text not null
        check (intended_role in ('OWNER', 'MEMBER')),
    status text not null default 'PENDING'
        check (status in ('PENDING', 'ACCEPTED', 'CANCELLED', 'EXPIRED')),
    auth_user_id uuid null
        references auth.users(id) on delete set null,
    invited_by_user_id uuid not null
        references auth.users(id) on delete restrict,
    expires_at timestamptz not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create unique index fpp_one_pending_invitation_per_email
    on public.fpp_invitations (organization_id, email)
    where status = 'PENDING';

create index fpp_memberships_user_idx
    on public.fpp_memberships (user_id);

create index fpp_invitations_org_idx
    on public.fpp_invitations (organization_id);

create index fpp_invitations_auth_user_idx
    on public.fpp_invitations (auth_user_id)
    where auth_user_id is not null;

create index fpp_invitations_invited_by_user_idx
    on public.fpp_invitations (invited_by_user_id);

create function private.fpp_set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.updated_at := now();
    return new;
end;
$$;

create trigger fpp_organizations_set_updated_at
before update on public.fpp_organizations
for each row execute function private.fpp_set_updated_at();

create trigger fpp_memberships_set_updated_at
before update on public.fpp_memberships
for each row execute function private.fpp_set_updated_at();

create trigger fpp_invitations_set_updated_at
before update on public.fpp_invitations
for each row execute function private.fpp_set_updated_at();

create function private.fpp_is_active_member(target_organization_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select
        (select auth.uid()) is not null
        and exists (
            select 1
            from public.fpp_memberships m
            where m.organization_id = target_organization_id
              and m.user_id = (select auth.uid())
              and m.status = 'ACTIVE'
        );
$$;

create function private.fpp_is_active_owner(target_organization_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select
        (select auth.uid()) is not null
        and exists (
            select 1
            from public.fpp_memberships m
            where m.organization_id = target_organization_id
              and m.user_id = (select auth.uid())
              and m.status = 'ACTIVE'
              and m.role = 'OWNER'
        );
$$;

revoke all on function private.fpp_set_updated_at() from public;
revoke all on function private.fpp_is_active_member(uuid) from public;
revoke all on function private.fpp_is_active_owner(uuid) from public;

grant usage on schema private to authenticated;
grant execute on function private.fpp_is_active_member(uuid) to authenticated;
grant execute on function private.fpp_is_active_owner(uuid) to authenticated;

alter table public.fpp_organizations enable row level security;
alter table public.fpp_memberships enable row level security;
alter table public.fpp_invitations enable row level security;

create policy fpp_organizations_select_active_member
on public.fpp_organizations
for select
to authenticated
using (private.fpp_is_active_member(id));

create policy fpp_memberships_select_self_or_owner
on public.fpp_memberships
for select
to authenticated
using (
    user_id = (select auth.uid())
    or private.fpp_is_active_owner(organization_id)
);

create policy fpp_invitations_select_owner
on public.fpp_invitations
for select
to authenticated
using (private.fpp_is_active_owner(organization_id));

revoke all on public.fpp_organizations from anon, authenticated;
revoke all on public.fpp_memberships from anon, authenticated;
revoke all on public.fpp_invitations from anon, authenticated;

grant select on public.fpp_organizations to authenticated;
grant select on public.fpp_memberships to authenticated;
grant select on public.fpp_invitations to authenticated;

-- No authenticated INSERT/UPDATE/DELETE grants or policies are created in Phase 12C.
-- Privileged mutations remain trusted-server/admin operations in later slices.
