# Phase 12I — App Status & Diagnostics Impact Record

Date: 2026-09-26

Status: **ACTIVE — PRE-IMPLEMENTATION**

## Goal

Give the operator one lean, read-only App Status screen that explains current Field Photo Prep account, Organization, Drive-binding, permission, and protected-work/queue state without exposing sensitive internals or creating a second state machine.

The screen must consume existing owners. It must not become an authorization, Drive, queue, or persistence owner.

## Classification

**Level 2**

Reason:
- ordinary Android screen/navigation/presentation;
- read-only derivation from existing local/runtime owners;
- no stored-data schema change;
- no Supabase schema/RLS/RPC/Edge Function change;
- no Drive destination, provider identity, permission, upload, retry, cleanup, or destructive semantic change.

If implementation discovers a need for a new persisted diagnostic state, new provider/account selection semantic, new remote operation, raw provider identity exposure, or a second authorization decision, stop and reclassify/split before continuing.

## Authoritative line

- governed base / rollback: `3962bad69add448a300fddc1eff3fcd6f2b26002`;
- base is the governed Phase 12G merge commit from PR #81;
- authoritative branch: `phase-12i/app-status-diagnostics`;
- authoritative PR: created after this preflight commit;
- no competing 12I branch or PR existed at takeover;
- unrelated draft PR #39 retains its UNCERTAIN-reconciliation scope and is not part of 12I.

## Required rule packs

- `GOVERNANCE.md`;
- `PROJECT_PROFILE.md`;
- `RULE_INDEX.md`;
- `CHANGE_CONTROL_CONTRACT.md`;
- `TESTING_CONTRACT.md`;
- relevant identity/photo-protection/queue/Drive portions of `CONTRACT.md`;
- affected sections of `REGRESSION_CHECKLIST.md`;
- `INTEGRATION_CONTRACT.md` for display/delegation around existing Drive binding;
- `rules/testing/DRIVE_PROVIDER.md` only if runtime work begins to alter or directly depend on provider behavior beyond reading the existing 12H binding result;
- `docs/IDENTITY_MODEL_V1.md`;
- `docs/PHASE_12_COMPLETE_DESIGN_2026-09-25.md`;
- `docs/PHASE_12_MASTER_PLAN_2026-09-25.md`.

## Existing owners 12I must consume

### Account / Organization

`RuntimeAuthorizationManager` and `AuthorizationDecision` already own:
- current authorization state;
- rounded grace-remaining source value;
- current User/Organization authorization identity;
- serialized Recheck Account behavior.

`AuthSessionState` already owns display context:
- Organization name;
- role;
- Membership status;
- last successful Membership validation time.

12I may display those values but may not persist a second permission/account flag.

### Drive

`OrganizationDriveBindingGuard` already owns the local Organization↔Drive usability decision.

`FolderPrefs` already owns local display context for:
- selected workspace/company;
- current provider-bound navigation.

12I may display a coarse Drive state and current Client Company name locally.

Support copy must never include:
- SAF URI;
- provider document IDs;
- Google account identity;
- raw provider errors.

No new Drive integration path is authorized. If Connect Drive is exposed from App Status, it must delegate to the existing MainActivity/12H SAF path and existing guard; it may not own folder selection itself.

### Protected work / queue

`PendingPhotoStore` and `PendingPhotoRecord.State` already own durable queue truth.

`ProtectedWorkGuard` already owns read-only protected-work/sign-out-blocking derivation.

12I may derive display counts from those stores without changing any record.

Expected display counts may include:
- CAPTURING;
- WAITING;
- UPLOADING;
- FAILED;
- UNCERTAIN;
- confirmed UPLOADED records where useful;
- protected originals;
- cleanup-pending/unreadable counts where useful.

No action on the status screen may retry, reconcile, upload, discard, delete, or rewrite those records.

### App / Android permission

Use existing Android permission APIs and `BuildConfig` for:
- app version;
- camera permission status.

No new platform permission is authorized.

## Local display allowlist

The normal App Status screen may show:
- signed-in / sign-in-required account state;
- active FPP Organization name;
- Membership role/status;
- last successful Membership validation time;
- current authorization state;
- rounded grace remaining when in GRACE;
- coarse Drive binding/connection state;
- current Client Company name where useful;
- queue/protected-work counts;
- app version;
- camera permission status;
- one practical next action/message consistent with the approved Phase 12 design.

The screen may show user-recognizable local context needed to operate the app. It must not show secrets or raw internal identifiers.

## Copied support-summary allowlist

Copy Support Status is stricter than the local screen.

It may include only:
- coarse authorization state label;
- coarse Drive state label;
- role when useful;
- rounded grace remaining;
- last-validation time rounded/coarsened;
- queue/protected counts by safe state label;
- camera permission state;
- app version.

It must exclude:
- access/refresh tokens;
- passwords;
- email by default;
- Organization UUID / User UUID / Membership UUID;
- Organization/client-company names by default;
- addresses;
- work-order names;
- notes;
- photos;
- SAF URI;
- provider document IDs;
- Google account identity;
- raw error strings that could contain sensitive values.

A strict allowlist builder is preferred over redacting a verbose/raw dump.

## Read/write surfaces

### Reads

12I may read:
- `RuntimeAuthorizationManager.currentDecision()`;
- `RuntimeAuthorizationManager.storedSession()`;
- `OrganizationDriveBindingGuard.current()`;
- `FolderPrefs` display context;
- `PendingPhotoStore` scan results;
- `ProtectedWorkGuard` read-only result;
- camera permission;
- `BuildConfig.VERSION_NAME`.

### Writes

The diagnostics model/screen writes no diagnostic persistence.

Permitted UI-side effects only:
- copy a strict support-summary string to Android clipboard;
- invoke the existing account recheck owner;
- navigate/delegate to the existing Connect Drive owner when allowed;
- navigate back.

No Supabase or Drive mutation is owned by 12I.

## Primary risks

- accidentally exposing tokens, IDs, SAF URIs, emails, customer addresses, or raw errors in copied support text;
- duplicating authorization logic instead of rendering `AuthorizationDecision`;
- counting queue state from UI rather than durable store truth;
- causing diagnostics to mutate or reconcile queue records;
- confusing FPP Organization identity with Drive account/workspace identity;
- showing stale Drive/company context after a wrong-Organization/invalid binding;
- creating a second Recheck Account or Connect Drive implementation instead of delegating existing owners.

## Verification plan

Focused automated coverage must prove:
- state-to-label mapping for all authorization states;
- grace display is rounded and never extends authorization;
- support-summary output is strict allowlist and contains no tokens, IDs, URIs, email, address/work-order content, or raw error text;
- queue counts are derived read-only from durable queue states;
- unreadable/protected work is represented without destructive action;
- Drive status comes from the existing 12H binding result;
- wrong-Organization/permission-missing/legacy/invalid states do not expose stale company/provider identity as usable;
- Recheck Account uses the existing authorization manager;
- Connect Drive, if present, delegates to the existing guarded Main/SAF path;
- Home/App Status navigation does not change Home/Work Orders/Photos behavior.

Final runtime gate:
- focused tests PASS;
- full Android CI PASS once on the exact final runtime head;
- affected emulator/UI smoke PASS;
- no real Drive reality gate is required if provider semantics remain unchanged.

## Google Drive integration impact

**No new Google Drive integration impact is authorized.**

12I may display the existing 12H binding result and may delegate to the already-approved Connect Drive owner. It must not alter SAF/provider access, folder identity, provider freshness, Drive writes, or permissions.

## Rollback

Revert 12I runtime/UI commits to governed base `3962bad69add448a300fddc1eff3fcd6f2b26002`.

Rollback must not clear or rewrite:
- FPP auth/session state;
- Organization↔Drive binding;
- SAF permissions;
- selected company/address/work-order provider identities;
- queue/reconciliation records;
- protected originals/prepared copies;
- Drive content.

## Stop conditions

Stop and reconcile/reclassify if:
- a new persisted diagnostic cache/state appears necessary;
- support copy needs sensitive identifiers to function;
- a new provider/SAF path appears necessary;
- the screen would perform Drive mutations rather than delegate existing owner behavior;
- queue inspection would require changing/reconciling queue state;
- account status requires a second authorization decision;
- scope expands into 12J recovery flows or 12K release/signing.
