# Phase 12L — Clean-Install / New-User Reality Gate Plan

Date: 2026-09-27

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

Observed Gate D result: **STOPPED — DEFECT FOUND**. The disposable MEMBER invitation was `PENDING / delivery SENT`, but clicking the real invitation email opened `localhost:3000` instead of the production FPP callback. Live Auth logs confirm the invite verification request carried `redirect_to=http://localhost:3000`. The deployed invite function still supplies the production callback with `?fpp_invitation_id=...`, so the evidence points to a missing production query-capable Auth redirect allowlist entry. Fix work is out of scope for this Level-1 evidence line and must occur on a separately governed defect branch before Gate D resumes.


Use a disposable invitation/account fixture and the existing Owner administration path:

- invite as MEMBER first unless evidence requires OWNER separately;
- accept/activate the invitation through the supported flow;
- sign in on the fresh production package;
- validate exact Organization and role;
- prove invitation acceptance did not silently grant/inherit Drive access;
- clean up the disposable Membership/Auth/Invitation fixture after evidence is accepted.

Observed Gate D invitation-delivery result: **PASS**. Owner administration shows `inandoutinspections2026+12lmember@gmail.com` as `MEMBER · PENDING · delivery SENT`. Invitation acceptance/new-user/Drive-separation proof remains pending.

If OWNER-specific invitation behavior differs materially from MEMBER, test a disposable OWNER invitation separately; otherwise reuse the already-proven role-administration/backend coverage and do not repeat equivalent physical steps merely for ceremony.

### Gate E — account transition / boundary safety

Using safe disposable identity state:

- sign-out obeys the protected-work guard;
- allowed sign-out does not delete Drive business data or protected local evidence;
- signing in as another identity does not silently reuse a Drive binding for an unauthorized/different Organization;
- no Organization boundary is crossed by stale navigation/provider identity.

If a second Organization or no-membership identity is needed to prove rejection, create only a disposable fixture through approved server-side/admin paths and remove it afterward.

### Gate F — production/internal separation

Prove on the device:

- production package and internal package remain separate;
- production uses production callback/signer identity;
- internal uses its existing internal package/test identity;
- clearing/testing production does not clear or alter internal app data.

## Second-phone limitation

A second physical Android device is not required to complete claims that do not depend on provider-ID portability.

If no suitable second phone is available:

- do not claim SAF/provider IDs are portable;
- do not treat Secure Folder, another Android user/profile, emulator, or the internal package as second-device portability proof;
- record the outstanding Phase 8C cross-device portability limitation explicitly.

## Evidence/stop rule

Record each physical observation once. Reuse already-valid 12K recovery/package evidence where the underlying behavior is unchanged.

Any unexpected account/Drive/protected-work behavior is a STOP condition for the affected gate. Preserve evidence and open a separately governed defect line rather than patching runtime behavior inside this documentation/evidence branch.
