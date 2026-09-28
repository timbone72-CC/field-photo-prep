# Phase 12M Build State

Date: 2026-09-28

Status: **REVIEW COMPLETE — PHASE 12 COMPLETION TAKES EFFECT WHEN THIS DOCUMENTATION CLOSEOUT MERGES**

## Governance classification

- Scope key: phase-12m-account-privacy-release-closeout
- Goal: reconcile stale Phase 12 status records and finish the approved account/privacy/release review.
- Affected surfaces: Phase 12 documentation, identity-design status, and roadmap only.
- Change level: Level 1
- Authoritative branch: docs/phase-12m-account-privacy-release-closeout
- Authoritative PR: the single PR for this branch; no competing 12M line existed at takeover.
- Required rule packs: AGENTS.md; GOVERNANCE.md; PROJECT_PROFILE.md; RULE_INDEX.md; CHANGE_CONTROL_CONTRACT.md; TESTING_CONTRACT.md; identity sections of CONTRACT.md; approved Phase 12 designs.
- Protected behavior: all Android runtime, photo protection, immutable destinations, authorization, SAF/Drive binding, backend permissions, signing, and deployment behavior.
- External systems changed: none; GitHub evidence and original-FPP Supabase were read only.
- Rollback point: 71ec024d4e8a3e655638d350dc7cf19e2c033683
- Verification boundary: documentation diff, final-runtime evidence reuse, source inspection, live backend catalogs/function comparison/advisors, and recorded physical evidence.
- Level 3 merge approval: N/A for this documentation-only closeout.

The user's 2026-09-28 instruction explicitly authorized reconciliation and remaining closeout verification. No runtime or live configuration fix is included.

## Final runtime and approval evidence

Final integrated runtime baseline: `c7970238e00139415a417f669d86915c4260154e` (PR #84).

At takeover `main` was `71ec024d4e8a3e655638d350dc7cf19e2c033683` (PR #87). The entire diff from the final integrated runtime baseline contains Markdown only. No app, backend source, Gradle, signing, script, or workflow content changed. The PR #86 hosted redirect correction remains separately recorded and physically verified.

GitHub job results re-read on 2026-09-28:
- Android CI [36316289354](https://github.com/timbone72-CC/field-photo-prep/actions/runs/36316289354): PASS, including unit tests, production identity checks, internal build/signer checks, instrumentation, and launch smoke. This is the complete-suite evidence for the unchanged final integrated runtime, not a new local test run.
- Production Release Candidate [36316384072](https://github.com/timbone72-CC/field-photo-prep/actions/runs/36316384072): PASS, including signed build and package/callback/client configuration/signer/hash verification.
- Phase 12L Gates A–F: PASS; evidence preserved in `PHASE_12L_BUILD_STATE.md`.

Merged implementation/approval records verified through GitHub:

| Phase | PR | Merge commit | Approval |
| --- | --- | --- | --- |
| 12E | #72 | 31bfaaffa012cadf2da0c9c31c6da64967d9d24f | Explicit Level 3 approval recorded 2026-09-25 |
| 12F | #74 | 5e3580d3ab769015b7f3c6168022a5def2f308c2 | Level 3 APPROVED |
| 12H | #80 | 7b4c2017e9fa013929247751039db08cdae9418f | Level 3 APPROVED |
| 12G | #81 | 3962bad69add448a300fddc1eff3fcd6f2b26002 | Level 2; N/A |
| 12I | #82 | 1476afec4305d366931c39f66d6fe13e63445db9 | Level 2; N/A |
| 12J | #83 | c502260b7207d75a1349581a2f72b397e7fa5e70 | Level 2; N/A |
| 12K | #84 | c7970238e00139415a417f669d86915c4260154e | Level 3 APPROVED |
| 12L redirect | #86 | 440c0d618f5cfbb922c5a11b43571cc33eafb9bc | Level 3 APPROVED |
| 12L | #85 | cc2bf42960fbd91313d2e7fa6b6762fce94c6ad4 | Level 1; N/A |

No repeated phone reset, invitation, upload, or runtime suite is required by this documentation-only change.

## Repository, privacy, and photo-safety review

PASS for the supported Phase 12 claims:
- Android uses only the dedicated original-FPP URL `https://vtyiktvqhbgabawotkrj.supabase.co` and a publishable client key; no Team runtime reference or localhost callback dependency was found in the inspected Android source/configuration.
- Tracked-file scans found no modern secret-key literal, service-role JWT, or PEM private-key marker. The only tracked keystore is the deliberately non-production `ci/field-photo-prep-test.jks`. This is a bounded source scan, not a claim about every historical commit or external secret store.
- `SecureAuthStore` encrypts session state using Android Keystore-backed AES-GCM. Both backup-rule files exclude shared preferences and operational data; the manifest references those rules. `allowBackup=true` does not override their explicit exclusions.
- `RuntimeAuthorizationManager` remains the policy owner, with serialized revalidation and session-generation protection. Action guards consume its decision; diagnostics does not persist a second authorization state machine.
- Capture reservations and Drive mutation/upload entry points use the central guard. Account sign-out checks protected work, fails closed on inspection failure, and clears only auth/session state. No account-close deletion flow is introduced.
- `OrganizationDriveBindingGuard` compares the saved Organization identity to the current authorized Organization and requires local provider permission. FPP email is not used to select or authorize Drive.
- The strict `AppStatusSnapshot.supportSummary()` allowlist excludes tokens, emails, identity UUIDs, Organization/company names, addresses, provider IDs, SAF URIs, and raw errors.
- Existing immutable photo destination and FAILED/UNCERTAIN behavior is unchanged from the tested baseline. Existing 12E/12H/12L evidence is reused; no fresh protected-work experiment was manufactured.

## Live backend review — 2026-09-28

Only project `vtyiktvqhbgabawotkrj`, named Field Photo Prep, was inspected. Team was not changed or used.

PASS:
- Four public tables only: Organizations, Memberships, Invitations, and the approved administration audit table. Inspected columns contain identity/administration data, not a job/photo/provider-ID mirror.
- RLS is enabled on all four public tables.
- `authenticated` has SELECT only on the three identity tables; `anon` has no table grants. The audit table has no client table grant/policy, intentionally denying direct access.
- SELECT policies restrict Organizations to active members, Memberships to self or Owner, and Invitations to Owner.
- All ten public administrative/acceptance functions deny anonymous execution and use an empty search path. Owner operations call the private ACTIVE-OWNER check for the exact Organization; acceptance checks the authenticated user's confirmed email against the exact invitation. Editable user metadata is not the authorization source.
- Internal lock/Owner-check/audit helpers deny direct authenticated execution; the two policy lookup helpers retain intentional authenticated execution.
- All 16 public/private FPP function bodies match checked-in migrations after whitespace normalization. Full differing diffs were inspected: eight bodies differ only in line wrapping of argument/column lists. No semantic divergence was found; migrations were not rewritten or re-applied.
- All four hosted migration versions/names match the repository: 20260925012939, 20260925013000, 20260925195816, 20260925195901.
- Deployed `fpp-owner-invite` version 1 is ACTIVE with JWT verification enabled. Its `index.ts` matches the repository byte for byte: SHA-256 `bf6746d25e0422ae1bf6c9ae4a93018e0aabbf050ed758b6bb0284173b23af33`.
- Zero Storage buckets; no job/photo backend expansion.
- Zero disposable `+12lmember` / `.example.invalid` Auth users or Invitations and zero Phase 12 fixture Organizations. One real Organization, one ACTIVE OWNER, and one ACTIVE MEMBER remain.

Security advisor findings match the previously reviewed 12F baseline:
- INFO: audit table RLS without policy — intentional deny-by-default with no client grants ([advisor](https://supabase.com/docs/guides/database/database-linter?lint=0008_rls_enabled_no_policy)).
- WARN: ten authenticated-callable SECURITY DEFINER RPCs — intentional narrow server entry points with internal authorization and no anonymous execution; live bodies/grants reviewed ([advisor](https://supabase.com/docs/guides/database/database-linter?lint=0029_authenticated_security_definer_function_executable)).
- WARN: leaked-password protection remains disabled, previously recorded as a Free-plan limitation; no claim that this protection is enabled ([guidance](https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection)).

The connected API does not expose the hosted Auth redirect/email configuration. Its evidence remains the accepted production recovery and query-bearing invitation reality gates and recorded allowlist change, not a claimed fresh configuration read. No new email or live mutation was performed.

## Reconciliation

This closeout corrects stale top-level roadmap labels for 12G/12H/12I, the Phase 12 umbrella, the master-plan instruction to begin 12E, and outdated design/build-state completion labels. Historical failed tests and intermediate checkpoints remain as history, explicitly superseded by the current closeout notice. It does not erase failure history or change approved runtime design.

## Explicit limits outside Phase 12 completion

- Phase 8C second-physical-Android/shared-master portability remains deferred. No cross-device/account provider-ID portability claim is made.
- Draft PR #39 remains separate, unmerged, and blocked on its own real-device/Level 3 gate. Phase 12 completion does not complete that provider-freshness fix.
- Controlled existing use only: wider/outside-user distribution still requires production-suitable business-controlled Auth email/SMTP and recovery/rate verification.
- First production install/signing identity is proven. A subsequent higher-version production update must prove install-over-existing data preservation before wider distribution; no such update was manufactured here.
- Public app-store release, subscriptions, public self-signup, account/Organization-close UX, and verified HTTPS App Links remain separately governed/deferred.
- The existing deployed Edge Function imports `@supabase/supabase-js@2` and an unversioned runtime-types reference. Current deployed bytes are verified; a future redeployment should pin/lock dependencies under a separate governed change. No dependency upgrade was performed for this closeout.

## Completion rule and handoff

The supported account/privacy/release review has passed with the explicit limits above. Phase 12M and Phase 12 are COMPLETE once this Level 1 documentation PR passes required GitHub checks and merges. Before that merge they are review-complete, not yet closed on governed main.

The next action on this branch is required documentation/governance checks and merge. Once merged, no Phase 12 gate remains and no additional phone action is required. Choose any later product or deferred-gate work separately; do not automatically resume old Phase 12 instructions.
