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
1. Edge Function delivery-failure visibility and safe retry behavior — **PASS**;
2. real disposable invitation email/deep-link acceptance path — **PASS**;
3. final RLS/grant/catalog + advisor snapshot — **PASS**;
4. focused Android Owner-administration and invitation UI verification — **PASS**;
5. final Android regression on the current Phase 12F runtime content — **PASS**, Android CI run `36241181047` at branch snapshot `2772ebdcc9165c2b6aae6c30f875a1c87f185a7c`;
6. smallest required Samsung reality gate — **PASS**;
7. clean the remaining disposable `Phase 12F Race Fixture` after preserving its evidence;
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


## PASS — MEMBER sign-out preserves field workspace

Physical Samsung check on 2026-09-26:
- signed out from the accepted ACTIVE MEMBER account;
- app returned to the Field Photo Prep sign-in screen;
- app explicitly reported that the Drive workspace and local field data were left unchanged.

This confirms the identity transition did not destructively reset the existing field workspace.


## PASS — Owner access restored after MEMBER sign-out

Physical Samsung check on 2026-09-26:
- MEMBER account signed out successfully;
- original Owner account `inandoutinspections2026@gmail.com` signed back in successfully;
- Organization resolved correctly to `In And Out Cleaner Inspections LLC`;
- role resolved correctly to `OWNER`;
- **Manage Members** was visible again.

This confirms the Owner/member identity transition restores Owner-only administration without carrying the prior MEMBER authorization state forward.


## PASS — Owner member list reconciles accepted invitation

Physical Samsung check on 2026-09-26:
- Owner administration showed the original Owner as `OWNER / ACTIVE`;
- the accepted invited account showed as `MEMBER / ACTIVE`;
- the corresponding invitation showed `MEMBER / ACCEPTED / delivery SENT`;
- no duplicate active Membership was displayed.

This confirms the Owner administration screen reconciles accepted invitation state into the active Membership list correctly.


## PASS — Owner promotes MEMBER to OWNER

Physical Samsung check on 2026-09-26:
- original Owner remained `OWNER / ACTIVE`;
- `timbone72@gmail.com` changed from `MEMBER / ACTIVE` to `OWNER / ACTIVE`;
- Owner administration refreshed to show both active Owners;
- live backend verification confirmed exactly two ACTIVE OWNER Memberships for the two tested identities.

This confirms the Owner role-promotion path updates both Android UI and hosted Membership state consistently.


## PASS — Owner demotes OWNER back to MEMBER

Physical Samsung check on 2026-09-26:
- `timbone72@gmail.com` changed from `OWNER / ACTIVE` back to `MEMBER / ACTIVE`;
- original Owner remained `OWNER / ACTIVE`;
- Owner administration refreshed to the expected role controls;
- live backend verification matched the Android UI exactly.

This confirms the reversible Owner-to-Member role-change path works without disturbing the remaining active Owner.


## PASS — Owner revokes MEMBER

Physical Samsung check on 2026-09-26:
- `timbone72@gmail.com` changed from `MEMBER / ACTIVE` to `MEMBER / REVOKED`;
- Owner administration replaced role/revoke controls with **REACTIVATE** for the revoked Membership;
- original Owner remained `OWNER / ACTIVE`;
- live backend verification matched the Android UI exactly.

This confirms the Owner revocation path disables the Membership without deleting the identity or accepted invitation record.


## PASS — Owner reactivates revoked MEMBER

Physical Samsung check on 2026-09-26:
- `timbone72@gmail.com` changed from `MEMBER / REVOKED` back to `MEMBER / ACTIVE`;
- original Owner remained `OWNER / ACTIVE`;
- Owner administration restored the expected active-member role and revoke controls;
- live backend verification matched the Android UI exactly.

This confirms revocation is reversible through the intended Owner-only reactivation path without recreating the Membership or invitation.


## PASS — real-device last-Owner revoke protection

Physical Samsung check on 2026-09-26:
- organization state before the attempt:
  - `inandoutinspections2026@gmail.com` = `OWNER / ACTIVE`;
  - `timbone72@gmail.com` = `MEMBER / ACTIVE`;
- Owner tapped **REVOKE** on the original Owner Membership;
- Android administration screen remained unchanged;
- live backend state remained exactly one ACTIVE OWNER and one ACTIVE MEMBER;
- `fpp_admin_audit` recorded `MEMBERSHIP_REVOKED / REJECTED` for the attempted Owner revocation.

This confirms the real-device Owner administration path cannot revoke the final active Owner.


## PASS — real-device last-Owner demotion protection

Physical Samsung check on 2026-09-26:
- organization state before the attempt:
  - `inandoutinspections2026@gmail.com` = `OWNER / ACTIVE`;
  - `timbone72@gmail.com` = `MEMBER / ACTIVE`;
- Owner tapped **MAKE MEMBER** on the original Owner Membership;
- Android administration screen remained unchanged;
- live backend state remained exactly one ACTIVE OWNER and one ACTIVE MEMBER;
- `fpp_admin_audit` recorded `MEMBERSHIP_ROLE_CHANGED / REJECTED` for the attempted demotion.

This confirms the real-device Owner administration path cannot demote the final active Owner.


## PASS — invitation delivery failure remains visible and retryable

Hosted disposable Race Fixture check on 2026-09-26:
- created one disposable MEMBER invitation in `Phase 12F Race Fixture`;
- first failed delivery record produced:
  - invitation status `PENDING`;
  - delivery status `FAILED`;
  - delivery attempt count `1`;
- same-role resend preparation returned `RESEND_READY`;
- resend reused the exact same invitation UUID;
- resend reset delivery state to `NOT_SENT` without resetting the prior attempt count;
- second failed delivery produced:
  - the same invitation UUID;
  - delivery status `FAILED`;
  - delivery attempt count `2`;
- no duplicate invitation was created;
- disposable invitation/audit evidence was then removed;
- fixture returned to **0 invitations**.

The deployed `fpp-owner-invite` source is already reconciled exactly to PR #74 and maps an Auth invite error to:
- `fpp_admin_record_invitation_delivery(..., false)`;
- response outcome `DELIVERY_FAILED`;
- `retryable: true`.

Together these prove failed delivery remains visible and a resend safely reuses the same pending invitation rather than creating a duplicate.


## PASS — final hosted RLS / grant / advisor snapshot

Final hosted snapshot on 2026-09-26:

Migration history remains exactly:
- `20260925012939_phase_12c_identity_foundation`;
- `20260925013000_phase_12c_invitation_fk_indexes`;
- `20260925195816_phase_12f_owner_member_admin`;
- `20260925195901_phase_12f_rpc_grant_hardening`.

Edge Function:
- `fpp-owner-invite` = ACTIVE;
- version = 1;
- JWT verification = enabled;
- deployed source remains the reconciled Phase 12F source.

Table authorization:
- RLS enabled on `fpp_organizations`, `fpp_memberships`, `fpp_invitations`, and `fpp_admin_audit`;
- authenticated table grants remain SELECT-only on Organizations, Memberships, and Invitations;
- authenticated has no table grant on `fpp_admin_audit`;
- no anon table grants were observed on the Phase 12 identity tables;
- `fpp_admin_audit` intentionally has RLS enabled with no client policy.

RPC/helper authorization:
- the ten approved public SECURITY DEFINER RPCs are executable by authenticated/service_role and not anon;
- `private.fpp_lock_active_organization`, `private.fpp_require_active_owner`, and `private.fpp_write_admin_audit` remain unavailable to authenticated/anon;
- `private.fpp_is_active_member` and `private.fpp_is_active_owner` are intentionally executable by authenticated because Phase 12C RLS SELECT policies call those predicates.

Security advisor:
- INFO: audit table RLS enabled with no policy — intentional server-only audit design;
- WARN: ten authenticated-callable SECURITY DEFINER RPCs — intentional narrow server-authorization surface;
- WARN: leaked-password protection disabled — previously recorded project-plan limitation.

Performance advisor:
- INFO only: four unindexed foreign keys on `fpp_admin_audit`;
- no correctness blocker reported.

No unexpected schema, grant, RLS, migration, or Edge Function drift was found.


## PASS — final Android regression

Android CI run `36241181047` completed successfully on 2026-09-26 for branch snapshot `2772ebdcc9165c2b6aae6c30f875a1c87f185a7c`.

Successful steps included:
- unit tests;
- internal debug build;
- stable test APK signer verification;
- instrumented image tests;
- internal launch smoke test;
- APK and rendered test-evidence artifact upload.

No runtime source changed after the proven Phase 12F implementation checkpoint; subsequent branch changes through this snapshot were governance/evidence documentation. This run therefore provides the required final regression evidence for the current Phase 12F runtime content.

## PASS — bounded Samsung reality gate complete

The required Phase 12F physical evidence is now recorded:
- real invitation delivery and deep-link acceptance;
- accepted MEMBER cannot access Owner administration;
- MEMBER sign-out preserves field workspace;
- Owner access restores correctly;
- accepted invitation reconciles into Membership list;
- MEMBER → OWNER promotion;
- OWNER → MEMBER demotion;
- MEMBER revoke/reactivate;
- final active Owner revoke blocked;
- final active Owner demotion blocked.

The remaining Phase 12F work is fixture cleanup and explicit Level 3 merge approval.
