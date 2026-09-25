# Phase 12F — Build State

Last updated: 2026-09-25

This file is the durable handoff point for the active Phase 12F implementation.

## Authoritative line

- governed base / completed Phase 12E merge on `main`: `31bfaaffa012cadf2da0c9c31c6da64967d9d24f`
- canonical Phase 12F branch: `phase-12f/membership-invitation-lifecycle`
- canonical draft PR: **#74 — Phase 12F: membership and invitation lifecycle**
- superseded Phase 12F PR: **#73 — closed, not merged, history preserved**
- Level 3: do not merge without explicit operator approval

Do not resume Phase 12F from PR #73 or `phase-12f/owner-member-administration` unless that line is explicitly re-audited.

## Last proven implementation checkpoint

The last runtime/backend implementation head before this documentation-only reconciliation is:

- `a0f12f75d08d596a5bfc53169e879a31fbb392dd`
- Android CI run `36190542857`: **PASS**

Documentation-only commits after that checkpoint do not expand runtime behavior.

## Source-of-truth reconciliation

Dedicated original-FPP Supabase project:
- project ref: `vtyiktvqhbgabawotkrj`
- project: Field Photo Prep

Live migration history currently contains exactly:
- `20260925012939_phase_12c_identity_foundation`
- `20260925013000_phase_12c_invitation_fk_indexes`
- `20260925195816_phase_12f_owner_member_admin`
- `20260925195901_phase_12f_rpc_grant_hardening`

PR #74 source-controls the two already-applied Phase 12F migration files under the exact live migration versions.

Live Edge Function:
- `fpp-owner-invite`
- ACTIVE
- version 1
- JWT verification enabled
- deployed `index.ts` text rechecked against PR #74 source on 2026-09-25: **exact match**

Do not create a second Phase 12F backend or renumber the already-applied Phase 12F migration history.

## Hosted backend evidence already PASS

Using only disposable Phase 12F fixtures:
- final ACTIVE Owner revoke/demotion protection;
- normalized invitation duplicate/idempotent resend behavior;
- explicit pending-role-change requirement;
- wrong-Organization Owner denial;
- revoked Owner denial;
- invitation acceptance identity check;
- repeat acceptance returns the same Membership;
- concurrent last-Owner race leaves exactly one ACTIVE OWNER;
- pending-role update;
- invitation cancellation;
- invitation expiry.

Permanent evidence:
- `docs/PHASE_12F_BACKEND_VERIFICATION_2026-09-25.md`

## Current advisor state

Security advisor:
- INFO: `public.fpp_admin_audit` has RLS enabled with no client policy — intentional server-write-only design;
- WARN: ten authenticated-callable `SECURITY DEFINER` RPCs — intentional public RPC surface only while each operation performs its exact server-side authorization checks;
- WARN: leaked-password protection disabled — known project-plan limitation already recorded.

Performance advisor:
- INFO: four unindexed foreign keys on `fpp_admin_audit`;
- INFO: two currently unused invitation indexes.

These advisor findings are recorded for final 12F/12M review; none is currently treated as proof of a correctness defect.

## Disposable fixture

`Phase 12F Race Fixture` remains intentionally present until the remaining backend evidence is finished. Do not delete it yet.

## Remaining Phase 12F gates

Still required before Phase 12F can be called complete:
1. prove Edge Function delivery-failure visibility and safe retry behavior;
2. prove one real disposable invitation email/deep-link acceptance path;
3. record the final RLS/grant/catalog + advisor snapshot;
4. finish/record focused Android Owner-administration and invitation UI verification;
5. run the appropriate final Android regression on the exact final runtime head;
6. run the smallest required Samsung reality gate;
7. clean disposable backend fixture data after it is no longer needed;
8. obtain explicit Level 3 operator approval before merge.

## Exact next checkpoint

Continue from PR #74 only.

First finish the remaining hosted invitation-delivery/email evidence against the existing backend. Do not redesign or redeploy the backend unless that evidence finds an actual defect.

Then finish the narrow Android Owner-administration/invitation gate and stop at the Level 3 merge decision.

## Working rule

Never modify `main` directly. Preserve the proven photo/Drive workflow. Keep original FPP separate from Field Photo Prep Team. Do not add a second auth, membership, invitation, or Drive-identity system.
