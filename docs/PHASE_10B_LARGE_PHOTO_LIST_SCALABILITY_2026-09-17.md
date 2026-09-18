# Phase 10B — Large Photo-List Scalability

Date: 2026-09-17

Status: **LEVEL 2 — COMPLETE**

Branch: `feat/phase-10b-photo-list-scale-20260917`

Rollback baseline: canonical `main` at Phase 10A completion.

## User-facing problem

Real work orders can contain roughly 100 or more photo records. The current Photos screen rebuilds every row inside one `LinearLayout` and decodes every thumbnail synchronously on the main UI thread whenever the list is refreshed.

At larger counts this creates unnecessary UI work, slower row selection/refresh, and avoidable thumbnail decode pressure.

## Approved behavior

- Keep the existing Home / Work Orders / Photos workflow.
- Keep exact photo ordering and current selection semantics.
- Preserve checkbox selection, Select All Ready, Clear, Upload Selected, Discard Selected, row `…` actions, selected-photo details, bulk reconciliation, and capture-order behavior.
- Virtualize the photo rows so only visible rows are inflated/bound.
- Move thumbnail decode work off the main UI thread.
- Keep selected-photo details in the same scrollable list surface as photo rows.
- Do not alter queue state, Drive state, upload/retry/reconciliation semantics, protected-original handling, destination identity, preparation policy, or deletion rules.

## Implementation choice

Use Android's built-in `ListView` with a fixed selected-photo header and a recycling `BaseAdapter`.

Reason:
- it provides the required row virtualization and recycling;
- it supports a fixed header without creating a second scrolling surface;
- it avoids adding a new UI dependency solely for this narrow performance repair;
- it preserves the existing XML row layout and action-owner model.

Thumbnail loading is moved to a small adapter-owned background executor with identity-checked delivery back to the visible row and a bounded in-memory cache.

## Owning files

Expected runtime owners:
- `PhotoCaptureActivity.java`: list data/model preparation and existing action owners.
- `PhotoListAdapter.java`: virtualized row binding and asynchronous thumbnail presentation only.
- `PhotoRowActionsView.java`: continue routing row `…` into the existing action owners without depending on a non-virtualized parent index.
- `screen_photos.xml`: replace the old ScrollView/LinearLayout photo container with the virtualized list and hidden existing action owners.
- `header_photo_selected.xml`: selected-photo detail header used by the list.
- `app/build.gradle`: internal version progression only.

Focused instrumentation tests are updated/added for the virtualized list contract and existing photo-row actions.

## Read surfaces

- persisted photo records already returned by `PendingPhotoStore.scan()`;
- prepared-copy existence already used for row state;
- existing local original/prepared file paths for thumbnail display only;
- current selection and busy-gate state.

## Write surfaces

No Drive write surface changes.

No persisted queue/schema write is added by the list.

Existing checkbox/selection changes continue to update only in-memory batch-selection state.

Thumbnail work reads local files only.

## Protected behavior

Must remain unchanged:
- protected original retention;
- prepared-photo creation policy;
- exact address/work-order provider identity;
- upload destination construction;
- sequential batch upload;
- provisional remote identity barrier;
- `FAILED` / `UNCERTAIN` semantics;
- strict reconciliation;
- confirmed cleanup;
- explicit local discard guards;
- Clear & Reuse behavior;
- capture-order sequence/occurrence behavior.

## Focused tests

Required:
- Photos screen uses a virtualized list rather than one child view per photo record;
- a 150-photo model does not create 150 simultaneous row views;
- row checkbox state and enabled/hidden behavior remain correct;
- row `…` still selects the exact photo and delegates to the existing action-owner buttons;
- selected-photo details remain available as the list header;
- existing Concept 3 interaction/render tests are updated for the virtualized surface.

## Final verification

- focused instrumentation coverage;
- one complete Android CI run on the exact final runtime head;
- focused Samsung smoke with a realistic large local photo count:
  - open Photos;
  - scroll through a large list;
  - select/unselect rows;
  - open row actions;
  - confirm thumbnails fill without blocking basic scrolling/taps;
  - confirm no queue or Drive action occurs merely from scrolling.

## Primary risks

- recycled rows displaying stale checkbox/selection state;
- asynchronous thumbnail result landing in a row that has since been recycled;
- row action popup selecting the wrong photo after recycling;
- selected-photo header changing scroll behavior;
- tests that assumed all rows existed simultaneously needing correct virtualization-aware assertions.

Mitigations:
- full row rebind on reuse;
- clear checkbox listener before binding then restore it;
- tag thumbnail views with an immutable load key before background work;
- row click binds directly to that record ID;
- row action menu anchors to the clicked control after row selection rather than relying on child index.

## Smoke checks

Affected regression areas:
- I. Upload status presentation only;
- K. selection must not delete or change queue state;
- M. Photos portion of minimal field workflow.

No Google Drive integration impact. No real Drive smoke is required solely for this Level 2 list-performance change.

## Approval

Implementation authorization: approved by the operator's instruction to proceed with the roadmap.

No separate Level 3 pre-merge approval is required unless scope expands into persisted state, Drive identity, upload/retry, deletion, or provider behavior.


## Completion evidence

- merged through PR #49 to `main` at `110477c286bace05bb7fa56ee22199cf2de7fa92`;
- exact runtime head `8140dcfa8701d0b4e37cbda7af4f0713f376b449`;
- Android CI run `35295211103`: PASS;
- unit tests, internal debug build, stable signer verification, full instrumentation, launch smoke, and artifacts passed;
- focused 150-photo virtualization instrumentation passed;
- focused asynchronous local-thumbnail instrumentation passed;
- operator Samsung smoke on the exact green 0.21 internal APK passed for large-list scrolling, selection, row actions, and thumbnail population;
- no Drive write, discard, Clear & Reuse, or provider change was required for the Level 2 physical gate.

The production behavior change is limited to Photos-list presentation/performance. Queue, Drive, destination, upload, retry, reconciliation, and deletion semantics remain unchanged.
