# Phase 12E — Runtime Authorization Enforcement Build State

Last updated: 2026-09-25

This file is the durable handoff point for Phase 12E. Do not rely on chat history to determine progress.

## Governed base

- main base at start of Phase 12E runtime work:
  - `0e1104db2c2d6026a427bfa7b5baa9213b2fac25`
- implementation branch:
  - `phase-12e/runtime-authorization-enforcement`
- draft PR:
  - `#72 Phase 12E: runtime authorization enforcement`
- Level 3:
  - do not merge without explicit operator approval

## Completed and committed

- central immutable authorization decision
- deterministic 72-hour grace policy
- exact grace-boundary tests
- wall-clock rollback fail-closed behavior
- owner/member role behavior
- authoritative revocation classification
- persisted revocation blocks offline grace resurrection
- protected-work sign-out guard derived from durable queue/files
- stored-membership revalidation classifier
- serialized/coalesced runtime authorization owner
- rotated refresh tokens persisted before membership validation
- startup authorization revalidation
- foreground authorization revalidation
- Drive folder create gate
- Drive folder rename gate
- Drive delete gate
- photo upload-attempt gate before queue mutation
- remote Drive photo-create gate
- photo screen capture gate
- camera shutter recheck before every new capture
- camera shutter disabled when new capture authority is unavailable
- stale revalidation/session-generation barrier in progress and committed

## CI evidence already observed

Passing snapshots:
- central authorization policy: unit tests PASS
- protected-work sign-out guard: unit tests PASS
- protected-work sign-out guard: debug build PASS
- protected-work sign-out guard: full Android CI PASS

One superseded failure:
- an earlier runtime-manager snapshot failed only because `Observation.kind` was private
- that compile issue was fixed in a later commit

Do not treat superseded CI failures as current-head failures.

## Remaining Phase 12E runtime work

1. verify latest runtime-manager/session-generation head compiles and tests
2. wire authenticated-session replacement through the central manager
3. add explicit Sign Out UI
4. block Sign Out while protected work exists
5. prove sign-out/revalidation race cannot restore an old session
6. finish any remaining lower-level mutation bypass audit against current branch
7. add focused gate tests for denied Drive/capture paths
8. run full Android CI on final head
9. collect final PR changed-file list and test evidence
10. update Phase 12E completion documentation
11. stop for explicit Level 3 merge approval

## Working rule

Every future Phase 12E change must:
1. touch one bounded concern,
2. commit immediately,
3. let CI validate it,
4. update this build-state file when the checkpoint materially changes.

Do not restart the Phase 12E audit unless repository evidence proves this build-state file is stale or wrong.
