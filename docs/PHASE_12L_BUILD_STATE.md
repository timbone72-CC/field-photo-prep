# Phase 12L Build State

Date: 2026-09-27

Status: **COMPLETE — ALL GATES PASS; PR #85 MERGED TO MAIN**

## Governed base

- original 12L base: `c7970238e00139415a417f669d86915c4260154e`;
- pre-merge reconciled `main`: `440c0d618f5cfbb922c5a11b43571cc33eafb9bc`;
- Phase 12L merge to `main`: `cc2bf42960fbd91313d2e7fa6b6762fce94c6ad4`;
- current-main reconciliation merge on 12L branch: `6419975c66ba19230516904f5a000ab63697b40b`;
- Phase 12K merge: PR #84;
- Phase 12K post-merge release activation: PASS;
- post-merge Android CI `36316289354`: PASS;
- Production Release Candidate `36316384072`: PASS.

## Authoritative line

- branch: `phase-12l/clean-install-new-user-reality-gates`;
- PR: #85 — `Phase 12L: clean-install and new-user reality gates` — **MERGED**;
- competing 12L branch/PR at takeover: none;
- unrelated Draft PR #39 remains outside Phase 12L.

## Classification

**Level 1 — documentation/evidence gate only.**

No runtime, schema, Drive-provider, authorization, signing, or backend mutation is authorized on this branch. A discovered defect must use a separately governed implementation line.

## Contract/source-of-truth audit

Reconciliation performed before physical testing:

- `CONTRACT.md`: add approved permanent production Android identity/signing/update rules from completed 12K;
- `TESTING_CONTRACT.md`: add release/clean-install evidence rules;
- `CHANGE_CONTROL_CONTRACT.md`: current; no edit required;
- `INTEGRATION_CONTRACT.md`: current; no edit required;
- `GOVERNANCE.md`: current; no edit required;
- `PROJECT_PROFILE.md`: current; no edit required;
- `RULE_INDEX.md`: current; no edit required;
- `REGRESSION_CHECKLIST.md`: no new runtime behavior in 12L, so no checklist expansion required.

## 12K closeout reconciled

- PR #84 merged to main at `c7970238e00139415a417f669d86915c4260154e`;
- GitHub Production Release Candidate run `36316384072`: PASS;
- GitHub-built v37 APK signer/package/callback/Supabase evidence: PASS;
- GitHub-built APK SHA-256: `49a02f6fe4d1a8becacae305966e46145bdaa9cb63cea105947763d3e24ad0dd`;
- artifact ID `10931335308`;
- main post-merge Android CI `36316289354`: PASS.

## Historical next gate (completed)

Before making the production package fresh, inspect the installed **production** app and prove it contains no protected local photos, unresolved upload/queue work, or production-only Drive binding/state that must be preserved.

Do **not** clear/uninstall `com.inandout.fieldphotoprep.internal`.

Once the production-package precondition is clean, begin Gate A from `docs/PHASE_12L_REALITY_GATE_PLAN_2026-09-27.md`.

## Production pre-clear safety evidence

Production pre-clear safety gate: **PASS**.

Observed production support status before any clear/uninstall:
- app version: `0.28.0`;
- account state: `VALIDATED`;
- role: `OWNER`;
- Drive state: `NO_WORKSPACE`;
- camera permission: `NOT_GRANTED`;
- queue CAPTURING / WAITING / UPLOADING / FAILED / UNCERTAIN / UPLOADED: all `0`;
- protected originals: `0`;
- cleanup pending: `0`;
- unreadable local records: `0`.

Conclusion: the production package contains no protected local work, unresolved upload state, or production-only Drive workspace binding that needs preservation. It is safe to clear **only** `com.inandout.fieldphotoprep` for Gate A. The working `com.inandout.fieldphotoprep.internal` package remains protected and must not be cleared or uninstalled.

## Gate A — clean/no-session production launch

Gate A clean/no-session launch: **PASS**.
- production package was cleared after the pre-clear safety gate passed;
- fresh production launch displayed `Field Photo Prep Account` with empty Email/Password fields and Sign In / Forgot Password actions;
- no prior OWNER session was silently restored;
- ordinary authenticated field use therefore remains gated behind FPP sign-in as designed.

Internal-package preservation sub-check: **PASS**. `adb shell pm path com.inandout.fieldphotoprep.internal` resolved successfully after production-package clear/retest, proving production clean-state testing did not remove the working internal package. This evidence may also be reused for Gate F.

## Gate B — returning Owner identity

Gate B returning Owner identity: **PASS**.
- fresh production package accepted the controlled Owner sign-in;
- app reached Home after authentication;
- Home shows Google Drive `Not connected` with explicit `Connect Drive` action;
- no Drive workspace was silently restored or inferred by FPP authentication;
- property state is fresh/empty on this production install.

Fresh post-clear App Status confirms:
- app version `0.28.0`;
- account state `VALIDATED`;
- role `OWNER`;
- grace not applicable;
- Drive state `NO_WORKSPACE`;
- all queue/protected-work counts remain zero.

Conclusion: the returning Owner identity validated successfully and FPP authentication did not establish or infer a Drive workspace.

## Gate C — deliberate Drive binding

Gate C deliberate Drive binding: **PASS**.
- fresh production Home remained `NO_WORKSPACE` after FPP authentication;
- operator explicitly tapped `Connect Drive`;
- Android system Storage Access Framework folder picker opened;
- picker shows the operator-controlled Drive provider and existing Drive folder hierarchy;
- no Drive workspace was inferred from the FPP Auth email/session.

Remaining Gate C proof: deliberately choose the correct workspace root (`Photos`, parent of company folders such as HNP Jobs), confirm the production package records that workspace, and verify company discovery without requiring any customer-content write merely for the gate.

Gate C workspace-selection evidence:
- operator navigated one level up from `HNP Jobs` to the intended multi-company workspace root `Photos`;
- operator explicitly accepted that folder through the Android SAF picker;
- production Home now shows workspace `Photos`;
- app prompts `Choose a company` / `Next: Choose a Company` rather than silently selecting a client company;
- no live customer write was required for this proof.

Company discovery proof: **PASS**. The bound `Photos` workspace exposes the expected company choices including `HNP Jobs` and `Tresmolino Jobs`. An older `FPP Phase 12H Test Company - DELETE` test folder is also visible; it is a stale Drive fixture and is not treated as a Gate C failure. No company was silently selected and no customer-content write was required.

## Gate D — invited user end-to-end

Gate D invited-user end-to-end: **PASS**.

Initial invitation-delivery evidence:
- Owner administration created a disposable invitation for `inandoutinspections2026+12lmember@gmail.com`;
- role: `MEMBER`;
- invitation status: `PENDING`;
- delivery status: `SENT`;
- existing accepted member `timbone72@gmail.com` remained a separate Membership/invitation record.

Bounded defect discovered during first acceptance attempt:
- the invitation email initially fell back to `http://localhost:3000` instead of opening the production callback;
- live Auth logs confirmed the failed verification used the localhost redirect;
- the deployed invite function was already supplying `com.inandout.fieldphotoprep://auth-callback?fpp_invitation_id=...`;
- the defect was isolated to the hosted Supabase Auth Redirect URL allowlist.

Defect disposition:
- Level-3 PR #86 `Fix production invitation redirect allowlist` owned the configuration correction;
- required wildcard entry `com.inandout.fieldphotoprep://auth-callback*` was added while retaining the exact production callback;
- no Android runtime code, Drive code, database schema, package identity, or signing change was required;
- focused live verification passed;
- PR #86 merged to `main` at `440c0d618f5cfbb922c5a11b43571cc33eafb9bc`.

Final Gate D acceptance evidence:
- the same disposable FPP invitation was reused; no duplicate FPP invitation was created;
- the newest resend opened the production Field Photo Prep app rather than localhost;
- invitation verification reached the Set Password flow;
- password completion succeeded;
- final invitation status: `ACCEPTED`;
- final delivery status: `SENT`;
- final Membership role: `MEMBER`;
- final Membership status: `ACTIVE`;
- invited identity landed on Home with Google Drive `Not connected`.

Conclusion: Gate D is **PASS**. Production invitation deep-linking, invitation activation, MEMBER authorization, and Drive-separation behavior are proven on the physical production package.

Fixture disposition:
- retain the disposable `+12lmember` identity only for the immediately following Gate E account-transition/boundary-safety evidence;
- clean the disposable Membership/Auth/Invitation fixture after Gate E unless a later Phase 12L record explicitly documents another reason to retain it.

## Gate E — account transition / boundary safety

Gate E: **PASS**.

Current production-package physical evidence:
- pre-sign-out disposable MEMBER state: `VALIDATED / MEMBER`, Drive `NO_WORKSPACE`, all queue/protected-work counters `0`;
- normal sign-out completed and reported that Drive workspace/local field data were left unchanged;
- controlled Owner then signed in successfully;
- Account Connected showed `In And Out Cleaner Inspections LLC` / `OWNER`;
- post-transition App Status: `VALIDATED / OWNER`, Drive `NO_WORKSPACE`, all queue/protected-work counters still `0`;
- no stale Drive binding or navigation state was silently inherited during MEMBER → OWNER transition.

Reused unchanged evidence:
- `ProtectedWorkGuardTest` covers sign-out blocking/fail-closed behavior for CAPTURING, WAITING, FAILED, UNCERTAIN, retained uploaded local copies, prepared-copy cleanup, and unreadable metadata;
- `AppStatusCollectorInstrumentedTest` verifies the blocking counters without mutating queue state;
- completed Phase 12H physical Samsung evidence proves a different Organization cannot silently inherit stale Drive/provider state while SAF permission remains, and that return to the owning Organization restores the correct binding without rewriting provider identity.

Conclusion: Gate E account transition / boundary safety is **PASS** without manufacturing new protected work on the production phone.

## Gate F — production/internal separation

Gate F: **PASS**.

Reused identity/separation evidence:
- production package: `com.inandout.fieldphotoprep`;
- internal package: `com.inandout.fieldphotoprep.internal`;
- production callback/signer and internal callback/test identity remain distinct as established in Phase 12K;
- first production install did not replace internal;
- Gate A post-clear ADB proof confirmed the internal package still existed after production data was cleared.

Current physical evidence:
- production most recently validated as OWNER with Drive `NO_WORKSPACE`;
- internal app opened independently afterward;
- internal Home still shows `HNP Jobs`, `Workspace: Photos`, and `25 properties`;
- working internal field/Drive state therefore survived the full production Gate A–F sequence unchanged.

Conclusion: Gate F production/internal separation is **PASS**.

## Phase 12L gate status

- Gate A: **PASS**
- Gate B: **PASS**
- Gate C: **PASS**
- Gate D: **PASS**
- Gate E: **PASS**
- Gate F: **PASS**

All numbered Phase 12L reality gates are complete.

## Completion

Phase 12L is **COMPLETE**.

- all physical Gates A–F: **PASS**;
- disposable fixture cleanup: **PASS**;
- PR #86 redirect defect: fixed, verified, and merged before 12L closeout;
- PR #85: merged to `main` at `cc2bf42960fbd91313d2e7fa6b6762fce94c6ad4`;
- no additional phone/provider/Supabase reality gate remains for Phase 12L.

The Phase 12M review is complete; `PHASE_12M_BUILD_STATE.md` owns final documentation closeout and retained limits.


## Disposable fixture cleanup

Cleanup: **PASS**.

Live Supabase verification after explicit operator approval:
- `inandoutinspections2026+12lmember@gmail.com` Auth users remaining: `0`;
- matching Invitations remaining: `0`;
- matching Memberships remaining: `0`;
- matching Gate D acceptance audit row remaining: `0`;
- Owner Auth user remains present with one ACTIVE OWNER Membership;
- `timbone72@gmail.com` remains present with one ACTIVE Membership.

The disposable account had already completed normal Gate E sign-out, so no active Auth session remained at cleanup time.

The hard-delete path initially stopped safely on the audit foreign key. No partial cleanup occurred because that attempt was transactional. After separate explicit approval, only the single disposable `INVITATION_ACCEPTED / SUCCEEDED` audit row was removed, then the disposable Membership, Invitation, and Auth user were deleted successfully.
