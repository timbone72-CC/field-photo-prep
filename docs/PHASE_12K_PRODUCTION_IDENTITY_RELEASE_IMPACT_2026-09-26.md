# Phase 12K — Production Identity / Release Path Impact Record

Date: 2026-09-26

Status: **ACTIVE — LEVEL 3 PRE-IMPLEMENTATION**

## Goal

Prepare a controlled, verifiable production APK/update path for Field Photo Prep without deciding Play Store/public distribution and without placing production signing material in source control.

## Classification

**Level 3 — deployment/signing/runtime trust**

This phase changes the production build/signing and delivery trust path. It therefore requires:
- dedicated branch and PR;
- full impact record;
- focused and complete automated verification;
- release artifact identity/signature/hash evidence;
- install/update reality verification on Android;
- exact rollback record;
- **explicit operator approval before merge**.

## Governed base / rollback

- governed base: `c502260b7207d75a1349581a2f72b397e7fa5e70`;
- base is the Phase 12J merge commit from PR #83;
- rollback source commit: `c502260b7207d75a1349581a2f72b397e7fa5e70`;
- authoritative branch: `phase-12k/production-identity-release-path`;
- PR: created immediately after this preflight checkpoint;
- no competing 12K branch/PR existed at takeover.

If the release candidate cannot be proven safe, do not publish/install it as production. Revert the 12K source changes to `c502260b7207d75a1349581a2f72b397e7fa5e70` or continue using the known-good internal package; do not uninstall or clear protected app data merely to recover from release tooling.

## Current verified baseline

Repository:
- production namespace/application ID: `com.inandout.fieldphotoprep`;
- internal/debug package: `com.inandout.fieldphotoprep.internal`;
- production callback identity: `com.inandout.fieldphotoprep://auth-callback`;
- internal callback identity: `com.inandout.fieldphotoprep.internal://auth-callback`;
- current versionCode on governed base: `36`;
- current versionName: `0.27.1-auth-session-hardening`;
- internal CI uses the intentional public/non-production stable test signer;
- `*.jks` and `*.keystore` are ignored except the intentional public test keystore;
- no production signing configuration/workflow currently exists.

Live dedicated FPP Supabase project verified through the connected Supabase management surface:
- project ref: `vtyiktvqhbgabawotkrj`;
- project name: `Field Photo Prep`;
- status: `ACTIVE_HEALTHY`;
- URL: `https://vtyiktvqhbgabawotkrj.supabase.co`;
- active modern publishable key matches the Android source value: `sb_publishable_QXGedlj5wCmMpV2HncZp9A_kfgLzuAQ`;
- legacy anon key also exists but is not required by the Android release path.

The connected management surface does not expose the Auth redirect allowlist. Therefore the production redirect `com.inandout.fieldphotoprep://auth-callback` is **not claimed verified yet**. It remains a required release reality gate.

Historical signer/package audit:
- the stable non-production test signer was introduced on 2026-09-10;
- before the later 2026-09-10 package split, debug/test builds briefly used unsuffixed `com.inandout.fieldphotoprep`;
- commit `ceda1509b0b2c94b602b76b312c828ff89f727bb` introduced `.internal` for debug builds;
- the suspected versionCode 18 / 0.13-field-ui CI artifact is confirmed as `field-photo-prep-internal-apk`, not a production artifact;
- nevertheless, an older pre-split test-signed unsuffixed package may still exist on a device unless explicitly proven absent.

The read-only device package/signer check is now complete:
- Android user list shows user 0 (`Tim Rush`) and Secure Folder user 150;
- visible FPP packages are `com.inandout.fieldphotoprep.internal` and `com.inandout.fieldphotoprep.team.internal`;
- `adb shell dumpsys package com.inandout.fieldphotoprep` returns `Unable to find package`.

Because Android PackageManager has no unsuffixed production package registered, there is no installed production signer/update lineage to preserve on this device. Phase 12K may establish the first permanent production signer.

## Approved implementation scope

### 1. Separate production signing path

Add a release signer configuration that is populated only from external environment/secrets.

Expected inputs:
- `FPP_RELEASE_STORE_FILE`;
- `FPP_RELEASE_STORE_PASSWORD`;
- `FPP_RELEASE_KEY_ALIAS`;
- `FPP_RELEASE_KEY_PASSWORD`;
- expected production certificate SHA-256 fingerprint.

Rules:
- no production keystore bytes in git;
- no production passwords in git;
- no production private key material in APK resources/source;
- debug/internal continues using the existing public test signer;
- release build must fail closed if required signing inputs are missing.

### 2. Preserve production package/update identity

Production remains:
- package/application ID: `com.inandout.fieldphotoprep`;
- auth callback: `com.inandout.fieldphotoprep://auth-callback`.

12K must not change:
- internal package/callback identity;
- Android private-data ownership semantics;
- FPP User/Organization identity;
- Drive provider identity;
- queue/photo destination identity.

The production candidate versionCode must be greater than governed-base versionCode `36`. The planned 12K candidate is `37`.

Permanent production signer established on the operator-controlled Linux laptop:
- alias: `field-photo-prep-production`;
- certificate SHA-256: `8B:A1:DC:6A:E4:90:72:74:A7:DD:B1:66:62:A4:6A:7B:B5:2A:0F:CA:5D:BE:AD:EB:47:80:01:05:2F:D2:A1:C2`;
- keystore file SHA-256 at creation: `5fc4dc15a73fbf3f8034c4a6a24389146a921f0d15bc9032995912e8205aa5f6`;
- keystore bytes and passwords remain outside git and are not recorded in this repository.

### 3. Manual controlled release-candidate workflow

Add a manual-only GitHub Actions workflow that:
- receives production signing material only through repository/environment secrets;
- materializes the keystore only under runner temporary storage;
- builds a signed release APK;
- verifies package/application ID;
- verifies monotonically advanced versionCode;
- verifies production auth callback identity;
- verifies release certificate SHA-256 against the expected fingerprint;
- verifies only the dedicated FPP Supabase URL and modern publishable key are embedded as client configuration;
- fails if obvious secret/service-role key patterns are found;
- computes APK SHA-256;
- records source commit, package, version, signer fingerprint, artifact hash, callback, and Supabase project ref;
- uploads the APK plus release evidence as private workflow artifacts;
- does **not** create a public GitHub Release, Play Store upload, or external distribution automatically.

### 4. Local verification/runbook

Add a release verification script/runbook usable against a locally built or downloaded candidate.

The runbook must preserve:
- exact signer fingerprint;
- exact APK SHA-256;
- exact versionCode/versionName;
- exact source commit;
- rollback APK/commit;
- install-over-existing observations.

## Required data

Required to create a real signed production candidate:
- one secured production keystore — **SATISFIED LOCALLY**;
- its alias/passwords;
- expected certificate SHA-256 fingerprint;
- 12K source commit;
- monotonically increasing versionCode;
- dedicated FPP Supabase URL and active publishable key;
- production callback `com.inandout.fieldphotoprep://auth-callback`.

Required before creating the permanent production signer: **PASS**.
- physical Android device checked;
- unsuffixed `com.inandout.fieldphotoprep` is absent;
- no legacy production-package signer lineage needs to be recovered.

Required before merge:
- code/config verification;
- complete Android CI;
- signed release-candidate verification;
- production callback/recovery reality test on Android;
- install/update continuity result appropriate to the available existing production install state;
- explicit operator pre-merge approval.

## Optional/deferred data

Not required for controlled current-use 12K:
- Play Store account/listing;
- public website/domain;
- verified HTTPS Android App Link;
- monetization/subscriptions/licensing;
- open public self-signup;
- custom SMTP for current owner-only controlled use.

Before invitation to outside users or public distribution:
- production-suitable business-controlled Auth email/SMTP and recovery/rate behavior become required;
- verified HTTPS App Link should be reconsidered when an owned domain is available.

## Schema / identity / permission / platform-access changes

Database schema: **none**.

Supabase RLS/RPC/Edge Functions: **none**.

FPP User/Organization identity: **none**.

Google Drive/SAF permissions: **none**.

Android package identity:
- production stays `com.inandout.fieldphotoprep`;
- internal stays `com.inandout.fieldphotoprep.internal`.

Platform trust change:
- a new secured production signing certificate becomes the permanent Android update identity for production unless an already-existing production signer is proven and intentionally retained.

This is the primary Level-3 risk.

## Master-folder / destination assumptions

12K does not change:
- Drive master/workspace selection;
- Organization↔Drive binding;
- Client Company selection;
- property/work-order/photo folder identity;
- queued destination snapshots;
- upload/retry/reconciliation behavior.

Existing 12H/queue evidence remains authoritative.

## Duplicate / idempotency behavior

No job/photo/Drive duplicate semantics change.

Release artifact identity is deterministic evidence:
- one APK hash identifies one exact candidate;
- one certificate fingerprint identifies the signer;
- one source commit/versionCode pair identifies the source lineage.

Re-running the workflow for the same commit may produce a new workflow artifact record but must not alter runtime business data.

## Offline / stale-state behavior

No auth grace, offline capture, queue, or stale-Drive behavior changes.

A release/update must preserve app-private data when Android accepts it as a same-package/same-signer update.

A signer mismatch must fail installation/update; it must never be worked around by uninstalling a live production app with protected work.

## Safe Drive fixture plan

No new Drive/provider behavior exists in 12K, so no new destructive/remote Drive fixture is required.

Post-install smoke may open the existing app and verify previously proven Drive state remains intact. It must not clear/rebind Drive merely to test release packaging.

## Baseline / expected verification

Baseline:
- current internal Android CI green on governed main;
- internal stable test signer remains unchanged;
- production release currently has no governed signing workflow.

Expected:
- debug/internal full CI remains green;
- production release configuration static checks pass without production secrets;
- manual release workflow fails closed when signing secrets are absent;
- with real production signer inputs, signed release APK verifies expected package/version/callback/Supabase client config/signer/hash;
- production callback/recovery opens the production package on Android;
- installing an update over an existing same-signer production installation preserves app-private state; if no previous production installation exists, establish the first signed production baseline and record that update-over-existing proof remains a subsequent candidate gate.

## Failure recovery

If build/signing verification fails:
- do not distribute/install the candidate;
- correct tooling/config on the branch;
- keep production key unchanged unless compromise is proven.

If Android reports signer mismatch against an existing production install:
- stop;
- do not uninstall the existing app;
- identify the signer currently protecting that installation;
- preserve protected work and Drive state;
- either recover the original production key or treat migration as a separately designed Level-3/4 operation.

If auth callback fails:
- do not weaken redirect parsing or use localhost fallback;
- verify Supabase allowed redirect configuration and exact release scheme/host;
- keep internal callback separate.

## Protected behavior

Must remain unchanged:
- protected originals and prepared copies;
- queue/reconciliation records;
- immutable upload destinations;
- 12E auth/session enforcement;
- 12H Organization↔Drive binding;
- 12G first-run/invitation behavior;
- 12I diagnostics privacy boundary;
- 12J recovery UX;
- internal package/test signer behavior;
- backup exclusions for auth/provider-sensitive state.

## Explicit pre-merge approval

Status: **APPROVED — 2026-09-27**

Operator explicitly approved the Level-3 Phase 12K merge after reviewing the completed production signer, signed APK, physical install, callback recovery, off-laptop backup, GitHub signing-secret, and CI evidence. No scope expansion was authorized.

## Local signed production candidate evidence

Verified on source commit `6065d48b943c657cae0ef3011c92fdc67213db44`:
- package: `com.inandout.fieldphotoprep`;
- versionCode: `37`;
- versionName: `0.28.0`;
- callback: `com.inandout.fieldphotoprep://auth-callback`;
- dedicated Supabase project ref: `vtyiktvqhbgabawotkrj`;
- modern publishable key present: yes;
- production signer SHA-256: `8ba1dc6ae4907274a7ddb16662a46a7bb52a0fca5dbeadeb478001052fd2a1c2`;
- APK SHA-256: `3e0785965531a56a4e5abc98ae09f070d1e69dcf1c8099e7804f96c85bfbe098`.

The local verifier completed successfully after the Phase 12K SIGPIPE fix in `scripts/verify-release-apk.sh`.

## Physical first-production install result

PASS on the operator Android device:
- signed production APK installed successfully without uninstalling the working internal package;
- `com.inandout.fieldphotoprep` resolves to an installed APK path;
- `dumpsys package` reports versionCode `37`, versionName `0.28.0`.

This establishes the first production-package install baseline using the permanent signer recorded above. Production auth callback reality verification subsequently passed as recorded below.

## Production auth callback reality result

**PASS** on the physical Android device.

Observed flow:
1. real Supabase recovery email generated with `redirect_to=com.inandout.fieldphotoprep://auth-callback`;
2. recovery link opened the signed production FPP package;
3. app rendered the Set Password screen;
4. password update succeeded;
5. app returned to Account Connected with the expected organization and OWNER role;
6. no localhost/browser dead end and no internal-package redirect occurred.

## Production signer off-laptop backup

**PASS**. An encrypted removable-media backup was created and verified by decrypting it through SHA-256. The recovered keystore hash is `5fc4dc15a73fbf3f8034c4a6a24389146a921f0d15bc9032995912e8205aa5f6`, exactly matching the original production keystore hash. No keystore bytes or passphrases are stored in the repository.
