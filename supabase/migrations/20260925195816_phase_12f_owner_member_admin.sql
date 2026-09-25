-- Phase 12F: Owner/member administration.
-- Privileged mutations remain server-authoritative. Android keeps only the publishable key.
-- No Drive, photo, work-order, or provider identity is stored here.

alter table public.fpp_invitations
    add column if not exists delivery_status text not null default 'NOT_SENT'
        check (delivery_status in ('NOT_SENT', 'SENT', 'FAILED')),
    add column if not exists delivery_attempt_count integer not null default 0
        check (delivery_attempt_count >= 0),
    add column if not exists last_delivery_at timestamptz null;

create table if not exists public.fpp_admin_audit (
    id uuid primary key default gen_random_uuid(),
    organization_id uuid not null
        references public.fpp_organizations(id) on delete restrict,
    actor_user_id uuid not null
        references auth.users(id) on delete restrict,
    target_membership_id uuid null
        references public.fpp_memberships(id) on delete set null,
    invitation_id uuid null
        references public.fpp_invitations(id) on delete set null,
    action text not null
        check (action in (
            'INVITATION_PREPARED',
            'INVITATION_ROLE_CHANGED',
            'INVITATION_CANCELLED',
            'INVITATION_DELIVERY',
            'MEMBERSHIP_ROLE_CHANGED',
            'MEMBERSHIP_REVOKED',
            'MEMBERSHIP_REACTIVATED',
            'INVITATION_ACCEPTED'
        )),
    result text not null
        check (result in ('SUCCEEDED', 'IDEMPOTENT', 'REJECTED', 'FAILED')),
    created_at timestamptz not null default now()
);

alter table public.fpp_admin_audit enable row level security;

revoke all on public.fpp_admin_audit from anon, authenticated;

create function private.fpp_lock_active_organization(p_organization_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    perform 1
    from public.fpp_organizations o
    where o.id = p_organization_id
      and o.status = 'ACTIVE'
    for update;

    if not found then
        raise exception 'ORGANIZATION_UNAVAILABLE' using errcode = 'P0001';
    end if;
end;
$$;

create function private.fpp_require_active_owner(p_organization_id uuid)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_actor uuid := (select auth.uid());
    v_membership_id uuid;
begin
    if v_actor is null then
        raise exception 'AUTH_REQUIRED' using errcode = '42501';
    end if;

    select m.id
      into v_membership_id
    from public.fpp_memberships m
    where m.organization_id = p_organization_id
      and m.user_id = v_actor
      and m.status = 'ACTIVE'
      and m.role = 'OWNER';

    if v_membership_id is null then
        raise exception 'OWNER_REQUIRED' using errcode = '42501';
    end if;

    return v_membership_id;
end;
$$;

create function private.fpp_write_admin_audit(
    p_organization_id uuid,
    p_actor_user_id uuid,
    p_target_membership_id uuid,
    p_invitation_id uuid,
    p_action text,
    p_result text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    insert into public.fpp_admin_audit (
        organization_id,
        actor_user_id,
        target_membership_id,
        invitation_id,
        action,
        result)
    values (
        p_organization_id,
        p_actor_user_id,
        p_target_membership_id,
        p_invitation_id,
        p_action,
        p_result);
end;
$$;

create function public.fpp_admin_list_members(p_organization_id uuid)
returns table (
    membership_id uuid,
    user_id uuid,
    email text,
    role text,
    status text)
language plpgsql
security definer
set search_path = ''
as $$
begin
    perform private.fpp_require_active_owner(p_organization_id);

    return query
    select
        m.id,
        m.user_id,
        coalesce(u.email, '')::text,
        m.role,
        m.status
    from public.fpp_memberships m
    join auth.users u on u.id = m.user_id
    where m.organization_id = p_organization_id
    order by
        case when m.role = 'OWNER' then 0 else 1 end,
        lower(coalesce(u.email, '')),
        m.id;
end;
$$;

create function public.fpp_admin_list_invitations(p_organization_id uuid)
returns table (
    invitation_id uuid,
    email text,
    intended_role text,
    status text,
    expires_at timestamptz,
    delivery_status text,
    delivery_attempt_count integer,
    last_delivery_at timestamptz)
language plpgsql
security definer
set search_path = ''
as $$
begin
    perform private.fpp_require_active_owner(p_organization_id);

    return query
    select
        i.id,
        i.email,
        i.intended_role,
        case
            when i.status = 'PENDING' and i.expires_at <= now() then 'EXPIRED'
            else i.status
        end,
        i.expires_at,
        i.delivery_status,
        i.delivery_attempt_count,
        i.last_delivery_at
    from public.fpp_invitations i
    where i.organization_id = p_organization_id
    order by i.created_at desc, i.id;
end;
$$;

create function public.fpp_admin_prepare_invitation(
    p_organization_id uuid,
    p_email text,
    p_intended_role text)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_actor uuid := (select auth.uid());
    v_email text := lower(btrim(coalesce(p_email, '')));
    v_invitation public.fpp_invitations%rowtype;
    v_existing_membership_id uuid;
begin
    if length(v_email) < 3 or position('@' in v_email) <= 1 then
        return jsonb_build_object('outcome', 'INVALID_EMAIL');
    end if;
    if p_intended_role not in ('OWNER', 'MEMBER') then
        return jsonb_build_object('outcome', 'INVALID_ROLE');
    end if;

    perform private.fpp_lock_active_organization(p_organization_id);
    perform private.fpp_require_active_owner(p_organization_id);

    select m.id
      into v_existing_membership_id
    from public.fpp_memberships m
    join auth.users u on u.id = m.user_id
    where m.organization_id = p_organization_id
      and lower(btrim(coalesce(u.email, ''))) = v_email
    limit 1;

    if v_existing_membership_id is not null then
        perform private.fpp_write_admin_audit(
            p_organization_id,
            v_actor,
            v_existing_membership_id,
            null,
            'INVITATION_PREPARED',
            'REJECTED');
        return jsonb_build_object(
            'outcome', 'MEMBERSHIP_EXISTS',
            'membership_id', v_existing_membership_id);
    end if;

    update public.fpp_invitations
       set status = 'EXPIRED'
     where organization_id = p_organization_id
       and email = v_email
       and status = 'PENDING'
       and expires_at <= now();

    select *
      into v_invitation
    from public.fpp_invitations i
    where i.organization_id = p_organization_id
      and i.email = v_email
      and i.status = 'PENDING'
    for update;

    if found then
        if v_invitation.intended_role <> p_intended_role then
            perform private.fpp_write_admin_audit(
                p_organization_id,
                v_actor,
                null,
                v_invitation.id,
                'INVITATION_PREPARED',
                'REJECTED');
            return jsonb_build_object(
                'outcome', 'ROLE_CHANGE_REQUIRED',
                'invitation_id', v_invitation.id,
                'current_role', v_invitation.intended_role);
        end if;

        update public.fpp_invitations
           set expires_at = now() + interval '7 days',
               invited_by_user_id = v_actor,
               delivery_status = 'NOT_SENT'
         where id = v_invitation.id
        returning * into v_invitation;

        perform private.fpp_write_admin_audit(
            p_organization_id,
            v_actor,
            null,
            v_invitation.id,
            'INVITATION_PREPARED',
            'IDEMPOTENT');

        return jsonb_build_object(
            'outcome', 'RESEND_READY',
            'invitation_id', v_invitation.id,
            'email', v_invitation.email,
            'intended_role', v_invitation.intended_role,
            'expires_at', v_invitation.expires_at);
    end if;

    insert into public.fpp_invitations (
        organization_id,
        email,
        intended_role,
        status,
        invited_by_user_id,
        expires_at,
        delivery_status)
    values (
        p_organization_id,
        v_email,
        p_intended_role,
        'PENDING',
        v_actor,
        now() + interval '7 days',
        'NOT_SENT')
    returning * into v_invitation;

    perform private.fpp_write_admin_audit(
        p_organization_id,
        v_actor,
        null,
        v_invitation.id,
        'INVITATION_PREPARED',
        'SUCCEEDED');

    return jsonb_build_object(
        'outcome', 'CREATED',
        'invitation_id', v_invitation.id,
        'email', v_invitation.email,
        'intended_role', v_invitation.intended_role,
        'expires_at', v_invitation.expires_at);
end;
$$;

create function public.fpp_admin_update_invitation_role(
    p_organization_id uuid,
    p_invitation_id uuid,
    p_intended_role text)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_actor uuid := (select auth.uid());
    v_invitation public.fpp_invitations%rowtype;
begin
    if p_intended_role not in ('OWNER', 'MEMBER') then
        return jsonb_build_object('outcome', 'INVALID_ROLE');
    end if;

    perform private.fpp_lock_active_organization(p_organization_id);
    perform private.fpp_require_active_owner(p_organization_id);

    select *
      into v_invitation
    from public.fpp_invitations i
    where i.id = p_invitation_id
      and i.organization_id = p_organization_id
    for update;

    if not found then
        return jsonb_build_object('outcome', 'NOT_FOUND');
    end if;

    if v_invitation.status = 'PENDING' and v_invitation.expires_at <= now() then
        update public.fpp_invitations
           set status = 'EXPIRED'
         where id = v_invitation.id;
        return jsonb_build_object('outcome', 'EXPIRED', 'invitation_id', v_invitation.id);
    end if;

    if v_invitation.status <> 'PENDING' then
        return jsonb_build_object(
            'outcome', 'NOT_PENDING',
            'invitation_id', v_invitation.id,
            'status', v_invitation.status);
    end if;

    if v_invitation.intended_role = p_intended_role then
        perform private.fpp_write_admin_audit(
            p_organization_id,
            v_actor,
            null,
            v_invitation.id,
            'INVITATION_ROLE_CHANGED',
            'IDEMPOTENT');
        return jsonb_build_object(
            'outcome', 'UNCHANGED',
            'invitation_id', v_invitation.id,
            'intended_role', v_invitation.intended_role);
    end if;

    update public.fpp_invitations
       set intended_role = p_intended_role,
           delivery_status = 'NOT_SENT'
     where id = v_invitation.id;

    perform private.fpp_write_admin_audit(
        p_organization_id,
        v_actor,
        null,
        v_invitation.id,
        'INVITATION_ROLE_CHANGED',
        'SUCCEEDED');

    return jsonb_build_object(
        'outcome', 'UPDATED',
        'invitation_id', v_invitation.id,
        'intended_role', p_intended_role);
end;
$$;

create function public.fpp_admin_cancel_invitation(
    p_organization_id uuid,
    p_invitation_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_actor uuid := (select auth.uid());
    v_invitation public.fpp_invitations%rowtype;
begin
    perform private.fpp_lock_active_organization(p_organization_id);
    perform private.fpp_require_active_owner(p_organization_id);

    select *
      into v_invitation
    from public.fpp_invitations i
    where i.id = p_invitation_id
      and i.organization_id = p_organization_id
    for update;

    if not found then
        return jsonb_build_object('outcome', 'NOT_FOUND');
    end if;

    if v_invitation.status = 'CANCELLED' then
        perform private.fpp_write_admin_audit(
            p_organization_id,
            v_actor,
            null,
            v_invitation.id,
            'INVITATION_CANCELLED',
            'IDEMPOTENT');
        return jsonb_build_object('outcome', 'CANCELLED', 'invitation_id', v_invitation.id);
    end if;

    if v_invitation.status <> 'PENDING' then
        return jsonb_build_object(
            'outcome', 'NOT_PENDING',
            'invitation_id', v_invitation.id,
            'status', v_invitation.status);
    end if;

    update public.fpp_invitations
       set status = 'CANCELLED'
     where id = v_invitation.id;

    perform private.fpp_write_admin_audit(
        p_organization_id,
        v_actor,
        null,
        v_invitation.id,
        'INVITATION_CANCELLED',
        'SUCCEEDED');

    return jsonb_build_object('outcome', 'CANCELLED', 'invitation_id', v_invitation.id);
end;
$$;

create function public.fpp_admin_record_invitation_delivery(
    p_organization_id uuid,
    p_invitation_id uuid,
    p_succeeded boolean)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_actor uuid := (select auth.uid());
    v_invitation public.fpp_invitations%rowtype;
    v_delivery_status text;
begin
    perform private.fpp_lock_active_organization(p_organization_id);
    perform private.fpp_require_active_owner(p_organization_id);

    select *
      into v_invitation
    from public.fpp_invitations i
    where i.id = p_invitation_id
      and i.organization_id = p_organization_id
    for update;

    if not found then
        return jsonb_build_object('outcome', 'NOT_FOUND');
    end if;

    v_delivery_status := case when p_succeeded then 'SENT' else 'FAILED' end;

    update public.fpp_invitations
       set delivery_status = v_delivery_status,
           delivery_attempt_count = delivery_attempt_count + 1,
           last_delivery_at = now()
     where id = v_invitation.id;

    perform private.fpp_write_admin_audit(
        p_organization_id,
        v_actor,
        null,
        v_invitation.id,
        'INVITATION_DELIVERY',
        case when p_succeeded then 'SUCCEEDED' else 'FAILED' end);

    return jsonb_build_object(
        'outcome', v_delivery_status,
        'invitation_id', v_invitation.id);
end;
$$;

create function public.fpp_admin_change_membership_role(
    p_organization_id uuid,
    p_membership_id uuid,
    p_role text)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_actor uuid := (select auth.uid());
    v_target public.fpp_memberships%rowtype;
    v_owner_count integer;
begin
    if p_role not in ('OWNER', 'MEMBER') then
        return jsonb_build_object('outcome', 'INVALID_ROLE');
    end if;

    perform private.fpp_lock_active_organization(p_organization_id);
    perform private.fpp_require_active_owner(p_organization_id);

    select *
      into v_target
    from public.fpp_memberships m
    where m.id = p_membership_id
      and m.organization_id = p_organization_id
    for update;

    if not found then
        return jsonb_build_object('outcome', 'NOT_FOUND');
    end if;

    if v_target.role = p_role then
        perform private.fpp_write_admin_audit(
            p_organization_id,
            v_actor,
            v_target.id,
            null,
            'MEMBERSHIP_ROLE_CHANGED',
            'IDEMPOTENT');
        return jsonb_build_object(
            'outcome', 'UNCHANGED',
            'membership_id', v_target.id,
            'role', v_target.role);
    end if;

    if v_target.status = 'ACTIVE'
       and v_target.role = 'OWNER'
       and p_role = 'MEMBER' then
        select count(*)
          into v_owner_count
        from public.fpp_memberships m
        where m.organization_id = p_organization_id
          and m.status = 'ACTIVE'
          and m.role = 'OWNER';

        if v_owner_count <= 1 then
            perform private.fpp_write_admin_audit(
                p_organization_id,
                v_actor,
                v_target.id,
                null,
                'MEMBERSHIP_ROLE_CHANGED',
                'REJECTED');
            return jsonb_build_object(
                'outcome', 'LAST_OWNER_BLOCKED',
                'membership_id', v_target.id);
        end if;
    end if;

    update public.fpp_memberships
       set role = p_role
     where id = v_target.id;

    perform private.fpp_write_admin_audit(
        p_organization_id,
        v_actor,
        v_target.id,
        null,
        'MEMBERSHIP_ROLE_CHANGED',
        'SUCCEEDED');

    return jsonb_build_object(
        'outcome', 'UPDATED',
        'membership_id', v_target.id,
        'role', p_role);
end;
$$;

create function public.fpp_admin_revoke_membership(
    p_organization_id uuid,
    p_membership_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_actor uuid := (select auth.uid());
    v_target public.fpp_memberships%rowtype;
    v_owner_count integer;
begin
    perform private.fpp_lock_active_organization(p_organization_id);
    perform private.fpp_require_active_owner(p_organization_id);

    select *
      into v_target
    from public.fpp_memberships m
    where m.id = p_membership_id
      and m.organization_id = p_organization_id
    for update;

    if not found then
        return jsonb_build_object('outcome', 'NOT_FOUND');
    end if;

    if v_target.status = 'REVOKED' then
        perform private.fpp_write_admin_audit(
            p_organization_id,
            v_actor,
            v_target.id,
            null,
            'MEMBERSHIP_REVOKED',
            'IDEMPOTENT');
        return jsonb_build_object('outcome', 'REVOKED', 'membership_id', v_target.id);
    end if;

    if v_target.status = 'ACTIVE' and v_target.role = 'OWNER' then
        select count(*)
          into v_owner_count
        from public.fpp_memberships m
        where m.organization_id = p_organization_id
          and m.status = 'ACTIVE'
          and m.role = 'OWNER';

        if v_owner_count <= 1 then
            perform private.fpp_write_admin_audit(
                p_organization_id,
                v_actor,
                v_target.id,
                null,
                'MEMBERSHIP_REVOKED',
                'REJECTED');
            return jsonb_build_object(
                'outcome', 'LAST_OWNER_BLOCKED',
                'membership_id', v_target.id);
        end if;
    end if;

    update public.fpp_memberships
       set status = 'REVOKED'
     where id = v_target.id;

    perform private.fpp_write_admin_audit(
        p_organization_id,
        v_actor,
        v_target.id,
        null,
        'MEMBERSHIP_REVOKED',
        'SUCCEEDED');

    return jsonb_build_object('outcome', 'REVOKED', 'membership_id', v_target.id);
end;
$$;

create function public.fpp_admin_reactivate_membership(
    p_organization_id uuid,
    p_membership_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_actor uuid := (select auth.uid());
    v_target public.fpp_memberships%rowtype;
begin
    perform private.fpp_lock_active_organization(p_organization_id);
    perform private.fpp_require_active_owner(p_organization_id);

    select *
      into v_target
    from public.fpp_memberships m
    where m.id = p_membership_id
      and m.organization_id = p_organization_id
    for update;

    if not found then
        return jsonb_build_object('outcome', 'NOT_FOUND');
    end if;

    if v_target.status = 'ACTIVE' then
        perform private.fpp_write_admin_audit(
            p_organization_id,
            v_actor,
            v_target.id,
            null,
            'MEMBERSHIP_REACTIVATED',
            'IDEMPOTENT');
        return jsonb_build_object('outcome', 'ACTIVE', 'membership_id', v_target.id);
    end if;

    update public.fpp_memberships
       set status = 'ACTIVE'
     where id = v_target.id;

    perform private.fpp_write_admin_audit(
        p_organization_id,
        v_actor,
        v_target.id,
        null,
        'MEMBERSHIP_REACTIVATED',
        'SUCCEEDED');

    return jsonb_build_object('outcome', 'ACTIVE', 'membership_id', v_target.id);
end;
$$;

create function public.fpp_accept_invitation(p_invitation_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_actor uuid := (select auth.uid());
    v_actor_email text;
    v_email_confirmed_at timestamptz;
    v_organization_id uuid;
    v_invitation public.fpp_invitations%rowtype;
    v_membership public.fpp_memberships%rowtype;
begin
    if v_actor is null then
        raise exception 'AUTH_REQUIRED' using errcode = '42501';
    end if;

    select lower(btrim(coalesce(u.email, ''))), u.email_confirmed_at
      into v_actor_email, v_email_confirmed_at
    from auth.users u
    where u.id = v_actor;

    if v_actor_email is null or v_actor_email = '' or v_email_confirmed_at is null then
        return jsonb_build_object('outcome', 'EMAIL_NOT_CONFIRMED');
    end if;

    select i.organization_id
      into v_organization_id
    from public.fpp_invitations i
    where i.id = p_invitation_id;

    if v_organization_id is null then
        return jsonb_build_object('outcome', 'INVITATION_UNAVAILABLE');
    end if;

    perform private.fpp_lock_active_organization(v_organization_id);

    select *
      into v_invitation
    from public.fpp_invitations i
    where i.id = p_invitation_id
      and i.organization_id = v_organization_id
    for update;

    if not found then
        return jsonb_build_object('outcome', 'INVITATION_UNAVAILABLE');
    end if;

    if v_invitation.email <> v_actor_email then
        return jsonb_build_object('outcome', 'INVITATION_UNAVAILABLE');
    end if;

    if v_invitation.auth_user_id is not null
       and v_invitation.auth_user_id <> v_actor then
        return jsonb_build_object('outcome', 'INVITATION_UNAVAILABLE');
    end if;

    if v_invitation.status = 'PENDING' and v_invitation.expires_at <= now() then
        update public.fpp_invitations
           set status = 'EXPIRED'
         where id = v_invitation.id;
        return jsonb_build_object('outcome', 'EXPIRED');
    end if;

    if v_invitation.status = 'CANCELLED' or v_invitation.status = 'EXPIRED' then
        return jsonb_build_object('outcome', v_invitation.status);
    end if;

    select *
      into v_membership
    from public.fpp_memberships m
    where m.organization_id = v_organization_id
      and m.user_id = v_actor
    for update;

    if v_invitation.status = 'ACCEPTED' then
        if v_membership.id is null then
            return jsonb_build_object('outcome', 'INVITATION_UNAVAILABLE');
        end if;
        return jsonb_build_object(
            'outcome', 'ALREADY_ACCEPTED',
            'membership_id', v_membership.id,
            'organization_id', v_membership.organization_id,
            'role', v_membership.role,
            'status', v_membership.status);
    end if;

    if v_membership.id is null then
        insert into public.fpp_memberships (
            organization_id,
            user_id,
            role,
            status)
        values (
            v_organization_id,
            v_actor,
            v_invitation.intended_role,
            'ACTIVE')
        returning * into v_membership;
    elsif v_membership.status <> 'ACTIVE' then
        update public.fpp_memberships
           set role = v_invitation.intended_role,
               status = 'ACTIVE'
         where id = v_membership.id
        returning * into v_membership;
    end if;

    update public.fpp_invitations
       set status = 'ACCEPTED',
           auth_user_id = v_actor
     where id = v_invitation.id;

    perform private.fpp_write_admin_audit(
        v_organization_id,
        v_actor,
        v_membership.id,
        v_invitation.id,
        'INVITATION_ACCEPTED',
        case
            when v_membership.role = v_invitation.intended_role then 'SUCCEEDED'
            else 'IDEMPOTENT'
        end);

    return jsonb_build_object(
        'outcome', 'ACCEPTED',
        'membership_id', v_membership.id,
        'organization_id', v_membership.organization_id,
        'role', v_membership.role,
        'status', v_membership.status);
end;
$$;

revoke all on function private.fpp_lock_active_organization(uuid) from public;
revoke all on function private.fpp_require_active_owner(uuid) from public;
revoke all on function private.fpp_write_admin_audit(uuid, uuid, uuid, uuid, text, text) from public;

revoke all on function public.fpp_admin_list_members(uuid) from public;
revoke all on function public.fpp_admin_list_invitations(uuid) from public;
revoke all on function public.fpp_admin_prepare_invitation(uuid, text, text) from public;
revoke all on function public.fpp_admin_update_invitation_role(uuid, uuid, text) from public;
revoke all on function public.fpp_admin_cancel_invitation(uuid, uuid) from public;
revoke all on function public.fpp_admin_record_invitation_delivery(uuid, uuid, boolean) from public;
revoke all on function public.fpp_admin_change_membership_role(uuid, uuid, text) from public;
revoke all on function public.fpp_admin_revoke_membership(uuid, uuid) from public;
revoke all on function public.fpp_admin_reactivate_membership(uuid, uuid) from public;
revoke all on function public.fpp_accept_invitation(uuid) from public;

grant execute on function public.fpp_admin_list_members(uuid) to authenticated;
grant execute on function public.fpp_admin_list_invitations(uuid) to authenticated;
grant execute on function public.fpp_admin_prepare_invitation(uuid, text, text) to authenticated;
grant execute on function public.fpp_admin_update_invitation_role(uuid, uuid, text) to authenticated;
grant execute on function public.fpp_admin_cancel_invitation(uuid, uuid) to authenticated;
grant execute on function public.fpp_admin_record_invitation_delivery(uuid, uuid, boolean) to authenticated;
grant execute on function public.fpp_admin_change_membership_role(uuid, uuid, text) to authenticated;
grant execute on function public.fpp_admin_revoke_membership(uuid, uuid) to authenticated;
grant execute on function public.fpp_admin_reactivate_membership(uuid, uuid) to authenticated;
grant execute on function public.fpp_accept_invitation(uuid) to authenticated;
