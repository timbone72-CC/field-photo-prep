-- Phase 12F hardening: make anonymous RPC execution impossible.
-- Authenticated execution remains intentional; every admin RPC re-checks exact server-side authority.

revoke execute on function private.fpp_lock_active_organization(uuid) from anon, authenticated;
revoke execute on function private.fpp_require_active_owner(uuid) from anon, authenticated;
revoke execute on function private.fpp_write_admin_audit(uuid, uuid, uuid, uuid, text, text)
    from anon, authenticated;

revoke execute on function public.fpp_admin_list_members(uuid) from anon;
revoke execute on function public.fpp_admin_list_invitations(uuid) from anon;
revoke execute on function public.fpp_admin_prepare_invitation(uuid, text, text) from anon;
revoke execute on function public.fpp_admin_update_invitation_role(uuid, uuid, text) from anon;
revoke execute on function public.fpp_admin_cancel_invitation(uuid, uuid) from anon;
revoke execute on function public.fpp_admin_record_invitation_delivery(uuid, uuid, boolean) from anon;
revoke execute on function public.fpp_admin_change_membership_role(uuid, uuid, text) from anon;
revoke execute on function public.fpp_admin_revoke_membership(uuid, uuid) from anon;
revoke execute on function public.fpp_admin_reactivate_membership(uuid, uuid) from anon;
revoke execute on function public.fpp_accept_invitation(uuid) from anon;

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
