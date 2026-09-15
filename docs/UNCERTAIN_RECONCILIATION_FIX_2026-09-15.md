# UNCERTAIN Reconciliation Provider-Freshness Fix — 2026-09-15

## Classification

Level 3. This change touches remote reconciliation and retry-release safety after an ambiguous Google Drive upload result.

## Field defect

On a Samsung Galaxy S22 using the current Field Photo Prep build, one of three uploaded photos entered `UNCERTAIN`. The operator invoked reconciliation and the photo remained `UNCERTAIN`.

## Root cause

The current Android reconciliation path over-trusts two advisory provider signals:

1. `ContentResolver.refresh(...) == false` is treated as proof that reconciliation cannot continue. Android documents `false` only as meaning the provider did not perform that optional refresh request; providers may simply not implement refresh.
2. Provider-reported `COLUMN_SIZE` is treated as a hard mismatch before SHA-256 content proof. Cloud-backed DocumentsProvider metadata can be stale immediately after a write, including a temporary zero or old byte count.

The permanent integration contract already requires refresh only when supported and requires strong content proof for uncertain uploads. The defect is therefore an implementation mismatch, not a product-contract change.

## Approved scope

Only `UNCERTAIN` reconciliation is changed.

- Keep exact provisional provider identity first.
- Keep deterministic filename fallback under the immutable stored work-order provider identity.
- Keep `EXTRA_LOADING` fail-closed behavior.
- Keep two matching settled child snapshots before absence/cardinality is trusted.
- Treat Android `refresh()` as best-effort only; lack of provider refresh support does not by itself block the settled-query path.
- Treat provider byte size as advisory during reconciliation; exact id/name/MIME plus SHA-256 of the remote bytes must prove a match before `UNCERTAIN -> UPLOADED`.
- Preserve `UNCERTAIN` when remote content cannot be read or its hash differs.
- No remote create, delete, overwrite, rename, move, permission change, or destination substitution occurs during reconciliation.

## Read/write surfaces

Reads: persisted queue metadata, prepared local JPEG, persisted master tree URI, immutable work-order provider ID, provisional remote provider ID when present, remote candidate metadata and bytes.

Writes: only the existing local reconciliation result transition after proof (`UNCERTAIN -> UPLOADED` or existing retry-safe absence transition). This patch itself adds no new queue state or schema.

## Protected behavior

- No blind retry from `UNCERTAIN`.
- No second Drive create during reconciliation.
- No destination change.
- No deletion of unconfirmed local photo data.
- No schema, permission, camera, preparation, folder, batch-selection, or signing change.

## Focused verification

Add regression coverage proving a candidate with stale provider size `0` still confirms only when the remote SHA-256 exactly matches the prepared local JPEG. Existing tests continue to cover mismatched content, duplicate candidates, `EXTRA_LOADING`, inconsistent snapshots, and fail-closed uncertainty.

The complete Android CI suite must pass on the final branch runtime head before an APK is staged.

## Real-provider gate

The operator's S22 observation is the triggering real-provider evidence. After CI passes, update the app in place and reconcile the same preserved `UNCERTAIN` photo once. Do not retry-upload it first. Expected result:

- if the existing Drive object contains the prepared photo bytes, reconciliation reaches `UPLOADED` without another remote create;
- if the remote bytes cannot be proven identical, it remains `UNCERTAIN` and the app must not retry automatically.

No merge to `main` is authorized until the Level-3 device result is reviewed and explicit pre-merge approval is granted.

## Rollback

Rollback baseline: `6c73909a0c00948643379b4fce9456c0b1d24ece`.

Revert the narrow reconciliation commit. Queue schema is unchanged, so preserved local `UNCERTAIN` records remain readable by the baseline build.
