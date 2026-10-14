# Field Photo Prep — Two-Tone Visual Refinement Blueprint

Date: 2026-10-14  
Status: **DESIGN/IMPLEMENTATION PLAN — NO RUNTIME CHANGE; visual sign-off and implementation evidence pending**  
Project: original **Field Photo Prep** Android app (FPP), not Field Work Hub (FWH).  
Plan authority: operator's 2026-10-14 current-app screenshot and conversation (prioritize **company selector**, then **Properties heading**; prefers two-tone green; wants matching underlying pages; no invented functionality).  
Governed planning base: `main` **1772db779576f8e42f7bc73b6c4b7b3a61c794ce**.  
Independent active line: draft PR **#102**, `feat/phase14-shared-cycle-foundation`, owns Phase 14 coordinator work and changes **MainActivity**, **row_work_order.xml**, and roadmap; draft PR **#96** owns unrelated GitHub Actions hardening. Neither is included or superseded.  
Classification: **Level 1 documentation-only plan**. Planned runtime work: **Level 2 Android UI/presentation**, with reclassification required if any persisted, photo, Drive, safety, or camera interaction behavior changes.

## 1. Objective and design lock

Make the existing FPP dark theme look more polished and consistent across real screens while retaining its familiar field workflow. Use **two-tone forest green** primarily for the company/workspace selector, quiet green for selected/success surfaces, and **existing blue for primary non-destructive actions**. Keep existing photo upload actions and their present meaning. Avoid full rebuilds, a new UI framework, duplicated logic, added steps, decorative dashboards, imaginary counts, or controls that FPP does not have.

The generated concepts are **style suggestions, not factual UI inventories or exact Android specs**. The current installed-app screenshot is the Home reference. Before runtime implementation, take current-version, privacy-safe baseline screenshots of **Work Orders, Photos, and relevant dialogs** (and evaluate the working camera); freeze a **sanitized Android reference** with actual content dimensions and correct existing controls. In a public repository, never commit screenshots exposing actual customer addresses, names, email addresses, Drive IDs, or protected photos. Use disposable fixtures/synthetic labels.

**Visual scope preference, highest first:**
1. Home's company selector (two-tone green) with real company selection, Drive/workspace status, refresh, and connection affordances preserved.
2. Home Properties heading/count/New Address alignment at narrow width and large font.
3. Consistent typography, cards, navigation, button shapes, status surfaces across existing Home / Work Orders / Photos.
4. Supporting account, App Status, member-management, and existing confirmation/dialog visual consistency **without altering their logic**.
5. Camera **review only by default**: its preview/shutter/Done/flash/torch/zoom/orientation controls remain locked. Any camera visual or interaction change needs a separate explicit scope and its real-device gate.

## 2. Verified current implementation and proposed style tokens

Verified on `main`: XML layouts `screen_home_properties.xml`, `row_home_property.xml`, `screen_work_orders.xml`, `row_work_order.xml`, `screen_photos.xml`, `row_photo.xml`, `field_bottom_navigation.xml`; `screen_app_status.xml`, `screen_auth.xml`, `screen_member_admin.xml`; `values/colors.xml` and `values-night/colors.xml`. `MainActivity` owns navigation/Drive-facing controls and existing interactions; `CameraCaptureActivity` is protected. See `docs/CONCEPT_3_NON_CAMERA_UI_IMPLEMENTATION_RECORD_2026-09-12.md`, `docs/PHASE_9_UI_MAKEOVER_RESET_PLAN_2026-09-12.md`, `docs/PHASE_10E_GUIDED_NEXT_ACTION_2026-09-17.md`, and Phase 13 lifecycle build-state.

**Current dark palette (verified, not proposed replacements):** background `#101512`; surface `#18201B`; surface alt `#202B24`; primary text `#F2F7F3`; secondary `#B2BDB5`; soft green `#173A2B`; green `#55D99D`; blue `#78B2FF`. **Current light palette** includes background `#F5F8F6`, surface white, green `#0B8B5C`, blue `#1473E6`.

**Proposed visual system, to be approved from actual rendered screenshots:**
- Keep both light and dark themes, and retain existing semantic status/error/warning colors. Add only the minimum shared token(s) needed to create the **two-green company panel** (e.g. a dark/medium forest surface pair in night mode) with readable text and true status labels. Avoid changing existing semantic color keys globally without visual review of every affected screen.
- Use one shared card treatment, one typographic hierarchy, consistent screen insets and spacing, and existing rounded controls; target compact density rather than adding header towers or full-width decorative imagery. Token sizes are adjusted against actual Android screenshots, not assumed from concept-art pixels.
- Maintain clear primary-action hierarchy: existing blue Create/Open/Camera next action, green Upload Selected as currently designed, quieter Edit/Clear & Reuse/destructive controls. Never recolor a destructive action as an ordinary success button.
- Preserve at least **48dp** usable hit targets for relevant touch controls; support large text and content descriptions. Maintain accessible contrast, focus, disabled/loading states, and safe system/navigation-bar insets.
- New iconography may reuse existing vectors; do not introduce photographic property thumbnails, map pins, progress charts, mock rating/status badges, new statistical tiles, or fake source data.

## 3. Locked screen-by-screen control inventory and visual targets

### Home / Properties — primary reference

**Existing real controls:** app title; `...` Drive/options path; selected company/workspace selector; Drive status dot/text, refresh and connection action; existing status message; Properties heading/count; New Address; Search addresses (including archived matches); addresses ready for review; active address rows with Last used and per-row `...` lifecycle actions; existing guided **Next** action; Home/Work Orders/Photos bottom navigation.

**Target:** two-tone compact company card, company name clearly primary; workspace and connection state secondary, not confused with the company name. Keep chevron and refresh separate, legible and independently clickable, and preserve the Connect action when needed. Ensure long names, missing workspace, disconnected/error/loading/permission states are readable without clipped labels or a fictitious green connected dot. **Do not create a new login, refresh, picker, or connection workflow**.

**Properties layout fix:** left side shows “Properties” and property count in a bounded, readable group; right side retains New Address with a large touch target. The count **must not wrap into the button or split awkwardly as “pro / perties”** on the operator's Samsung font/display scale. A stacked count below heading or equivalent adaptive layout is preferable to truncation; do not conceal the count. Search and row menu remain obvious.

Keep synthetic/property icon rows (not house photographs), two-line addresses when needed, muted Last used, safe row-menu target, existing ordering/search/archive behavior. Retain the guided Next button and bottom navigation; list remains scrollable above them, without controls overlapping rows.

### Work Orders — same system, same behavior

**Existing:** back, refresh, selected address and current WO context, selectable dated WO list, selected Open Photos, Edit Work Order, Clear & Reuse, work-order name/date/new dated work-order action, status text and bottom navigation. PR #102 proposes a **read-only In Drive** photo count; it is **not a live approved shared synchronization feature**.

**Target:** consistent headers and cards using shared tokens, clear selected WO/context, prominent Open Photos, quieter yet visible Edit and Clear & Reuse, legible date input. Preserve all duplicate, edit/date, same-date Clear & Reuse, cancellation, unsafe-state blocking, identity and active-photo safeguards. Do not add a Finish Job button, lead selector, Two-mode control, or fabricated shared counts in this visual scope.

**Concurrency hold:** **do not edit `MainActivity`, `row_work_order.xml`, or Phase 14 contract/roadmap files in a new UI line while #102 owns overlapping changes**. Before Work Orders implementation, explicitly reconcile #102's authoritative state and then integrate against the correct authoritative head; a future merge of #102 is not presumed.

### Photos / upload list — same system, no invented actions

**Existing:** selected address/WO context, status banner, Open Camera, existing real local photo thumbnails, individual **48dp checkbox** for batch selection, Select All Ready, Clear selection, per-photo `...` state-safe actions, Reconcile All Uncertain when applicable, guided Next action, Discard Selected, and **Upload Selected (N)**. Photos can be WAITING/UPLOADING/FAILED/UNCERTAIN/UPLOADED etc. according to existing queue ownership.

**Target:** matching card/text/button styles, readable and truthful existing statuses, prominent camera and batch upload, usable row checkbox/touch areas, no extra selection boxes. Preserve 100–150-item list scalability and sticky upload affordance; never place controls over photo rows or hide retry/reconcile. Thumbnails are for **photos actually held locally**, not an invented property/remote gallery.

### Camera and supplemental screens

Camera remains the proven CameraX preview-first session; shutter, Done, flash, torch, zoom slider/pinch and orientation are **protected, not redesigned** by this pass. Check visual consistency at transition boundaries and document any future requested skinning separately.

Auth/sign-in, Manage Members, App Status/support, company switcher, address create/reactivate/archive/delete dialogs, Edit Work Order, Clear & Reuse date/confirmation, and photo-action menus should use consistent colors/spacing **only where existing owning controls can be safely styled**. No text/action/confirmation omissions, new roles, or authorization changes. If a dialog is controlled by Android system UI (Drive picker, date picker), respect its platform styling rather than attempting to imitate a screenshot unsafely.

## 4. End-to-end workflow coverage before code

| Affected flow | Disposition |
| --- | --- |
| Connect/select workspace or company, disconnect/unavailable, return and refresh | **Preserve**, improve display only |
| Search active/archived address; select, create, archive, reactivate, manual delete | **Preserve**, same action owners and confirmations |
| Select existing WO; add date/name; Edit; Clear & Reuse; cancel/date collision | **Preserve**, same exact folder identity and destructive gates |
| Photos select/deselect; camera/Done; prepare; upload; retry/reconcile; discard | **Preserve**, no queue/camera/Drive mutation logic changes |
| Offline/restart/loading/error states; protected originals and UNCERTAIN | **Preserve**, show truthfully, no new auto actions |
| Two-phone/One-Two photographer Phase 14 | **Excluded**, independent unfinished safety-gated scope |
| FWH admin/dashboard or any property photo library | **Excluded**, not existing FPP workflow |

Any uncovered button or missing design/control is a **stop/report** item, not permission to invent a replacement.

## 5. Implementation order: one coherent UI line, bounded review gates

1. **Baseline and visual lock (no code):** capture sanitized actual screen references in light/dark plus operator's real dark/font settings; enumerate every visible/conditional action per screen; produce a measured screenshot or device-rendered target for Home; obtain operator visual acceptance of the two-tone selector/Properties layout. Distinguish screenshot illustration from native views.
2. **Home-led styling batch:** work from the correct governed head in a separate authoritative **UI** branch/PR once safe; change only owning Android XML/drawable/color/style files and necessary existing view-presentation wiring, not business logic. Start with company card and Properties heading; preserve Home controls. Render on emulator and inspect screenshot parity before spreading reusable styles.
3. **Consistent other screens, no speculative rebuild:** propagate approved design tokens to Work Orders/Photos and appropriate supplemental screens, preserving the existing action inventory. First reconcile any file overlap with active Phase 14; do not silently rebase over #102 or overwrite its work. Limit camera to review unless separately approved.
4. **Verification and phone acceptance:** focused UI tests during development, one complete Android suite on **final runtime head**, render/screenshot comparisons in light/dark and increased font, and smallest applicable Samsung physical visual/tap smoke. Fix only observed discrepancies. Separate screenshot appearance acceptance from functional/Drive reality evidence.
5. **Closeout:** inspect every changed file/block against approved visual scope, reconcile requirement/evidence matrix, get any needed Level 3 approval **only if scope actually expands**, then merge under governance with an explicit rollback baseline. Do not call the redesign complete before required checks and the operator's on-device visual acceptance.

Do not split these into endless speculative micro-PRs. The first genuine stop is approval of the measured target; the next is Phase 14 overlap resolution before touching its owned files; the final stop is physical screenshot/user acceptance.

## 6. Test/acceptance matrix for runtime phase

| Requirement | What must be proven | State |
| --- | --- | --- |
| Company card matches approved two-tone reference | Same data, one existing selector, refresh/connect truthful; long-name and disconnected cases | Not started |
| Properties count + New Address | No overlap/wrapping at Samsung width and increased font; both readable and button usable | Not started |
| Home card/search/list/actions | Active-only ordering, Last used, search archived, ellipsis and navigation untouched | Not started |
| Work Orders usability | Select/create/Edit/date/Clear & Reuse accessible and exact existing confirmation/cancel actions preserved | Not started, #102 boundary |
| Photos usability | Photo checkbox, local thumbnails, row menu, multi-select, prepare/upload/reconcile/discard reachable and correct | Not started |
| Navigation / overlays | Bottom nav, Next guide, scrolling, system/keyboard insets, dialog visibility | Not started |
| Account/status/support | Existing Auth, Manage Members, App Status and dialogs are not hidden or behaviorally changed | Not started |
| Light/dark and scaling | Render normal and ~1.3 font scaling; portrait, no clipping/false status/color meaning | Not started |
| Protected behavior | No changed Drive/SAF identity, local queue, photo bytes, numbering, upload, retry, Clear & Reuse, deletion, camera or FWH scope | Not started |
| Final verification | Focused Android tests, full final-head CI, Samsung visual/tap acceptance, reviewed diff/rollback | Not started |

**Read surfaces:** existing app view state, photo/Drive status already rendered by the app.  
**Write surfaces:** Android rendering resources and narrowly necessary presentation code only. **No new persistence, server calls, permissions, provider writes, remote photo mutation, schemas, or deployment.**  
**Primary risk:** cosmetic restyling can clip controls, obscure disabled/error states, alter touch targets or mask a real action. **Mitigation:** protected control inventory, screenshots including large-font/offline/long-name states, focused UI interaction tests and Android real-device review.  
**Rollback:** revert the eventual isolated UI PR to the governed runtime baseline captured at implementation start; do not roll back Phase 14 or previously tested core behavior.  
**Privacy:** real customer address screenshots and photo content stay out of the public repository; use sanitized fixture data only.

## 7. Current handoff / limits of today's authorization

- This record documents a **planning decision** based on the user's approved direction (two-tone green, company selector first, Properties count next; carry consistent styling across real app pages).
- Actual home dark-mode screenshot and current XML were consulted; **no current Samsung screenshots for Work Orders, Photos, or the latest dialogs were provided in this design turn**. Exact pixel parity on those screens must therefore be measured later; no fabricated approval.
- No runtime work has been started by this plan. No Android APK, Drive state, backend, contract, permissions, photo content, deployment, or existing PR state changed.
- Next real checkpoint: **approve the sanitized measured Home visual target and existing-control inventory**, then stage the bounded presentation change. Respect unresolved Phase 14 overlap before implementing Work Orders.

## Governance sources

`AGENTS.md`; `GOVERNANCE.md`; `PROJECT_PROFILE.md`; `RULE_INDEX.md`; `CHANGE_CONTROL_CONTRACT.md`; `CONTRACT.md`; `TESTING_CONTRACT.md`; `REGRESSION_CHECKLIST.md`; `docs/PHASE_9_UI_MAKEOVER_RESET_PLAN_2026-09-12.md`; `docs/CONCEPT_3_NON_CAMERA_UI_IMPLEMENTATION_RECORD_2026-09-12.md`; `docs/PHASE_10E_GUIDED_NEXT_ACTION_2026-09-17.md`; `docs/PHASE_13_HOME_ADDRESS_LIFECYCLE_BUILD_STATE.md`; `docs/PHASE_14_TWO_PHONE_JOB_CYCLE_PLAN.md`; open PR #102 authoritative Phase 14 build state.
