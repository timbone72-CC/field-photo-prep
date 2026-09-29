# Field Photo Prep — Evidence-Based Repository Audit

Date: 2026-09-28
Audit mode: read-only; no runtime, live-system, repository-setting, deployment, PR, merge, or cleanup changes
Report path: `docs/FIELD_PHOTO_PREP_AUDIT_2026-09-28.md`
Authoritative completed baseline: `origin/main` at `2ffa723c9be500765503af31a4c53baef7fdb992`
Checkout inspected: `phase-12k/production-identity-release-path` at `6065d48b943c657cae0ef3011c92fdc67213db44`
Checkout status: clean tracked tree with pre-existing untracked `release-evidence/release-evidence.txt`; preserved unchanged.

## Overall result

The supported Field Photo Prep workflow is structurally sound and the repository's governed `main` accurately records Phase 12 completion. I found no confirmed current data-loss, wrong-destination, blind-retry, or cross-Organization authorization defect in the inspected Android, queue, Drive, authentication, backend, or release paths.

The checkout itself is not the authoritative current state: it is an older Phase 12K line, while fetched `origin/main` contains the merged Phase 12L/12M closeout. A fresh agent using the checkout without resolving that baseline would see stale status and may incorrectly resume completed work. The remaining material limits are documented and real: Phase 8C second-phone/provider portability, broader external email/recovery readiness, production update-over-existing verification, leaked-password protection, and the unmerged Phase 10C PR #39 candidate.

## Immediate risk summary

- No immediate evidence of photos currently in `UNCERTAIN`. The 2026-09-17 record documents 53/53 reconciliation, zero retry-safe-missing, and zero unresolved; a later normal Samsung upload was confirmed on its first attempt; the 2026-09-28 closeout records no current UNCERTAIN photos. This must not be reopened solely because PR #39 remains open.
- No confirmed wrong-destination path. Queued destination IDs are persisted and immutable; upload creation rereads the stored work-order ID and verifies returned identities.
- No confirmed original-deletion-before-confirmation path. Local original cleanup requires durable `UPLOADED` state with a remote file ID; failure leaves cleanup pending.
- No confirmed client-company sharing-isolation claim. The app enforces FPP Organization/binding isolation, but Google Drive sharing permissions are external and were not independently audited here.

## Findings

### F-01 — Audit checkout is stale relative to authoritative `main`

Severity: Medium process/documentation risk
Confidence: High
Classification: Confirmed repository-state mismatch, not an app-runtime defect
Smallest correction: continue from fetched `origin/main` or explicitly reconcile the Phase 12K branch before any work; documentation-only/L1 unless a takeover or supersession is required.

Evidence:

- `git fetch origin --prune` advanced `origin/main` to `2ffa723c9be500765503af31a4c53baef7fdb992`, the PR #88 merge.
- The checkout remains `phase-12k/production-identity-release-path` at `6065d48`; its `docs/ROADMAP.md:558-585` still says “12K IN PROGRESS” and describes the pre-12M line.
- `origin/main:docs/ROADMAP.md` says Phase 12 is complete and preserves the separate limits; `origin/main:docs/PHASE_12M_BUILD_STATE.md` records the final review and closeout.

Impact: an agent or operator working from this checkout can repeat completed Phase 12 work, treat stale closeout instructions as active, or incorrectly infer that the release line is still awaiting implementation. Existing uncommitted release evidence was not modified.

### F-02 — Local runtime verification was blocked by the environment

Severity: Medium evidence gap
Confidence: High
Classification: Verification limitation, not a failed repository test
Smallest correction: run the required Gradle checks in a functioning local/CI environment; no source change is indicated.

Evidence:

- `gradle :app:test --no-daemon`, `gradle :app:verifyProductionReleaseIdentity --no-daemon`, and `gradle :app:assembleDebug --no-daemon` all stopped before project evaluation with `Failed to load native library 'libnative-platform.so' for Linux amd64`.
- `git diff --check origin/main...HEAD` passed.
- Exact-head CI evidence reused under `docs/PHASE_12M_BUILD_STATE.md` records Android CI run `36316289354` PASS and production release candidate run `36316384072` PASS for the final integrated runtime baseline; this is valid recorded CI evidence, not a new local run.

Impact: this audit cannot independently certify a fresh local build/test run. The CI evidence supports the governed main claims, but unavailable local tooling is not treated as a pass.

### F-03 — Supabase leaked-password protection is disabled

Severity: Medium security limitation
Confidence: High; live advisor observed 2026-09-29T00:31Z and independently recorded in closeout docs
Classification: Accepted external-service limitation, not an Android defect
Smallest correction: enable Supabase leaked-password protection when the project plan supports it, then re-run the backend security/advisor and authentication evidence; likely Level 3 because it changes live Auth security configuration.

Evidence:

- Live read-only Supabase security advisor reports `auth_leaked_password_protection` WARN.
- `docs/PHASE_12M_BUILD_STATE.md` explicitly records the same limitation as a Free-plan constraint and makes no claim that it is enabled.

Impact: compromised passwords are not rejected by the hosted Auth password-protection feature. This is a real account-security limitation, but it does not expose Drive data or bypass the app's Organization checks.

### F-04 — Edge Function and GitHub Actions dependencies are version/tag based rather than immutable

Severity: Medium supply-chain/reproducibility risk
Confidence: High
Classification: Confirmed maintenance risk; currently accepted/documented for the deployed function
Smallest correction: pin the Edge Function imports to reviewed versions and pin GitHub Actions to reviewed commit SHAs in one governed release/supply-chain batch; Level 3 for a backend redeployment, Level 2 for workflow-only changes.

Evidence:

- `supabase/functions/fpp-owner-invite/index.ts:1-2` imports an unversioned JSR runtime-types path and `npm:@supabase/supabase-js@2`.
- `.github/workflows/android-ci.yml:14,59,65,143,153,161` and `.github/workflows/production-release-candidate.yml:18,20,25,83` use mutable action tags such as `@v4`, `@v5`, and `@v2`.
- The live function is ACTIVE with JWT verification enabled, and its checked-in source/content was inspected read-only. `docs/PHASE_12M_BUILD_STATE.md` explicitly retains the unpinned Edge Function dependency as a future governed change rather than claiming it is fixed.

Impact: future dependency/tag movement can change build or deployed behavior without a source diff, weakening reproducibility and reviewability. No current compromise was found.

### F-05 — Live Drive permission isolation remains unverified

Severity: Medium evidence gap
Confidence: High
Classification: Explicit product boundary/evidence limitation, not a confirmed defect
Smallest correction: perform a disposable, read-only/least-impact sharing-permission review with the actual Drive owner/admin before claiming customer-company isolation; likely Level 3 integration evidence.

Evidence:

- `PROJECT_PROFILE.md` and `CONTRACT.md` separate FPP Organization authorization from Android SAF/Google Drive authorization.
- `OrganizationDriveBindingGuard.java:115-150` compares the bound Organization and provider permission; `FolderPrefs.java:383-424` persists the binding only after explicit provider selection.
- No repository code grants, changes, or inspects Google Drive sharing permissions. The Phase 12L records prove deliberate binding and Organization separation, not Drive ACL isolation between client companies.

Impact: the app can prevent an FPP Organization from using a mismatched local binding, but it cannot establish that a Drive user who already has broad sharing can see only one client company. Do not report that stronger claim without separate provider evidence.

## Positive verification conclusions

### Capture and local protection

`PendingPhotoStore.java:86-118` reserves a unique app-private original before camera bytes are written; `:340-388` preserves non-empty interrupted captures and converts interrupted uploads to `UNCERTAIN`. `PendingPhotoRecord.java:253-376` prevents a provisional remote identity from becoming retryable failure and requires explicit confirmation before `UPLOADED`.

### Upload and reconciliation

`DrivePhotoUploader.java:188-248` reads and verifies the exact stored destination before creation; `:255-377` persists provisional identity before writing, checks byte count and returned name/MIME/identity, and fails closed to reconciliation on ambiguous outcomes. `PhotoUploadCoordinator.java` and `PhotoReconciliationBatchRunner.java` keep sequential batch behavior and do not blind-retry unresolved remote state.

### Cleanup and discard

`ConfirmedPhotoCleanup.java:44-96` permits cleanup only after durable `UPLOADED` plus remote ID. `LocalPhotoDiscardCoordinator.java:66-151` validates the exact stored address/work-order binding and only permits local discard for `WAITING` or `FAILED`; it preserves the original if prepared-copy deletion fails.

### Auth/session and Organization binding

The repository routes authorization through `RuntimeAuthorizationManager`/`AuthorizationActionGuard`, stores session material in `SecureAuthStore`, and uses `OrganizationDriveBindingGuard` as the binding owner. The Phase 12 design and closeout records document serialized refresh, 72-hour grace boundaries, revocation fail-closed behavior, protected-work sign-out blocking, and no silent Drive selection.

### Backend

Read-only live inspection of project `vtyiktvqhbgabawotkrj` found four public FPP tables, all RLS-enabled; two identity tables plus invitations and the intentionally deny-by-default audit table; four applied migration versions matching the repository; one ACTIVE `fpp-owner-invite` Edge Function with JWT verification; and no Storage buckets or job/photo tables. Live function grants showed no anonymous execution. The ten authenticated `SECURITY DEFINER` RPC warnings are intentional narrow entry points whose bodies perform exact Organization/Owner checks, and remain documented advisor findings.

### Release and supply chain

`app/build.gradle:5-119` keeps production and internal package/callback identities distinct, embeds only the dedicated project URL and publishable key, uses the checked-in test keystore only for internal/debug builds, and fails release builds closed without external signing inputs. `scripts/verify-release-apk.sh` and the CI workflows verify package, version, callback, signer, hash, and forbidden internal identity. The tracked keystore is explicitly non-production (`ci/field-photo-prep-test.jks`).

## Coverage table

| Area | Result | Evidence / boundary |
|---|---|---|
| Governance and source baseline | Reviewed / verified | Required entry docs and routed contracts read; fetched refs inspected |
| Runtime architecture/ownership | Reviewed | First-party Java, activities, queue, Drive, auth, binding, resources inspected |
| Capture/orientation/process interruption | Reviewed; prior physical evidence reused | Source/tests and Phase 8/10/12 records; no fresh phone run manufactured |
| Preparation/original protection | Reviewed; verified by source/tests/records | `PhotoPreparer`, queue, store, cleanup paths and focused tests inspected |
| Upload/retry/reconciliation/cleanup | Reviewed; verified by exact recorded evidence | 53/53 reconciliation and later first-attempt upload remain resolved; PR #39 remains observation input |
| Drive/folder/reuse/SAF | Reviewed; Phase 12L/earlier provider evidence reused | Provider IDs and freshness guards inspected; Drive ACLs not independently verified |
| Auth/session/authorization | Reviewed; verified by Phase 12 CI/physical records | Live runtime source and closeout evidence; no new physical run |
| Supabase schema/RLS/grants/functions | Reviewed and live read-only verified | Tables, migrations, function ACLs, advisors, Edge Function status/content |
| Build/release/signing/CI | Reviewed; recorded CI verified; local run blocked | Workflows, Gradle, scripts, keystore class, release records |
| Tests | Reviewed; local execution blocked | Unit/instrumentation inventory inspected; exact CI evidence reused |
| Documentation/roadmap/build state | Reviewed; mismatch found only in non-authoritative checkout | `origin/main` closeout is coherent; checkout is pre-12M |
| Binaries/generated/vendored material | Reviewed | No tracked APK/AAB/media/vendor tree; one intentional test JKS; untracked release evidence preserved |

## Commands/checks performed

- Read `AGENTS.md`, `GOVERNANCE.md`, `PROJECT_PROFILE.md`, `RULE_INDEX.md`, all required contracts, testing packs, identity/staging docs, Phase 10A/10C records, Phase 12 master/design/build-state/closeout records.
- `git fetch origin --prune`: PASS; fetched current `origin/main` and active/related refs.
- `git status --short --branch`, `git log`, `git branch -a -vv`, `git diff --check`: PASS with pre-existing untracked `release-evidence/release-evidence.txt` preserved.
- `gh pr list --repo timbone72-CC/field-photo-prep --state open`: BLOCKED by `api.github.com` connection failure. Open-PR disposition therefore uses fetched refs and durable records, not an invented live PR list.
- `gradle :app:test --no-daemon`: BLOCKED before evaluation by missing/failed `libnative-platform.so` initialization.
- `gradle :app:verifyProductionReleaseIdentity --no-daemon`: same environment block.
- `gradle :app:assembleDebug --no-daemon`: same environment block.
- `git ls-files` secret/material scan: PASS for no tracked production key/private-key material; one tracked non-production test JKS identified.
- Read-only Supabase catalog, migrations, Edge Function list/source, SQL function ACLs, security advisors, and performance advisors: completed against dedicated original-FPP project only.

## Current phase and open-PR disposition

- Phase 12: complete on governed `main` through PR #88 merge `2ffa723`; no Phase 12 runtime gate should be reopened without contradictory evidence.
- Phase 10C: observing. PR #39 remains an unmerged candidate for stale-provider reconciliation hardening; its open state does not mean the operator has unresolved photos.
- Phase 8C: separately deferred until a second suitable physical Android phone/provider context is available.
- Wider external-user email/recovery readiness and production update-over-existing data-preservation verification remain documented release gates.
- Historical/superseded phase branches and PRs should not be resumed merely because refs remain; the authoritative source is governed `main` plus the explicit Phase 10C line for its narrow scope.
- Exact current GitHub open-PR status could not be queried in this session because GitHub API access failed; this is an evidence limitation, not a claim that other PRs are open or closed.

## Explicitly resolved issues that must not be reopened without new evidence

- The 53-photo UNCERTAIN incident was reconciled exactly: 53/53 confirmed, zero retry-safe missing, zero unresolved.
- The subsequent Samsung upload was confirmed on its first attempt without reconciliation.
- Phase 12L production invite redirect fallback was fixed and physically verified through PR #86; do not reclassify the historical localhost incident as current runtime failure.
- Phase 12A–12M identity, authorization, binding, diagnostics, recovery, release, and clean-install claims are closed on governed main for their documented boundaries.
- Do not manufacture UNCERTAIN uploads, clear/reinstall the working internal package, or repeat broad physical gates solely to satisfy stale branch text.

## Remaining verification gates

1. Phase 8C second-phone/shared-master portability: needed because SAF/provider IDs are device/provider-context identities and no second physical context was available.
2. Wider-distribution email/recovery/rate-abuse verification: needed before inviting outside users or public distribution; controlled internal use is the proven boundary.
3. Production update-over-existing verification: needed for a later higher-version production update; no update test was manufactured during closeout.
4. Drive sharing-permission isolation: needed only if the product intends to claim client-company ACL isolation; app-side Organization/binding checks are not that claim.
5. Local Gradle rerun: needed if a fresh local execution record is required; CI exact-head evidence remains valid for unchanged runtime.

## Prioritized repair plan

1. **Baseline hygiene batch (L1):** make future audit/agent entry points unambiguous by continuing from `origin/main` and quarantining stale Phase 12K instructions; do not modify runtime or close PR #39 as part of this batch.
2. **Supply-chain hardening batch (L2/L3):** pin Actions and Edge Function dependencies, review the resulting lock/materialization behavior, run full CI, then redeploy only through a separately approved backend/release change.
3. **Auth security batch (L3):** enable leaked-password protection when commercially available, verify Auth behavior and advisors, and update the retained limitation record.
4. **Reality-gate batch (L3 where applicable):** execute only when hardware/operational need exists: second-phone portability, broader email/recovery, or production update-over-existing. Keep Drive ACL claims separate from app binding evidence.

## Recommended next action

Use `origin/main` at `2ffa723c9be500765503af31a4c53baef7fdb992` as the sole baseline for the next task, preserve the existing untracked release evidence, and do not reopen Phase 12 or the resolved UNCERTAIN incident. The next product/repository action should be chosen from the explicitly bounded limits above—preferably supply-chain pinning or the next genuinely available physical verification gate—under a new governed scope.
