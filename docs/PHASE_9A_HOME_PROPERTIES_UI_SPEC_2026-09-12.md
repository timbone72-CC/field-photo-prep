# Phase 9A — Home / Properties UI Specification

Date: 2026-09-12

Status: **APPROVED FOR IMPLEMENTATION — OPERATOR APPROVED 2026-09-12**

Parent plan: `docs/PHASE_9_UI_MAKEOVER_RESET_PLAN_2026-09-12.md`

Runtime baseline when implementation begins: current governed `main`.

## 1. Goal

Make the first screen feel like a finished Android field app instead of a development utility while preserving the existing Drive/address behavior exactly.

The screen's job is simple:

1. show whether the approved Drive master is available;
2. let the operator refresh properties;
3. let the operator open an existing property quickly;
4. let the operator add a new address when needed.

Everything else is secondary.

## 2. Explicit failure baseline — do not reproduce this structure

The physical screenshot supplied on 2026-09-12 is the visual rejection baseline.

The replacement Home screen must **not** reproduce these characteristics:

- oversized `Field Photo Prep` heading sitting close to/under the Android status bar;
- separate large `Choose a property` heading beneath it;
- standalone large `19 address folders found` status text;
- giant Google Drive card occupying a major fraction of the screen;
- `Connect Drive` shown as a primary-looking action while Drive is already connected;
- oversized full-width `+ New Address` bar;
- large `Addresses` heading followed by very tall cards;
- repeated `Tap to view work orders` copy on every row;
- raw underscore-heavy Drive folder names as the main visual treatment;
- only three to four property rows visible on a normal phone screen because chrome consumes the rest.

A new build that merely changes colors, corner radii, or typography on that hierarchy is a FAIL.

## 3. Approved Home screen structure — top to bottom

### A. System bars and app bar

Use proper Android system-bar insets. App content must begin below the status bar and remain above the navigation/gesture area.

Use a compact Material app bar:

- title: **Field Photo Prep**;
- one line only;
- normal app-title scale (Material title style, approximately 20sp before user font scaling);
- no giant subtitle block;
- no content behind the clock/status icons;
- optional overflow menu only for secondary settings/actions that already exist.

The title must remain readable at the operator's actual Samsung font/display settings without dominating the screen.

### B. Drive status strip

Immediately below the app bar, show a **compact status row**, not a large card.

Connected state:

- small Drive/status icon or green connected indicator;
- primary text: **HNP Jobs** (or the actual selected master-folder display name);
- small secondary text: **Drive connected**;
- trailing refresh icon button;
- secondary `Change Drive` action lives in a small overflow/menu or quiet text action, not beside Refresh as a second giant button.

Disconnected/expired state:

- compact warning/neutral status row;
- text such as **Google Drive not connected** or **Drive access expired**;
- one clear **Connect Drive** / **Reconnect** action.

Do not show a `Connect Drive` primary button when the app is already connected.

Target connected-strip height: roughly 52–64dp, expanding only when accessibility text genuinely requires it.

### C. Properties section heading

Use one compact section row:

- left: **Properties**;
- right or beside title: small count, e.g. **19** or **19 properties**;
- no separate giant `19 address folders found` paragraph.

Normal successful refresh count belongs here or in a small transient message, not as major screen chrome.

### D. Property list

The property list is the dominant part of the screen.

Use a dedicated list adapter/row layout rather than large dynamically decorated development controls.

Each property row:

- compact Material row/card, target normal height approximately 64–76dp;
- 48dp minimum touch target;
- primary text is the property/address display name;
- primary text may wrap to two lines if required, then ellipsize rather than creating a giant card;
- trailing chevron/arrow indicates that tapping opens the property;
- do **not** repeat `Tap to view work orders` on every row;
- no provider/document ID in the normal row unless duplicate-name disambiguation genuinely requires it;
- rows use subtle surface separation, not oversized high-contrast cards.

### E. Human-readable address display

Drive folder identity and stored folder name are immutable data for this makeover.

The Home list may apply a **display-only** normalization:

- replace underscores with spaces;
- collapse repeated spaces;
- trim display whitespace;
- preserve the underlying folder name and provider ID unchanged;
- do not rename Drive folders;
- do not invent address components or silently rewrite street/city/state semantics.

Example display only:

`101_CHUCKER_LN_ELK_CITY_OK` → `101 CHUCKER LN ELK CITY OK`

A later separately approved feature may provide smarter address formatting. Phase 9A does not guess commas, ZIP codes, directional corrections, or canonical addresses.

### F. New Address action

Use a compact Material **extended floating action button** or equivalent bottom-end primary action:

**+ New Address**

Requirements:

- remains easy to reach one-handed;
- does not consume a full-width 100dp-style block at the top of the list;
- respects bottom navigation/gesture insets;
- invokes the existing safe address-entry/create workflow;
- does not change duplicate/create/reuse semantics.

If the implementation platform makes an extended FAB materially awkward with accessibility scaling, a compact 48–52dp primary button aligned to the section header is acceptable. A giant full-width bar is not.

## 4. Feedback states

### Loading / refresh

Refreshing the list must not replace the screen with technical text.

Preferred presentation:

- small progress indicator in/near the Drive status row or Properties section;
- keep existing list visible when safe;
- disable conflicting actions only as required by existing behavior.

### Success

Routine success such as `19 address folders found` should be represented by the property count and, if useful, a short transient Snackbar. It is not a permanent large text block.

### Error / expired permission

Persistent errors get a compact inline banner/status row with an actionable message. Transient errors may use Snackbar/dialog presentation where appropriate.

Existing fail-closed Drive rules remain authoritative.

### Empty state

If Drive is connected and contains no address folders:

- center a small friendly empty-state message: **No properties yet**;
- keep **+ New Address** available;
- do not show an empty giant list/card shell.

## 5. Navigation behavior

Tapping a property:

- uses that exact `DriveFolder` provider identity;
- persists/selects the same address identity as current main;
- transitions into the existing Work Orders flow;
- does not create, rename, or refresh a different property implicitly.

Phase 9A may leave the Work Orders screen visually old while 9A is being tested. That is intentional. Work Orders is replaced only after Home/Properties passes and is locked.

Android Back behavior remains normal.

## 6. Accessibility and density

- all interactive touch targets at least 48dp;
- use Material text appearances/sp rather than pixel-sized text;
- support the operator's actual Samsung font scaling;
- do not solve accessibility by making every card/button oversized;
- allow rows to grow when large text genuinely needs more space;
- icons have content descriptions where needed;
- connected/error state must not depend on color alone;
- light and dark themes must maintain readable contrast.

Density target on the operator's phone:

- after app bar + compact Drive status + Properties heading, the list should normally show **at least about five complete property rows** at once when names fit on one or two lines;
- accessibility text scaling may legitimately reduce that count, but the screen must remain materially denser than the rejected screenshot.

## 7. Technical implementation boundary

Phase 9A is a Level 2 UI/navigation change.

Preferred owning surfaces:

- purpose-built Home/Properties XML layout;
- dedicated property row XML;
- dedicated list adapter (RecyclerView preferred);
- Material theme/resources;
- narrow MainActivity wiring changes needed to bind existing Drive/address actions to the new views.

Do **not** use `Application.ActivityLifecycleCallbacks` or a post-creation decorator to transform the old screen into this one.

Do **not** move Drive write/list/create logic into the adapter or view classes.

Do **not** alter Camera, photo queue, upload, reconciliation, or cleanup code.

## 8. Phase 9A physical acceptance gate

The staged APK passes only if all of the following are true on the operator's Samsung phone:

1. `Field Photo Prep` sits cleanly below the Android status bar.
2. The Home screen is **immediately and unmistakably structurally different** from the rejected screenshot.
3. Connected Drive status is compact; no giant green Drive panel.
4. No `Connect Drive` primary action is shown while already connected.
5. `+ New Address` is compact and does not dominate the screen.
6. Property names are readable without underscore-heavy presentation.
7. Property rows are materially shorter; the property list is clearly the dominant screen content.
8. Repeated `Tap to view work orders` helper text is gone from every row.
9. Refresh remains available.
10. Tapping one existing property opens its existing Work Orders flow successfully.
11. Camera behavior is not retested because Phase 9A does not touch the camera.
12. No Drive create/delete/rename test is required solely for this visual gate.

If any of items 1–8 fail, the UI direction is considered failed even if navigation technically works.

## 9. Lock rule after PASS

Once the operator reports Phase 9A PASS:

- record the device observation once;
- merge the governed 9A change after required automated evidence;
- treat Home/Properties layout as locked;
- begin Phase 9B from that new main baseline;
- do not redesign 9A during 9B unless a concrete field defect is discovered.
