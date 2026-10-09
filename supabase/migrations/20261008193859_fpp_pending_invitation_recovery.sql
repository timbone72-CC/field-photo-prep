-- Verified-account pending invitation recovery.
-- No automatic acceptance: this readonly lookup supplies an ID only for the
-- exact signed-in, email-confirmed recipient; Android asks before calling
-- the existing public.fpp_accept_invitation(uuid).
create or replace function public.fpp_find_my_pending_invitation()
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_actor uuid := (select auth.uid());
    v_email text;
    v_confirmed_at timestamptz;
    v_count integer;
    v_invitation_id uuid;
    v_org_name text;
    v_role text;
begin
    if v_actor is null then
        raise exception 'AUTH_REQUIRED' using errcode = '42501';
    end if;

    select lower(btrim(coalesce(u.email, ''))), u.email_confirmed_at
      into v_email, v_confirmed_at
    from auth.users u
    where u.id = v_actor;

    if v_email is null or v_email = '' or v_confirmed_at is null then
        return jsonb_build_object('outcome', 'EMAIL_NOT_CONFIRMED');
    end if;

    -- Never turn a revoked/existing membership into a new invitation path.
    if exists (select 1 from public.fpp_memberships m where m.user_id = v_actor) then
        return jsonb_build_object('outcome', 'NOT_ELIGIBLE');
    end if;

    select count(*)
      into v_count
    from public.fpp_invitations i
    join public.fpp_organizations o on o.id = i.organization_id
    where i.email = v_email
      and i.status = 'PENDING'
      and i.expires_at > now()
      and o.status = 'ACTIVE';

    if v_count = 0 then
        return jsonb_build_object('outcome', 'NO_PENDING');
    end if;
    if v_count > 1 then
        return jsonb_build_object('outcome', 'MULTIPLE_PENDING');
    end if;

    select i.id, o.name, i.intended_role
      into v_invitation_id, v_org_name, v_role
    from public.fpp_invitations i
    join public.fpp_organizations o on o.id = i.organization_id
    where i.email = v_email
      and i.status = 'PENDING'
      and i.expires_at > now()
      and o.status = 'ACTIVE';

    if v_invitation_id is null then
        return jsonb_build_object('outcome', 'NO_PENDING');
    end if;

    return jsonb_build_object(
        'outcome', 'FOUND',
        'invitation_id', v_invitation_id,
        'organization_name', v_org_name,
        'intended_role', v_role);
end;
$$;

revoke all on function public.fpp_find_my_pending_invitation()
    from public, anon, authenticated;
grant execute on function public.fpp_find_my_pending_invitation()
    to authenticated;
