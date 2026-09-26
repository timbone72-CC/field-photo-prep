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

## Proven implementation checkpoint

The Phase 12F Android/backend runtime checkpoint is:

- runtime/backend implementation commit: `a0f12f75d08d596a5bfc53169e879a31fbb392dd`
- Android CI run `36190542857`: **PASS**

Subsequent governance, CI-workflow and documentation integration did not change Phase 12F Android runtime source. The integrated branch snapshot `05a7c761bd56f41eb4923da87e50349349a53b54` also passed Android CI run `36203850206`.

Exact-head phone-test artifact from run `36203850206`:
- artifact: `field-photo-prep-internal-apk`
- artifact ID: `10892887267`
- workflow artifact digest: `sha256:c1673340bba4ba3a1e761eecf4727be1deb0f8aaf3cbad96d6f499dd807f8059`

## Source-of-truth reconciliation

Dedicated original-FPP Supabase project:
- project ref: `vtyiktvqhbgabawotkrj`
- project: Field Photo Prep
- project status rechecked 2026-09-25: **ACTIVE_HEALTHY**

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
- deployed `index.ts` remains an exact match to PR #74 source

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

## Current live invitation baseline

Immediately before the physical invitation gate:
- persisted `fpp_invitations` rows: **0**
- matching `fpp-owner-invite` / delivery log events in the inspected prior 24-hour window: **0**

The first real phone invitation then produced:
- one persisted MEMBER invitation;
- status `PENDING`;
- delivery status `SENT`;
- delivery attempt count `1`;
- successful HTTP 200 invite and delivery-record calls;
- a received invitation email;
- an approved internal callback containing the exact FPP invitation UUID.

**Real invitation delivery: PASS.**

**Real invitation deep-link acceptance: PASS after hosted redirect allowlist correction.**

The first attempt exposed a valid hosted configuration defect: the dynamic invitation callback fell outside the redirect allowlist and fell back to localhost. The allowlist was corrected by adding `com.inandout.fieldphotoprep.internal://auth-callback*`. The partial disposable Auth user was safely removed, the same FPP invitation was resent, and the full Samsung acceptance path then completed successfully.

Final live state for the reality fixture:
- invitation `ACCEPTED`;
- delivery `SENT`;
- delivery attempts `2`;
- Membership `MEMBER / ACTIVE`.

## Current advisor state

Security advisor rechecked 2026-09-25:
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
2. real disposable invitation email/deep-link acceptance path — **PASS**;
3. record the final RLS/grant/catalog + advisor snapshot;
4. finish/record focused Android Owner-administration and invitation UI verification;
5. run the appropriate final Android regression on the exact final runtime head;
6. run the smallest required Samsung reality gate;
7. clean disposable backend fixture data after it is no longer needed;
8. obtain explicit Level 3 operator approval before merge.

## Exact next checkpoint

Continue from the proven real invitation acceptance state. Next verify that the accepted MEMBER account cannot access Owner administration, then continue the remaining bounded Owner/member administration reality gates.

With the existing Owner account:
1. open the account screen;
2. confirm **Manage Members** is visible;
3. open **Manage Members**;
4. send one MEMBER invitation to a disposable email address the operator can open on that same phone;
5. confirm the invitation row shows its delivery status;
6. open the received invitation link on the phone;
7. set the invited account password and confirm the app finishes invitation activation without borrowing the prior Owner identity.

After that reality result is known, inspect Supabase invitation state/logs, record the result, then run the bounded delivery-failure/retry gate. Do not merge during this checkpoint.

## Working rule

Never modify `main` directly. Preserve the proven photo/Drive workflow. Keep original FPP separate from Field Photo Prep Team. Do not add a second auth, membership, invitation, or Drive-identity system.


## PASS — accepted MEMBER cannot access Owner administration

Physical Samsung check on 2026-09-25:
- accepted identity: `timbone72@gmail.com`;
- Organization: `In And Out Cleaner Inspections LLC`;
- Membership role/status: `MEMBER / ACTIVE`;
- reopening the Account screen showed the account as active;
- **Manage Members** was not present.

This confirms the Android UI does not expose Owner administration to an ACTIVE MEMBER after real invitation acceptance.
