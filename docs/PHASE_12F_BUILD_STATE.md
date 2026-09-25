# Phase 12F — Build State

Last updated: 2026-09-25

This file is the durable handoff point for Phase 12F.

## Governed base

- main / Phase 12E merge: `31bfaaffa012cadf2da0c9c31c6da64967d9d24f`
- branch: `phase-12f/membership-invitation-lifecycle`
- Level 3: do not merge without explicit operator approval

## Preflight reconciliation finding

The live FPP Supabase backend is ahead of the repository.

Live project `vtyiktvqhbgabawotkrj` currently reports these migrations:
- `20260925012939_phase_12c_identity_foundation`
- `20260925013000_phase_12c_invitation_fk_indexes`
- `20260925195816_phase_12f_owner_member_admin`
- `20260925195901_phase_12f_rpc_grant_hardening`

But `main` contains only the two Phase 12C migration files.

The live project also has active Edge Function:
- `fpp-owner-invite`, JWT verification enabled

Do not write a second 12F backend. Reconcile the already-deployed backend into source control first.

## Live server behavior already observed

Present server RPCs:
- `fpp_admin_list_members`
- `fpp_admin_list_invitations`
- `fpp_admin_prepare_invitation`
- `fpp_admin_update_invitation_role`
- `fpp_admin_cancel_invitation`
- `fpp_admin_record_invitation_delivery`
- `fpp_admin_change_membership_role`
- `fpp_admin_revoke_membership`
- `fpp_admin_reactivate_membership`
- `fpp_accept_invitation`

Private helpers:
- `private.fpp_lock_active_organization`
- `private.fpp_require_active_owner`
- `private.fpp_write_admin_audit`

Current table grants remain SELECT-only for authenticated on Organizations/Memberships/Invitations. Public mutation is through narrow RPC execution grants.

## Known live fixture

A disposable Organization named `Phase 12F Race Fixture` exists alongside the real Organization. It has one active Owner and one admin-audit row from a Membership revocation test.

Do not delete it until backend evidence is reconciled.

## Exact next checkpoint

1. Reconstruct and source-control the two applied Phase 12F migration files from the verified live catalog.
2. Source-control the deployed `fpp-owner-invite` Edge Function.
3. Verify function ACLs, policies, constraints, indexes, and advisors match the source-controlled reconstruction.
4. Record any live/server defect before Android UI work.
5. Only then continue to narrow Android Owner administration and invitation acceptance UI.

## Working rule

Use small checkpoints. Commit each bounded concern. Never modify `main` directly. Do not deploy a replacement server path merely because source control was stale.
