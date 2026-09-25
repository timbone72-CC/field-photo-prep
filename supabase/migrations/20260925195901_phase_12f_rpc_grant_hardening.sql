-- Reconstructed from the live Field Photo Prep function ACLs.
-- This migration version is already applied to project vtyiktvqhbgabawotkrj.

revoke all on function private.fpp_lock_active_organization(uuid) from public, anon, authenticated;
revoke all on function private.fpp_require_active_owner(uuid) from public, anon, authenticated;
revoke all on function private.fpp_write_admin_audit(uuid, uuid, uuid, uuid, text, text)
    from public, anon, authenticated;

revoke all on function public.fpp_admin_list_members(uuid) from public, anon;
revoke all on function public.fpp_admin_list_invitations(uuid) from public, anon;
revoke all on function public.fpp_admin_prepare_invitation(uuid, text, text) from public, anon;
revoke all on function public.fpp_admin_update_invitation_role(uuid, uuid, text) from public, anon;
revoke all on function public.fpp_admin_cancel_invitation(uuid, uuid) from public, anon;
revoke all on function public.fpp_admin_record_invitation_delivery(uuid, uuid, boolean) from public, anon;
revoke all on function public.fpp_admin_change_membership_role(uuid, uuid, text) from public, anon;
revoke all on function public.fpp_admin_revoke_membership(uuid, uuid) from public, anon;
revoke all on function public.fpp_admin_reactivate_membership(uuid, uuid) from public, anon;
revoke all on function public.fpp_accept_invitation(uuid) from public, anon;

grant execute on function public.fpp_admin_list_members(uuid) to authenticated, service_role;
grant execute on function public.fpp_admin_list_invitations(uuid) to authenticated, service_role;
grant execute on function public.fpp_admin_prepare_invitation(uuid, text, text) to authenticated, service_role;
grant execute on function public.fpp_admin_update_invitation_role(uuid, uuid, text) to authenticated, service_role;
grant execute on function public.fpp_admin_cancel_invitation(uuid, uuid) to authenticated, service_role;
grant execute on function public.fpp_admin_record_invitation_delivery(uuid, uuid, boolean) to authenticated, service_role;
grant execute on function public.fpp_admin_change_membership_role(uuid, uuid, text) to authenticated, service_role;
grant execute on function public.fpp_admin_revoke_membership(uuid, uuid) to authenticated, service_role;
grant execute on function public.fpp_admin_reactivate_membership(uuid, uuid) to authenticated, service_role;
grant execute on function public.fpp_accept_invitation(uuid) to authenticated, service_role;
