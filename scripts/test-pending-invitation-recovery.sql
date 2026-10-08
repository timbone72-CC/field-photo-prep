-- Tests run ONLY against GitHub Actions' isolated PostgreSQL service.
-- Auth simulation: role=authenticated/anon + request.jwt.claim.sub.
-- This reproduces SQL role/claim semantics, NOT Supabase token signature checks.
\set ON_ERROR_STOP on
\echo 'Starting isolated FPP invitation recovery SQL contract tests'

-- All identities are disposable, test-local values; never use real mailboxes.
insert into auth.users (id, email, email_confirmed_at) values
('10000000-0000-4000-8000-000000000001', 'owner@fixture.invalid', now()),
('10000000-0000-4000-8000-000000000002', 'invitee@fixture.invalid', now()),
('10000000-0000-4000-8000-000000000003', 'stranger@fixture.invalid', now()),
('10000000-0000-4000-8000-000000000004', 'unconfirmed@fixture.invalid', null),
('10000000-0000-4000-8000-000000000005', 'multi@fixture.invalid', now()),
('10000000-0000-4000-8000-000000000006', 'revoked@fixture.invalid', now()),
('10000000-0000-4000-8000-000000000007', 'existing@fixture.invalid', now()),
('10000000-0000-4000-8000-000000000008', 'cancelled@fixture.invalid', now()),
('10000000-0000-4000-8000-000000000009', 'expired@fixture.invalid', now()),
('10000000-0000-4000-8000-000000000010', 'closed@fixture.invalid', now()),
('10000000-0000-4000-8000-000000000011', 'former-invite@fixture.invalid', now());

insert into public.fpp_organizations (id, name, status) values
('20000000-0000-4000-8000-000000000001', 'Fixture Active Organization', 'ACTIVE'),
('20000000-0000-4000-8000-000000000002', 'Fixture Other Organization', 'ACTIVE'),
('20000000-0000-4000-8000-000000000003', 'Fixture Closed Organization', 'CLOSED');

insert into public.fpp_memberships
(organization_id, user_id, role, status) values
('20000000-0000-4000-8000-000000000001','10000000-0000-4000-8000-000000000001','OWNER','ACTIVE'),
('20000000-0000-4000-8000-000000000001','10000000-0000-4000-8000-000000000006','MEMBER','REVOKED'),
('20000000-0000-4000-8000-000000000001','10000000-0000-4000-8000-000000000007','MEMBER','ACTIVE');

insert into public.fpp_invitations
(id, organization_id, email, intended_role, status, invited_by_user_id, expires_at)
values
('30000000-0000-4000-8000-000000000001','20000000-0000-4000-8000-000000000001','invitee@fixture.invalid','MEMBER','PENDING','10000000-0000-4000-8000-000000000001',now()+interval '2 days'),
('30000000-0000-4000-8000-000000000002','20000000-0000-4000-8000-000000000001','unconfirmed@fixture.invalid','MEMBER','PENDING','10000000-0000-4000-8000-000000000001',now()+interval '2 days'),
('30000000-0000-4000-8000-000000000003','20000000-0000-4000-8000-000000000001','multi@fixture.invalid','MEMBER','PENDING','10000000-0000-4000-8000-000000000001',now()+interval '2 days'),
('30000000-0000-4000-8000-000000000004','20000000-0000-4000-8000-000000000002','multi@fixture.invalid','OWNER','PENDING','10000000-0000-4000-8000-000000000001',now()+interval '2 days'),
('30000000-0000-4000-8000-000000000005','20000000-0000-4000-8000-000000000001','revoked@fixture.invalid','MEMBER','PENDING','10000000-0000-4000-8000-000000000001',now()+interval '2 days'),
('30000000-0000-4000-8000-000000000006','20000000-0000-4000-8000-000000000001','existing@fixture.invalid','MEMBER','PENDING','10000000-0000-4000-8000-000000000001',now()+interval '2 days'),
('30000000-0000-4000-8000-000000000007','20000000-0000-4000-8000-000000000001','cancelled@fixture.invalid','MEMBER','CANCELLED','10000000-0000-4000-8000-000000000001',now()+interval '2 days'),
('30000000-0000-4000-8000-000000000008','20000000-0000-4000-8000-000000000001','expired@fixture.invalid','MEMBER','PENDING','10000000-0000-4000-8000-000000000001',now()-interval '1 hour'),
('30000000-0000-4000-8000-000000000009','20000000-0000-4000-8000-000000000003','closed@fixture.invalid','MEMBER','PENDING','10000000-0000-4000-8000-000000000001',now()+interval '2 days'),
('30000000-0000-4000-8000-000000000010','20000000-0000-4000-8000-000000000001','former-invite@fixture.invalid','MEMBER','EXPIRED','10000000-0000-4000-8000-000000000001',now()+interval '2 days');

-- Catalog security: anonymous has no execute, authenticated has it, tables stay read-only.
do $$
begin
    if has_function_privilege('anon', 'public.fpp_find_my_pending_invitation()', 'EXECUTE')
       or not has_function_privilege('authenticated','public.fpp_find_my_pending_invitation()','EXECUTE')
       or has_table_privilege('authenticated','public.fpp_invitations','INSERT')
       or has_table_privilege('authenticated','public.fpp_memberships','UPDATE')
       or exists (
           select 1 from pg_class c join pg_namespace n on n.oid=c.relnamespace
           where n.nspname='public'
             and c.relname in ('fpp_invitations','fpp_memberships','fpp_organizations')
             and not c.relrowsecurity
       )
    then raise exception 'FAIL: function or table grants/RLS boundary'; end if;
    raise notice 'PASS: grants and RLS boundary';
end $$;

-- The API invocation is rejected for anonymous role, even when a forged claim is set.
set role anon;
select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000000002',false);
do $$
declare denied boolean:=false;
begin
    begin
        perform public.fpp_find_my_pending_invitation();
    exception when insufficient_privilege then denied:=true;
    end;
    if not denied then raise exception 'FAIL: anonymous execution allowed'; end if;
    raise notice 'PASS: anonymous execution denied';
end $$;
reset role;

-- Signed-in role but no authenticated subject still fails closed.
set role authenticated;
select set_config('request.jwt.claim.sub','',false);
do $$
declare denied boolean:=false;
begin
    begin
        perform public.fpp_find_my_pending_invitation();
    exception when insufficient_privilege then denied:=true;
    end;
    if not denied then raise exception 'FAIL: missing subject accepted'; end if;
    raise notice 'PASS: no-subject request denied';
end $$;

select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000000003',false);
do $$
begin
    if (public.fpp_find_my_pending_invitation()->>'outcome') <> 'NO_PENDING'
    then raise exception 'FAIL: other user could view invitation'; end if;
    raise notice 'PASS: other email denied';
end $$;

select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000000004',false);
do $$
begin
    if (public.fpp_find_my_pending_invitation()->>'outcome') <> 'EMAIL_NOT_CONFIRMED'
    then raise exception 'FAIL: unconfirmed email allowed'; end if;
    raise notice 'PASS: unconfirmed email blocked';
end $$;

select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000000005',false);
do $$
begin
    if (public.fpp_find_my_pending_invitation()->>'outcome') <> 'MULTIPLE_PENDING'
    then raise exception 'FAIL: ambiguous multi-org invitation allowed'; end if;
    raise notice 'PASS: multiple pending invites blocked';
end $$;

select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000000006',false);
do $$
begin
    if (public.fpp_find_my_pending_invitation()->>'outcome') <> 'NOT_ELIGIBLE'
    then raise exception 'FAIL: revoked membership allowed'; end if;
    raise notice 'PASS: revoked member blocked';
end $$;

select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000000007',false);
do $$
begin
    if (public.fpp_find_my_pending_invitation()->>'outcome') <> 'NOT_ELIGIBLE'
    then raise exception 'FAIL: existing membership allowed'; end if;
    raise notice 'PASS: existing member blocked';
end $$;

select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000000008',false);
do $$
begin
    if (public.fpp_find_my_pending_invitation()->>'outcome') <> 'NO_PENDING'
    then raise exception 'FAIL: cancelled invitation visible'; end if;
    raise notice 'PASS: cancelled invitation hidden';
end $$;

select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000000009',false);
do $$
begin
    if (public.fpp_find_my_pending_invitation()->>'outcome') <> 'NO_PENDING'
    then raise exception 'FAIL: expired invitation visible'; end if;
    raise notice 'PASS: expired invitation hidden';
end $$;

select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000000010',false);
do $$
begin
    if (public.fpp_find_my_pending_invitation()->>'outcome') <> 'NO_PENDING'
    then raise exception 'FAIL: inactive organization invitation visible'; end if;
    raise notice 'PASS: closed organization hidden';
end $$;

select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000000011',false);
do $$
begin
    if (public.fpp_find_my_pending_invitation()->>'outcome') <> 'NO_PENDING'
    then raise exception 'FAIL: explicitly expired invitation visible'; end if;
    raise notice 'PASS: explicitly expired invitation hidden';
end $$;

select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000000002',false);
do $$
declare details jsonb;
begin
    details := public.fpp_find_my_pending_invitation();
    if details->>'outcome' is distinct from 'FOUND'
       or details->>'invitation_id' is distinct from '30000000-0000-4000-8000-000000000001'
       or details->>'organization_name' is distinct from 'Fixture Active Organization'
       or details->>'intended_role' is distinct from 'MEMBER'
    then raise exception 'FAIL: verified invitee did not receive exact own invitation: %',details; end if;
    raise notice 'PASS: verified invitee sees only own pending invitation';
end $$;
reset role;

-- Merely looking up must not activate any user, change invite status, or write an audit.
do $$
begin
    if (select count(*) from public.fpp_memberships where user_id='10000000-0000-4000-8000-000000000002') <> 0
       or (select status from public.fpp_invitations where id='30000000-0000-4000-8000-000000000001') <> 'PENDING'
       or (select count(*) from public.fpp_admin_audit) <> 0
    then raise exception 'FAIL: lookup mutated membership, invitation, or audit'; end if;
    raise notice 'PASS: invitation lookup is read-only';
end $$;

-- Acceptance itself must be an explicit second call through the existing RPC.
set role authenticated;
select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000000002',false);
do $$
declare res jsonb;
begin
    res := public.fpp_accept_invitation('30000000-0000-4000-8000-000000000001');
    if res->>'outcome' is distinct from 'ACCEPTED'
       or res->>'role' is distinct from 'MEMBER'
       or res->>'status' is distinct from 'ACTIVE'
    then raise exception 'FAIL: explicit acceptance failed: %',res; end if;
    if (public.fpp_find_my_pending_invitation()->>'outcome') <> 'NOT_ELIGIBLE'
    then raise exception 'FAIL: accepted member still offered pending invite'; end if;
    if (public.fpp_accept_invitation('30000000-0000-4000-8000-000000000001')->>'outcome') <> 'ALREADY_ACCEPTED'
    then raise exception 'FAIL: existing acceptance not idempotent'; end if;
    raise notice 'PASS: explicit acceptance and idempotency';
end $$;
reset role;

do $$
begin
    if (select count(*) from public.fpp_memberships
        where user_id='10000000-0000-4000-8000-000000000002'
          and status='ACTIVE' and role='MEMBER') <> 1
        or (select status from public.fpp_invitations
            where id='30000000-0000-4000-8000-000000000001') <> 'ACCEPTED'
        or (select count(*) from public.fpp_admin_audit) <> 1
    then raise exception 'FAIL: membership/invitation/audit completion invariant'; end if;
    raise notice 'PASS: single active membership, accepted invitation, single audit';
end $$;

\echo 'PASS: isolated pending-invitation recovery SQL contract tests'
