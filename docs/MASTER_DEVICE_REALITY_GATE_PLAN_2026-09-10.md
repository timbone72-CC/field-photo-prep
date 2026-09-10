# Master Device Reality Gate Plan — Straight-Line Field Validation

Date: 2026-09-10

Status: **STAGED — operator Android phone unavailable**

Planning baseline: `feat/phase-6b-h2-create-persist-write-barrier`

Baseline documentation head when staged: `a3439a0f0e07ff676886fd8dc9e6c41e32e5ff2f`

Exact H2 tested runtime/test head: `8d23b061307725589aef69a31a00249744523406`

## Purpose

This plan stages the remaining physical Android reality gates so the eventual phone session follows one controlled path instead of repeatedly stopping to redesign, re-check, re-run, or ask for decisions that are already settled.

Default path:

`one-time preflight → Phase 3B → Phase 4 → integrated Phase 5 + 6A + 6B-H4 → consolidated review`

The plan may veer from that line only when an unexpected result invalidates a required safety assumption, prevents a required observation from being made honestly, or creates uncertain remote state.

The goal is not maximum manual checking. The goal is the **smallest real-device evidence set** that closes the platform-specific gaps left after automated verification.

## Mandatory reread checkpoints

Any agent or operator-assisting AI performing device-gate work must re-read this file:

1. at the start of every physical-device test session;
2. immediately before installing or switching to the next APK/gate;
3. immediately before declaring a gate PASS, BLOCKED, or FAIL;
4. before deviating from the straight-line path because of an unexpected result; and
5. before recommending merge approval or starting Phase 7B design from the device evidence.

Do not rely on memory or a prior summary when this file is available. The reread is intended to prevent scope drift, repetitive safety rituals, unnecessary Bash prompting, and accidental retries of uncertain remote operations.

## Operating doctrine — no-loop rule

1. **Reuse valid evidence.** Existing automated CI results tied to exact runtime SHAs remain valid unless relevant runtime behavior changes. Do not rerun complete suites locally as paperwork.
2. **Preflight once.** Verify device connection, safe test hierarchy, APK identity, provider/account context, and required local tooling once at session start. Repeat only if one of those facts changes.
3. **Install each required APK once by default.** Reinstall only if installation failed, the wrong APK was installed, or app/device state is demonstrably corrupted.
4. **One pass per required observation.** Once a behavior is clearly proven and evidence is captured, do not repeat it for confidence theater.
5. **One controlled repeat for obvious non-product mistakes.** A step may be repeated once when the failure is clearly an operator mis-tap, missed screenshot, dismissed system picker, or similar issue that occurred before any risky remote side effect.
6. **No retry loop on uncertain remote state.** If create/write/delete/rename may have happened and the result is uncertain, stop that affected path. Preserve evidence. Do not keep trying until it works.
7. **No refresh loop.** For a risky provider decision, allow the app's normal settled-state path and at most one deliberate operator refresh/re-entry. If provider state remains non-authoritative, record the gate as provider-blocked and stop that affected path.
8. **Do not manufacture unsafe failures.** If a real-provider failure cannot be induced safely, document that limitation once. Do not add testing backdoors or destructive hacks.
9. **No repeated approval prompts.** Approval of this staged plan covers execution of its documented safe test actions. Ask again only if scope expands or a destructive action outside this plan is proposed. Existing in-app Clear & Reuse confirmation remains required.
10. **No coding during the phone session.** Capture the unexpected result first. Runtime fixes return to the normal governed change/test process.
11. **Stop when proven.** Passing evidence closes the observation. Do not continue poking the same path looking for a failure.
12. **One consolidated review at the end.** Do not interrupt the straight-line session with merge discussions after every successful gate.

## Workstation interaction policy

The phone session must not become a chain of one-command-at-a-time Bash prompts.

Default behavior:

- stage repository refs, APKs, checksums, and test notes before the phone session;
- verify each artifact checksum once;
- use one `adb devices` check when USB debugging is used;
- use one install command per required APK;
- do not repeatedly request `git status`, `git rev-parse`, `git fetch`, full test commands, or artifact checks when those facts are already known and unchanged;
- use connected GitHub evidence where available instead of making the operator reproduce repository facts manually;
- ask the operator primarily for actions that physically require the phone;
- batch non-destructive workstation commands whenever practical;
- when a Bash command succeeds and establishes a stable fact, retain that fact for the rest of the session unless later evidence contradicts it.

## Why the default requires three APK installs

The remaining gates do not all live on one branch lineage.

- Phase 3B Clear & Reuse is on its own development branch.
- Phase 4 Address Folder Creation is on its own development branch.
- Phase 5, Phase 6A, Phase 7A, Phase 6B, H1, and H2 form the later integrated upload lineage.

Therefore the default minimum is **three installs**, not five:

1. Phase 3B APK;
2. Phase 4 APK;
3. H2 integrated APK for Phase 5 + 6A + 6B-H4 observations.

Do not install the separate Phase 5 or Phase 6A historical APKs merely to repeat behavior already present in the integrated H2 build.

If the H2 integrated build fails a Phase 5 or 6A observation, do **not** fall back to an older APK to obtain a passing result. The integrated app is the behavior that matters and the failure must be investigated.

## Staged artifact identities

### Install 1 — Phase 3B Clear & Reuse

Branch: `feat/phase-3b-clear-reuse`

Exact automated-tested runtime head: `0a7071744791b470f1c0bd795d78934c8f28c87e`

CI run: `34302110486`

Artifact ID: `10085354689`

Artifact digest: `sha256:fb62638024d479a17c82d0caa43e3f0ca0410769ae9a4a6ea9fa8e41e41f1032`

### Install 2 — Phase 4 Address Folder Creation

Branch: `feat/phase-4-address-folder-creation`

Exact automated-tested runtime head: `a1ca4f5e1ecd871f24d0724b6c1239bfd9666570`

CI run: `34425740379`

Artifact ID: `10132648386`

Artifact digest: `sha256:bccbcd28c822c731d7a51de63d170915c6ddd14ad7ef7ebb5ce95982b56a5b5a`

### Install 3 — Integrated Phase 5 + 6A + 7A + 6B-H1/H2

Branch: `feat/phase-6b-h2-create-persist-write-barrier`

Exact automated-tested runtime/test head: `8d23b061307725589aef69a31a00249744523406`

CI run: `34467462173`

Artifact ID: `10148261046`

Artifact digest: `sha256:0011223e86c2dff7da92f98e030de2160c2c25d76e0dee605006e856c3dd2569`

The workflow artifact name may still say `field-photo-prep-phase-1-debug-apk`; the digest and runtime SHA, not the inherited artifact label, identify the build.

## Safe test hierarchy

Use only a disposable test area under the approved Google Drive test master. Do not use a live customer/job folder.

Preferred existing safe context where still valid:

`HNP Jobs → FIELD PHOTO PREP TEST`

Create additional disposable address/work-order names only when a gate requires a new object and the name is chosen to make the test item unmistakable.

Before any destructive Phase 3B action, visually confirm the full hierarchy and that every direct child in scope is disposable test content.

## One-time session preflight

Perform once before Install 1:

1. Confirm the phone is the intended Android test device and is functioning normally enough for camera and Drive-provider testing.
2. Confirm Google Drive is installed/signed in and the intended account can see the safe test master.
3. Confirm the test hierarchy contains no live job content.
4. If using ADB, confirm the device once with `adb devices`.
5. Stage all three APK artifacts and verify their SHA-256 digests once.
6. Confirm no runtime code changed after the exact artifact SHAs above. Documentation-only later commits do not invalidate those runtime artifacts.
7. Record device model, Android version, test date, and Drive provider/account context once for the session record.

If all seven are true, continue directly to Install 1. Do not repeat this preflight before each gate unless something changes.

# Gate A — Phase 3B Clear & Reuse

## Entry condition

Install 1 is installed and the operator is in the safe test hierarchy.

## Required straight-line observations

1. Select the exact disposable non-empty old work-order folder.
2. Confirm the app shows the correct master/address/work-order hierarchy and direct-child count.
3. Exercise **Cancel** once and prove no Drive content changes.
4. Re-enter the same disposable scenario.
5. Confirm **Clear & Reuse** once.
6. Prove only the confirmed direct children are removed.
7. Prove the folder becomes empty before rename.
8. Prove the same provider folder identity is renamed/reused for the new dated work order.
9. Confirm sibling/unrelated test content remains unchanged.

## Provider freshness rule

If the app says provider state is loading/stale/non-authoritative, allow one deliberate refresh/re-entry. If it still cannot establish authoritative-enough state, mark **BLOCKED — REAL PROVIDER FRESHNESS** and stop Gate A. Do not refresh repeatedly.

## Failure-path attempt

Attempt a deterministic delete/rename failure only if it can be induced safely without changing production code or threatening unrelated content. If not, record **NOT SAFELY INDUCIBLE** and move on; this is allowed by the integration contract.

## Exit

- PASS: all required safe observations are proven.
- BLOCKED: provider cannot establish authoritative-enough state after the one allowed refresh/re-entry.
- FAIL: wrong hierarchy, wrong child scope, wrong rename identity, unexpected deletion, or another product defect is observed.

Reread this master plan before installing Install 2.

# Gate B — Phase 4 Address Folder Creation

## Entry condition

Install 2 is installed and the safe master folder is selected.

## Required straight-line observations

1. Refresh/list the safe master once through the normal app path.
2. Select/reuse one known existing test address folder and prove no duplicate is created.
3. Create one unmistakably named disposable new address folder under the exact safe master.
4. Verify the actual Drive item exists under the intended parent with the expected folder type/name.
5. Reopen/re-enter the same address selection and prove the existing folder is reused rather than duplicated.
6. Prove its provider identity is persisted/reused and work-order discovery occurs beneath that exact address.
7. Confirm unrelated test master content remains unchanged.
8. If a safe exact-duplicate-name fixture already exists or can be created without clutter/risk, verify operator choice instead of guessing. Do not create needless duplicates solely to satisfy this optional observation.

## Provider freshness rule

As in Gate A, allow at most one deliberate refresh/re-entry beyond the app's normal settled-state verification. Persistent non-authoritative state is a BLOCKED result, not an invitation to loop.

## Exit

- PASS: reuse, one new create, reopen/reuse, identity, and unrelated-content checks pass.
- BLOCKED: real provider state cannot become authoritative enough for the create decision.
- FAIL: duplicate creation, wrong parent, wrong identity, or other product defect is observed.

Reread this master plan before installing Install 3.

# Gate C — Integrated Phase 5 + Phase 6A + Phase 6B-H4

This is one integrated phone run using the exact H2 tested build. It intentionally reuses the same captured photo/work occurrence to prove multiple inherited behaviors rather than repeating capture/preparation separately.

## Entry condition

Install 3 is installed. The app is connected through the Android system picker to the intended Google Drive test master. A distinct disposable test address/work-order destination exists.

## C1 — Phase 5 capture and temporary protection

1. Select the exact test address/work-order destination.
2. Confirm the displayed hierarchy is correct.
3. Take one ordinary still photo through the real system camera.
4. Confirm a non-empty protected local photo returns to the app.
5. Fully close/reopen the app once and prove the pending photo survives.
6. Change the currently selected work order and prove the captured photo still retains its original stored destination identity.
7. Return to the intended test occurrence.
8. Capture a second photo only if needed to prove per-photo independence; otherwise reuse existing automated evidence and avoid redundant manual work.

PASS C1 once real capture, restart survival, and immutable destination are observed. Do not repeat the camera test for confidence.

## C2 — Phase 6A real-photo preparation

Use the C1 real camera photo.

1. Prepare the photo once.
2. Confirm the UI remains responsive enough during preparation.
3. Visually inspect the prepared result for usable orientation/content.
4. Confirm the protected original still exists and was not replaced by the prepared copy.
5. Confirm conflicting Prepare/Discard/Take Photo actions cannot race the active preparation where observable in the normal UI.

Do not take a new photo just for C2 unless C1's photo is unusable for a clear non-product reason.

PASS C2 once the one real camera photo proves usable orientation, non-destructive preparation, and acceptable responsiveness.

## C3 — Phase 6B-H4 exact Drive upload

Use the same prepared C1/C2 photo whenever practical.

1. Confirm the queue record still points to the exact immutable original work-order provider identity.
2. Start one upload.
3. Confirm the created remote JPEG appears under that exact work-order parent.
4. Capture whatever app/log/provider evidence is available for the returned provider document identity.
5. Confirm the queue reaches `UPLOADED` only after the provider-visible write/verification path succeeds.
6. Confirm confirmed `remoteFileId` corresponds to the created provider identity and provisional state is cleared after success.
7. Confirm the protected original and prepared local copy still exist after H4 success because cleanup is not yet enabled.
8. Confirm unrelated Drive test content is unchanged.

The H2 automated tests already prove the internal create → durable provisional identity → byte write ordering. The physical gate exists to validate the real Android/Google Drive provider boundary; do not invent extra instrumentation during the phone session merely to re-prove an ordering already covered automatically unless real-device behavior contradicts it.

## C4 — interrupted/ambiguous upload evidence

Attempt one safe interruption only if there is a controlled method that does not risk uncontrolled duplicate creation—for example a clearly reversible connectivity interruption at a documented safe point.

If the outcome may be remotely ambiguous:

- verify the local photo remains recoverable;
- verify the original destination identity remains unchanged;
- verify the record becomes or remains non-retryable `UNCERTAIN` where appropriate;
- **do not press Upload again**;
- do not delete the uncertain remote object;
- record the state for later Phase 7B reconciliation design.

If a safe deterministic ambiguous condition cannot be induced, record that limitation. Do not loop or improvise increasingly risky failure attempts.

## Gate C exit

- PASS: C1, C2, and C3 are proven and any safely attempted C4 behaves fail-closed.
- BLOCKED: provider/device behavior prevents an honest required observation without exposing unrelated data.
- FAIL: photo loss, changed destination, duplicate unsafe create, false success, unusable preparation, or another product defect is observed.

Reread this plan before declaring Gate C result.

# Unexpected-result branch

This is the only permitted reason to leave the straight-line path.

When something unexpected occurs:

1. Stop the affected action before another risky side effect.
2. Capture the visible error/state and enough context to reproduce it.
3. Classify it as one of:
   - operator/environment issue before side effect;
   - provider freshness/availability block;
   - product defect;
   - uncertain remote side effect;
   - evidence-only limitation.
4. If it is an obvious operator/environment issue before side effect, allow the one controlled repeat.
5. If it is provider-blocked, product-defective, or remotely uncertain, do not repeat the risky operation.
6. Continue to a later independent gate only when doing so cannot hide or worsen the failure and does not depend on the failed behavior.
7. Do not patch code during the session.
8. Return to governed analysis with the captured evidence.

# Evidence package

Collect only evidence needed to support the gate result:

- test date and device/Android version;
- APK runtime SHA and artifact digest for each install;
- safe Drive hierarchy names used;
- screenshots for important before/after states;
- provider/document identities where the app or logs already expose them;
- queue state before/after upload where available;
- Drive-visible parent/name/type result for new folders/photos;
- explicit confirmation that unrelated safe-test content remained unchanged;
- exact unexpected error text if a gate blocks/fails;
- PASS / BLOCKED / FAIL per gate.

Do not turn evidence collection into continuous logging or screenshot every tap.

# Completion and decision flow

After the session ends or a true blocker stops it:

1. Reread this master plan.
2. Produce one consolidated device-gate record covering A, B, and C.
3. Reuse existing automated CI evidence; do not rerun complete suites merely because the phone session finished.
4. Identify which PR/device dependencies are now satisfied and which remain blocked.
5. Do not merge Level 3 work until the applicable required evidence exists and explicit operator pre-merge approval is given.
6. If H4 passes, use the real provider observations to design Phase 7B Remote Retry, Reconciliation & Cleanup.
7. During Phase 7B design, revisit the deferred lean-audit cleanup candidates only where the new reconciliation design proves whether they are needed.

## Phase 7B handoff questions to answer from H4

Before implementing Phase 7B, record the real provider answers to these questions:

- How quickly is a just-created/closed JPEG visible through the provider under the same parent?
- Does the returned provider document identity remain resolvable after reopen/restart?
- What metadata is reliably available immediately after write/close?
- What provider behavior is observed during safe interruption/ambiguity, if one was induced?
- What evidence is sufficient to distinguish confirmed existing remote content from authoritative-enough absence?
- Which local cleanup actions can safely occur after confirmed success without weakening recovery?

Do not answer these from mocks when H4 can supply real evidence.

## Explicit non-goals

This plan does not authorize:

- runtime code changes;
- production testing backdoors;
- repeated destructive Drive experiments;
- live customer/job data testing;
- blind retry of UNCERTAIN uploads;
- Phase 7B implementation before H4 evidence;
- merging any Level 3 PR without explicit operator approval;
- a general runtime lean-up/refactor phase.

## Final principle

**Straight line by default. Veer only for real evidence, not anxiety.**

The safety system exists to stop genuinely dangerous or ambiguous operations, not to create endless loops of prompts, checks, rebuilds, refreshes, or duplicate testing.