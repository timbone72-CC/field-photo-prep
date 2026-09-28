# Phase 12L — Clean-Install / New-User Reality Gate Plan

Date: 2026-09-27

Status: **ALL GATES PASS — CLOSEOUT COMPLETE; PR #85 MERGE IS THE FINAL 12L ACTION**

## Goal

Prove the assembled Phase 12 identity/release behavior on real Android without risking the operator's working field package or pretending that same-device evidence proves cross-device provider portability.

## Governed base

- `main`: `c7970238e00139415a417f669d86915c4260154e`;
- Phase 12K: COMPLETE;
- post-merge Production Release Candidate run `36316384072`: PASS;
- unrelated Draft PR #39 remains outside Phase 12L.

## Classification

The authoritative 12L line is **Level 1 documentation/evidence work** unless a runtime or backend defect is discovered.

12L itself does not authorize a runtime fix. Any defect that requires code, schema, permissions, Drive semantics, signing, or backend mutation returns to a separately governed branch at the appropriate Level 2/3 classification.

## Contract reconciliation

Before starting the physical gates:

- `CONTRACT.md` is reconciled with the approved Phase 12K production package/signing/update invariants;
- `TESTING_CONTRACT.md` is reconciled with production release and clean-install evidence rules;
- `CHANGE_CONTROL_CONTRACT.md` already classifies deployment/signing work as Level 3 and requires post-install verification — no change needed;
- `INTEGRATION_CONTRACT.md` already requires deliberate SAF/provider selection and forbids silent provider/account switching — no change needed;
- `GOVERNANCE.md`, `PROJECT_PROFILE.md`, and `RULE_INDEX.md` already route release/signing, external-state parity, physical gates, and new-phase work correctly — no change needed.

These changes codify behavior already approved/proven in 12K; they do not expand runtime scope.

## Safest physical test arrangement

Use the separately installed **production package** `com.inandout.fieldphotoprep` as the disposable 12L clean-state surface.

The working field package `com.inandout.fieldphotoprep.internal` is protected and must **not** be cleared, uninstalled, or repurposed for clean-install testing.

Before clearing/uninstalling production package data, physically confirm in that production package:

1. no protected local photos;
2. no WAITING / UPLOADING / FAILED / UNCERTAIN work that would be stranded;
3. no production-package-only Drive binding or business state that must be retained.

If any of those are present, STOP and choose another safe test surface.

## Reality gates

### Gate A precondition — PASS

Production support status was inspected before clearing. All queue/protected-work counts were zero and Drive state was `NO_WORKSPACE`, so no production-only field/Drive state needs preservation. Clearing only `com.inandout.fieldphotoprep` is authorized for this disposable clean-state test. The `.internal` package remains protected.

### Gate A — clean/no-session production launch

After the production package is made safely fresh:

- launch shows/signals sign-in required;
- ordinary field work is not authorized before FPP authentication;
- no prior FPP session is silently restored;
- internal package state remains unchanged.

Observed Gate A result: **PASS**. Fresh production launch showed the FPP Account sign-in screen with no restored session, and `adb shell pm path com.inandout.fieldphotoprep.internal` still resolved afterward, proving the working internal package remained installed.

### Gate B — returning Owner identity

Sign in with the controlled Owner account and prove:

- ACTIVE Membership validates;
- only the authorized Organization is loaded;
- role is OWNER;
- FPP sign-in alone does **not** establish a Drive workspace binding.

Observed Gate B result: **PASS**. Fresh production sign-in reached Home while Google Drive remained `Not connected` and required the explicit `Connect Drive` action. Fresh App Status then confirmed account `VALIDATED`, role `OWNER`, and Drive state `NO_WORKSPACE`. This proves FPP authentication alone did not establish or infer Drive authorization.

### Gate C — deliberate Drive binding

From the fresh production package:

- Drive remains disconnected until operator action;
- workspace selection goes through Android's SAF/system picker;
- the selected workspace/provider identity belongs only to that installation/package context;
- signing into FPP did not infer the Drive account from the FPP Auth email.

Observed Gate C picker result: **PASS for deliberate picker invocation**. After explicit `Connect Drive`, Android's SAF/system folder picker opened and displayed the Drive folder hierarchy. No Drive workspace had been established before that operator action. Observed Gate C workspace result: **PASS for deliberate workspace binding**. The operator selected the `Photos` workspace through SAF, and production Home now shows `Photos` with an explicit `Choose a company` next step. No client company was silently inferred and no customer-content write was needed. Observed Gate C company-discovery result: **PASS**. The company chooser opened from the bound `Photos` workspace and listed expected company folders including `HNP Jobs` and `Tresmolino Jobs`. No company was silently chosen and no write was required. A stale `FPP Phase 12H Test Company - DELETE` test folder remains visible in Drive and is recorded as cleanup context, not a Gate C defect.

Use a safe/disposable workspace context where a write is required. Do not use live customer content merely to prove the picker.

### Gate D — invited user end-to-end

Observed Gate D result: **PASS**. The initial invitation click exposed a hosted Supabase Auth redirect-allowlist defect: the query-bearing production callback fell back to `http://localhost:3000`. That defect was isolated on Level-3 PR #86, corrected by adding `com.inandout.fieldphotoprep://auth-callback*` while retaining the exact production callback, live-tested successfully, and merged to `main` at `440c0d618f5cfbb922c5a11b43571cc33eafb9bc`. The same disposable MEMBER invitation was then resent and completed through the production Field Photo Prep app.

Use a disposable invitation/account fixture and the existing Owner administration path:

- invite as MEMBER first unless evidence requires OWNER separately;
- accept/activate the invitation through the supported flow;
- sign in on the fresh production package;
- validate exact Organization and role;
- prove invitation acceptance did not silently grant/inherit Drive access;
- clean up the disposable Membership/Auth/Invitation fixture after evidence is accepted.

Observed Gate D end-to-end evidence: **PASS**. The newest resend opened the production app, reached the invitation Set Password flow, completed activation, and produced an `ACTIVE` `MEMBER` Membership for `inandoutinspections2026+12lmember@gmail.com`. The invited identity landed on Home with Google Drive `Not connected`, proving invitation acceptance did not silently grant or inherit a Drive workspace. The disposable fixture was retained through Gate E, then cleaned during Phase 12L closeout after explicit operator approval.

If OWNER-specific invitation behavior differs materially from MEMBER, test a disposable OWNER invitation separately; otherwise reuse the already-proven role-administration/backend coverage and do not repeat equivalent physical steps merely for ceremony.

### Gate E — account transition / boundary safety

Observed Gate E result: **PASS**.

Current production-package evidence on 2026-09-28:
- disposable invited identity was `VALIDATED / MEMBER`;
- Drive state was `NO_WORKSPACE`;
- all queue states, protected originals, cleanup pending, and unreadable local records were `0` before sign-out;
- normal Sign Out completed and explicitly reported that the Drive workspace and local field data were left unchanged;
- the controlled Owner account then signed in successfully;
- Account Connected showed the expected `In And Out Cleaner Inspections LLC` Organization and `OWNER` role;
- fresh App Status showed `VALIDATED / OWNER`, Drive `NO_WORKSPACE`, and all queue/protected-work counts still `0`;
- no Drive binding, company, property, or work-order state was silently inherited during the MEMBER → OWNER transition.

Protected-work guard proof is reused rather than manufacturing disposable protected photos on the production phone:
- current `ProtectedWorkGuardTest` proves non-empty CAPTURING reservations, WAITING, FAILED, UNCERTAIN, retained uploaded originals/prepared copies, and unreadable metadata block sign-out/fail closed;
- current `AppStatusCollectorInstrumentedTest` proves those blocking queue/protected-work counts are read without mutating queue state;
- `AuthActivity.signOutSafely(...)` remains the single sign-out owner and consults `ProtectedWorkGuard` before clearing the authenticated session.

Cross-Organization stale-provider isolation is reused from completed Phase 12H physical Samsung evidence because that binding behavior is unchanged:
- a different Organization could not silently inherit the prior workspace even while Android retained the SAF permission;
- stale provider/company/property state did not leak across Organizations;
- returning to the owning Organization recovered the correct binding without overwriting provider identity.

Conclusion: the current production sign-out/account-transition path preserves local/Drive state, respects the protected-work guard, and does not cross Organization/Drive boundaries.

### Gate F — production/internal separation

Observed Gate F result: **PASS**.

Reused Phase 12K / Gate A identity evidence:
- production package is `com.inandout.fieldphotoprep`;
- internal package is `com.inandout.fieldphotoprep.internal`;
- production callback is `com.inandout.fieldphotoprep://auth-callback`;
- internal callback is `com.inandout.fieldphotoprep.internal://auth-callback`;
- production signer SHA-256 is the established permanent production certificate;
- internal uses the existing stable test signer/identity;
- first production install did not remove or replace the internal package;
- after production-package clear/retest, `adb shell pm path com.inandout.fieldphotoprep.internal` still resolved.

Current physical separation evidence on 2026-09-28:
- production package remains a separate fresh test surface and most recently showed `VALIDATED / OWNER` with Drive `NO_WORKSPACE`;
- the working internal app was opened independently after all production testing;
- internal Home still shows the preserved field state `HNP Jobs`, `Workspace: Photos`, and `25 properties`;
- the internal app's existing field/Drive state was therefore not cleared, replaced, or inherited by the production package.

Conclusion: production and internal packages remain separate in package identity, callback/signer identity, and local app data. Clearing/testing production did not clear or alter the working internal app state.

## Second-phone limitation

A second physical Android device is not required to complete claims that do not depend on provider-ID portability.

If no suitable second phone is available:

- do not claim SAF/provider IDs are portable;
- do not treat Secure Folder, another Android user/profile, emulator, or the internal package as second-device portability proof;
- record the outstanding Phase 8C cross-device portability limitation explicitly.

## Evidence/stop rule

Record each physical observation once. Reuse already-valid 12K recovery/package evidence where the underlying behavior is unchanged.

Any unexpected account/Drive/protected-work behavior is a STOP condition for the affected gate. Preserve evidence and open a separately governed defect line rather than patching runtime behavior inside this documentation/evidence branch.


## Fixture cleanup — PASS

After all numbered gates passed, the disposable `inandoutinspections2026+12lmember@gmail.com` fixture was cleaned from the dedicated FPP Supabase project with explicit operator approval.

Verified cleanup:
- disposable Auth user: removed;
- disposable ACTIVE MEMBER Membership: removed;
- disposable ACCEPTED Invitation: removed;
- active Auth session: none remained after normal Gate E sign-out;
- the single disposable `INVITATION_ACCEPTED / SUCCEEDED` audit row that directly referenced the disposable Auth user was removed with separate explicit approval because its foreign key otherwise correctly blocked hard deletion;
- Owner account and ACTIVE OWNER Membership remain present;
- existing `timbone72@gmail.com` account and ACTIVE Membership remain present.

Durable Gate D/E evidence remains in this Phase 12L record and the merged PR #86 defect record.
