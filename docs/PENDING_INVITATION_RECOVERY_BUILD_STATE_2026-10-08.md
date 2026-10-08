# Pending Invitation Recovery — Impact / Build State

Date: 2026-10-08
Status: SOURCE IMPLEMENTED / AUTOMATED CI PASS / LIVE BACKEND UNCHANGED — REALITY GATES PENDING
Scope key: fpp-verified-invitation-recovery
Change level: **Level 3** — membership/authentication authorization, new constrained Supabase RPC.

## Grounded problem / approval
An Owner invited an outside person as MEMBER using the existing production Field Photo Prep app. The recipient's first invite link reached the wrong package and consumed the Auth invite verification; a later invitation link was invalid. Password recovery succeeded but left the original Organization invitation PENDING and no ACTIVE Membership. Password sign-in then returned “This account does not have an active Field Photo Prep membership.” Operator approved implementing a recovery path on 2026-10-08. This approval is implementation authorization only, **not Level-3 merge/deployment approval**.

## Authority / external state
- Repository: `timbone72-CC/field-photo-prep`
- Governed base and rollback: `c9e6965dc934d697b18e7b62e5f4ebbc0dfb9656` (`main` at takeover)
- Authoritative branch: `fix/pending-invitation-after-password-recovery`; PR pending creation
- Existing open PR #95 owns address/work-order cleanup and PR #96 owns workflow pinning, neither owns this authentication recovery path.
- Dedicated Supabase project: `vtyiktvqhbgabawotkrj` (original Field Photo Prep; not Field Work Hub)
- A real recipient has a PENDING invitation and an email-confirmed Auth user, but no activated Organization membership; **do not mutate this live account as a development fixture**.
- No live database, identity, Auth redirect, Drive, permission, or app installation change is authorized by a source commit.

## Proposed smallest safe behavior
When a password is valid but ordinary Membership validation finds zero ACTIVE memberships, the app may ask its existing Supabase backend for *only* the unexpired PENDING invitation for that exact authenticated, email-confirmed user. A user with zero invitations, ambiguous multiple invitations, an existing (including revoked) Membership, or an unconfirmed email remains blocked. The app displays the exact Organization and intended role, and requires explicit **Accept Invitation**. Acceptance calls the *existing* `fpp_accept_invitation` RPC with the returned invitation UUID. It then performs the *existing* complete Membership validation and only afterward stores a validated session. The unvalidated sign-in tokens remain in memory for the brief recovery screen and never grant field/Drive rights.

## Affected owner surfaces
- `supabase/migrations/*pending_invitation_recovery*.sql`: narrow verified-self lookup RPC, no broad table grants.
- `app/src/main/java/com/inandout/fieldphotoprep/SupabaseAuthClient.java`: typed no-membership outcome / self lookup.
- `app/src/main/java/com/inandout/fieldphotoprep/AuthActivity.java`: explicit pending-invitation confirmation UI inside existing Account screen.
- Focused auth parsing/state tests. No extra sign-in or invitation implementation, no new persistence owner.

## Risk / verification / rollback
- Required data: Auth User UUID and verified email, one PENDING non-expired invitation, exact Organization and role, existing acceptance RPC. No new persisted client data.
- Optional data: user-visible Organization name for informed consent. No customer names/photos/Drive identifiers in Auth.
- Security gates: no anonymous/MEMBER/Owner cross-account lookup; revoke/unconfirmed/expired/multiple pending stay blocked; invalid/expired tokens denied; no old persisted session overwritten before full acceptance; no bypass of offline/revocation/Drive gates; no automatic membership mutation on lookup; idempotent acceptance only via existing RPC.
- Focused tests for response state and sign-in/missing-membership UI. CI complete on exact final runtime head; hosted Auth/RLS/grant/reality test with disposable account/invitation before live use. Supabase integration requires verifying backend migration parity first and deployment only after operator approval.
- No code on `main`, no live schema/config changes, no new APK distribution from this branch until gates pass.
- Rollback: revert this branch/new client changes and, if deployed later, revoke the added RPC EXECUTE grant or drop only that RPC through governed forward migration. Preserve all real invitations, Auth users, protected photos, queue, SAF grants, and Drive data.
- Explicit Level-3 approval required before merge and deployment. No claim of a successful real recipient activation until physically verified.

## 2026-10-08 verification checkpoint

Authoritative draft PR: [#97](https://github.com/timbone72-CC/field-photo-prep/pull/97).
Runtime source head: `5d0d694de6b4a556909897e202b8996b27e63067`.
Implementation-only commit includes a narrow confirmed-self invitation-lookup RPC migration, Android typed lookup/sign-in confirmation path, and `PendingInvitationRecoveryInstrumentedTest`.

### Automated and repository evidence
- Android CI [37799742270](https://github.com/timbone72-CC/field-photo-prep/actions/runs/37799742270): **SUCCESS** for source head above. Unit tests, debug APK build, production identity/fail-closed signer checks, test-signer verification, complete `connectedDebugAndroidTest` (including new instrumentation test), theme/rendering verification, and launch smoke succeeded. The workflow's deliberately failed unsigned production build is an **expected successful fail-closed test**, not an overall CI failure.
- Artifact `field-photo-prep-internal-apk`, ID `11560810573`, from this CI run. **Not authorized for installation on the recipient's phone**: this branch inherits `main` internal versionCode `42` / `0.28.5-internal`, below the separately installed `0.28.6-internal` / versionCode `43`. Do not uninstall/reset/downgrade to bypass this; plan a monotonically higher approved candidate after reconciling open PR #95's version 44.
- Governance [37799888320](https://github.com/timbone72-CC/field-photo-prep/actions/runs/37799888320): **SUCCESS**, after formatting PR's Level 3 merge approval as `PENDING`. Earlier formatting-only governance failure [37799743082](https://github.com/timbone72-CC/field-photo-prep/actions/runs/37799743082) is superseded by this passing rerun without altering runtime code.

### Read-only production backend evidence
Supabase original-FPP project `vtyiktvqhbgabawotkrj` inspected, **not changed**.
- Current live migrations remain only `20260925012939`, `20260925013000`, `20260925195816`, `20260925195901`. The new `20261008161000` migration has **not** been applied.
- Existing `public.fpp_accept_invitation`: SECURITY DEFINER, EXECUTE permitted for `authenticated`, not `anon`.
- `public.fpp_find_my_pending_invitation` is **absent** on the live backend, as expected.
- `fpp_invitations`, `fpp_memberships`, `fpp_organizations`: RLS enabled.
- SQL was source-reviewed for exact `auth.uid()`/confirmed-email restriction, no-existing-membership fail-closed behavior, PENDING/non-expired/ACTIVE organization filtering, and ambiguous result refusal. This is a **source review**, not a passed hosted SQL isolation or deployment test.

### Remaining gates and ordering
1. In a safe disposable test environment, validate the new SQL function with Auth contexts: anonymous denial, other-email denial, unconfirmed-email denial, no invitation, expired/cancelled invitation, multiple pending invitations, and existing/revoked membership. Prove no membership mutation from the lookup alone. Check function grants and RLS after migration there. **Not yet done.**
2. Reconcile PR #95's versionCode 44 before preparing an installable Internal version newer than the recipient's versionCode 43. Preserve production signer identity if a production build is selected. New installation/update is a separate approved release path.
3. Before any live deployment, operator reviews the Level-3 reality-gate plan and authorizes the staged live backend change; then apply only the reviewed migration, inspect grants and run one disposable real-server/device invitation-acceptance test. The existing invitation/recipient account remains unchanged during fixture testing.
4. Physical check on recipient's phone: correct Auth account → pending invite displayed → explicit Accept → validated MEMBER/ACTIVE, with Drive unbound until separately selected. Stop on wrong organization, any unexpected provider binding, duplicated membership, or permission mismatch.
5. Obtain distinct explicit Level-3 merge/deployment approval after evidence. PR remains DRAFT, no merge. Auth passwords/tokens, photos, queue, company Drive, SAF bindings, and real invitation are not mutated by this checkpoint.

No claim of backend compatibility, active new Membership, physical update success, or complete production readiness is made.

## 2026-10-08 isolated test environment blocker

- Operator approved a short-lived Supabase development branch at the quoted cost of $0.01344/hour.
- `confirm_cost` acknowledged the quoted rate. `create_branch` returned `PaymentRequiredException: Branching is supported only on the Pro plan or above`.
- Subsequent `list_branches` returned only the default `main` branch for the original FPP project; **no isolated development branch was created**.
- No live project SQL was executed, no migration was applied, no invitation/membership/account record was changed, and no hourly testing-branch charge was initiated.
- **STOP** hosted isolated-branch gate. Do not silently substitute transactional production-schema DDL, live recipient fixture, a second paid project, or a plan upgrade.
- Recommended no-cost alternative, subject to operator approval and governance reconciliation: a one-off isolated PostgreSQL/Supabase-compatible test using GitHub CI service containers, with disposable fixtures and explicit exclusion of the original FPP project credentials. Confirm the approach can exercise the migration and Auth/JWT semantics before claiming parity. Retain the live hosted reality gate before any production activation.

## 2026-10-08 operator-approved isolated PostgreSQL contract test — PASS

After Supabase Pro-plan-only branching blocked the disposable hosted test, the operator explicitly approved a no-new-Supabase-project GitHub Actions isolated PostgreSQL test. This is a source/SQL behavioral check, not a claim of Supabase Auth/JWT or hosted redirect parity.

- Scope line: draft [PR #97](https://github.com/timbone72-CC/field-photo-prep/pull/97); separate from PR #95's work-order workflow and PR #96's workflow-pinning rollout.
- Checked runtime SQL/client/test revision: `66a6f7f4df9311e6aafc2d5225bc71e4404db174`.
- Workflow: `.github/workflows/fpp-invitation-recovery-sql.yml`, exclusively temporary GitHub CI PostgreSQL 17 service `fpp_recovery_test`. No original-FPP project URL, API tokens, or live data connections are used.
- Bootstrap: `scripts/test-pending-invitation-recovery-bootstrap.sql` defines minimal disposable Auth user table, `auth.uid()` SQL claim accessor, and `anon`/`authenticated` roles. Exact checked-in four original FPP migrations plus the candidate recovery migration are applied to this database.
- Assertions: `scripts/test-pending-invitation-recovery.sql` covers RLS/grant boundaries; anonymous and missing-subject denial; other-account isolation; unconfirmed email; multiple pending; revoked/existing Membership; cancelled, past-due, explicitly expired invitations; inactive Organization; positive own invitation; read-only lookup; explicit acceptance; idempotent repeat acceptance; exactly one ACTIVE Membership and one acceptance audit.
- Isolated workflow [run 37832869591](https://github.com/timbone72-CC/field-photo-prep/actions/runs/37832869591): **SUCCESS**. Job logs report all 16 security/result assertions PASS and `PASS: isolated pending-invitation recovery SQL contract tests`; no failures.
- PR #97 Governance Check [run 37832864859](https://github.com/timbone72-CC/field-photo-prep/actions/runs/37832864859): **SUCCESS** for same source revision.
- Concurrent Android CI [run 37832869269](https://github.com/timbone72-CC/field-photo-prep/actions/runs/37832869269) is still under verification as of this record; do not claim this newer head complete until job finishes.

**Evidence boundary:** PostgreSQL harness injects the `request.jwt.claim.sub` test variable and sets SQL roles. It reproduces SQL security function behavior, not Supabase Auth's cryptographic token verification, PostgREST gateway, Supabase hosted redirect allowlist, email-delivery behavior, or device binding. It does not authorize production deployment. Existing real recipient invitation remains unchanged.

**Next gate:** Wait for same-runtime Android CI; review PR and local SQL behavior. Before live use, obtain explicit separate authorization for the Level-3 schema deployment and run hosted RLS/Auth and real disposable recipient acceptance checks, then establish a correctly signed, monotonically versioned installation path. Never uninstall/downgrade the working Internal package or alter live customer photos to force the gate.

## 2026-10-08 hosted additive migration verification and production update staging

- Operator approved the live Supabase verification and a prepared safe Android update. **Live original FPP Supabase** now has the additive `public.fpp_find_my_pending_invitation()` function. It was created using the exact source content from PR #97, verified by source SHA and safety checks, through the governed Supabase migration connector.
- The platform assigned **applied migration version `20261008193859`**, name `fpp_pending_invitation_recovery`. The checked-in migration filename is renamed to `supabase/migrations/20261008193859_fpp_pending_invitation_recovery.sql` for **version parity**; prior proposed filename 20261008161000_fpp_pending_invitation_recovery.sql was never applied under that version.
- Read-only catalog verification after deployment: `SECURITY DEFINER`, empty search path, `authenticated` EXECUTE allowed and `anon` EXECUTE denied. All three identity tables still use RLS; no membership/invitation statuses or Auth passwords were written in deployment.
- Security-only SQL harness was proven on isolated PostgreSQL; **this does not yet prove end-to-end hosted Supabase PostgREST JWT and physical Android acceptance**. Recipient membership remains pending until the person explicitly accepts on correct signed app.
- Corrected the existing manual production candidate workflow to verify the repo's current Gradle **production package** versionCode `42`, versionName `0.28.5`, and retained production signing certificate fingerprint/production callback/project checks. It now labels the artifact `field-photo-prep-production-candidate-v42`, with previous distributed production versionCode input default `37`. The separately installed **Internal** package v0.28.6/code43 is a different Android package and must not be overwritten or downgraded. Candidate build is *not* a general FPP release; no APK distributed yet.
- The manual production workflow still requires its existing GitHub Actions signing secrets, fails closed if missing, and is **not** triggered automatically. Its workflow file overlaps the independent pending PR #96 GitHub Actions pinning; preserve PR #96's reviewed pins during merge/rebase and never silently roll them back.
- Exact live schema rollback: revoke `EXECUTE` on `public.fpp_find_my_pending_invitation()` from `authenticated` if server exposure becomes unsafe; formal removal by source-controlled forward migration only after reviewing affected clients. No deletion of Auth users, invitations, memberships, Drive files, or local photos.
- Earlier head `66a6f7f4` Android CI run `37832869269` FAILED an emulator touch-injection stage affecting unrelated Home tests; investigation showed window ownership errors. Until a succeeding final runtime-head complete Android run exists, do not claim this issue resolved or release an APK. Security SQL run `37832869591` passed.
- This is a **staged additive hosted function change**, not an invitation acceptance nor a Level-3 PR merge. User has not yet confirmed real hosted callback and candidate install. Finish automated Android CI, existing photo protection checks, explicit production signer/update continuity, then live recipient acceptance on correct production app before claiming real completion.

## 2026-10-08 signed production candidate — PASS / DISTRIBUTION STILL PENDING

The operator initiated the existing manual production signing workflow using `fix/pending-invitation-after-password-recovery` and previous production versionCode `37`.

- GitHub [Production Release Candidate run #37842908843](https://github.com/timbone72-CC/field-photo-prep/actions/runs/37842908843) completed **SUCCESS**.
- Exact built source head: `a970c8162668b6ec54b5ed03f7d78ea4c2e58b48`. Android CI #37833717599, isolated SQL #37833717482, governance #37833955053 also succeeded for that source.
- APK artifact: `field-photo-prep-production-candidate-v42`, Actions artifact ID `11577993806`, private artifact uploaded successfully, retention 14 days.
- Verified package: `com.inandout.fieldphotoprep`; versionCode `42`, versionName `0.28.5`; callback `com.inandout.fieldphotoprep://auth-callback`; original-FPP Supabase project `vtyiktvqhbgabawotkrj`. The separately installed Internal app has package `com.inandout.fieldphotoprep.internal` and is not being replaced.
- Verified production certificate SHA-256 `8ba1dc6ae4907274a7ddb16662a46a7bb52a0fca5dbeadeb478001052fd2a1c2`.
- Verified APK SHA-256 `139e0e263367f31282f783e7d9a6cf960fc48937f63711c4ef9b6414f684fb52`.
- **Not yet installed or physically tested**. Next step is operator-controlled artifact download and existing regular production app safe same-package, same-signer, higher-version update on the recipient's phone. Confirm local protected-work status first if any concern arises; do not uninstall or clear app data. Perform actual pending-invitation confirmation and post-acceptance authoritative Membership review. If anything fails, stop; do not delete or recreate the invited account.
- Level-3 PR #97 remains DRAFT and merge approval remains PENDING. The live backend additive lookup is installed but no client-side acceptance has yet occurred.

## 2026-10-08 physical recipient invitation acceptance — PASS

**Reality gate: passed on the recipient's Samsung phone, with operator screenshots and live read-only database corroboration.** This checkpoint supersedes earlier “not installed / pending reality gate” statements above; the prior chronology is retained for audit.

- The operator downloaded GitHub Actions signed production candidate `field-photo-prep-production-candidate-v42` (run #37842908843), extracted `app/build/outputs/apk/release/app-release.apk`, and transferred the APK to the recipient phone by USB.
- An initial claim that the APK was “installed” did **not** prove the update: Android App Info screenshots still showed the regular `Field Photo Prep` app at **0.28.0**, and sign-in returned “This account does not have an active Field Photo Prep membership.” The pre-update client therefore remained in use at that point.
- The operator re-opened the copied `app-release.apk` (5.68 MB shown in My Files); Android explicitly prompted **“Field Photo Prep — Do you want to update this app?”** The operator tapped **Update**. No uninstall, app-data clearing, password reset, or Internal-package replacement was requested. A separate post-update Android App Info version screenshot was **not** captured; do not claim independent post-update package/version observation beyond the signed artifact metadata, Android update prompt, and subsequent behavior.
- Subsequently the physical phone displayed **“Account Connected — Invitation accepted and account verified”** for `rushingl27@gmail.com`, organization **In And Out Cleaner Inspections LLC**, role **MEMBER**. The operator tapped **Close** and explicitly confirmed the **regular Field Photo Prep home screen opened**.
- Read-only query against the original production Supabase project `vtyiktvqhbgabawotkrj` after phone acceptance confirmed **one verified Auth account**, exactly **one ACCEPTED MEMBER invitation** for the intended organization, and exactly **one ACTIVE MEMBER membership** in that **ACTIVE** organization. The same read-only check before acceptance had shown a verified user, one unexpired PENDING invitation, and zero memberships.
- **Scope proved:** existing signed regular-production app can complete the verified pending invitation / password-recovery membership flow against the live backend and open Home, with authoritative post-acceptance membership confirmation. No credential text or Auth tokens were collected.
- **Not proved / unchanged:** client Google Drive consent, folder identity and company isolation, photos/queue or contractor-specific field workflow; these remain separate checks. No photo or Drive files were deliberately changed by this verification. Field Photo Prep Internal remains a separate package; it was not part of this test.
- Governance: [PR #97](https://github.com/timbone72-CC/field-photo-prep/pull/97) stays **DRAFT and unmerged**. This documentation commit records a passed reality gate only; it does **not** grant Level-3 merge approval or authorize broader distribution/release. Review remaining repository governance and cross-PR workflow/version conflicts before seeking explicit merge approval.
