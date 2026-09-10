# Phase 3B + Phase 4 Integration Device Record

Date: 2026-09-10

## Scope

This record closes the smallest real-device smoke required for the controlled integration of already phone-validated Phase 3B Clear & Reuse and Phase 4 Address Folder Creation behavior onto the merged Phase 7B runtime.

Runtime branch: `integrate/phase-3b-4-onto-phase7-main`

Exact tested/installable runtime head: `b4de6f1ab75ff19f558d1c984ba3d4780a66454c`

Version: `versionCode 16`

Automated gate: GitHub Actions run `34525176175` — PASS

Artifact: `10171378783`

Artifact digest: `sha256:5ecad0380d7da24f5d0008dfdc3c934a2d466d0a1f8738e4e378f0b80c6c8556`

Device: Samsung Galaxy A16

Safe hierarchy: `HNP Jobs → FIELD PHOTO PREP TEST → Cut Grass - 2026-09-21`

## Device observations

1. The version-16 integration APK installed over the existing stable-signed Phase 7B app without uninstalling or clearing app data.
2. Persisted master-folder access remained usable after the update.
3. The existing safe address `FIELD PHOTO PREP TEST` was reused through the current app flow; the operator reached its existing `Cut Grass - 2026-09-21` work order without creating a replacement destination.
4. The integrated large-display-safe work-order-selection flow successfully selected the existing work order.
5. With work-order name `Cut Grass` and requested date `2026-09-22`, **Clear & Reuse Selected Folder** reached the destructive confirmation screen without performing deletion.
6. The confirmation displayed the exact expected hierarchy:
   - Master: `HNP Jobs`
   - Address: `FIELD PHOTO PREP TEST`
   - Old folder: `Cut Grass - 2026-09-21`
   - Direct items to remove: `3`
   - New folder: `Cut Grass - 2026-09-22`
7. The operator used the non-destructive cancel path. Independent Google Drive inspection after the observation showed the old folder still contains exactly three JPEGs, so nothing was removed or renamed.
8. The existing Phase 7B local metadata also survived the update. The photo screen still showed both previously confirmed records as `UPLOADED · ATTEMPT 1`.

## Independent Drive verification

Backend Drive folder `FIELD PHOTO PREP TEST` remains a single existing folder with ID `1tFJRrJ3tYEFQ2JtBo5tyR51yelBgXzEu` in the inspected safe hierarchy.

Its `Cut Grass - 2026-09-21` work-order folder is backend Drive ID `1nU46afPB_LR6C0_T4-SKIZyAFK3YpcRL`.

After the cancel-path observation, that folder still contained exactly these three JPEGs:

- `field-photo-245bc9a8-6a15-48e1-8c46-ab36ef8a95f7.jpg`
- `field-photo-973f23ec-2f16-418c-b8a4-c703951e3346.jpg`
- `field-photo-88ba77d6-2af9-4a3d-ad07-0bdefe30232c.jpg`

No destructive Phase 3B action was repeated because the original Phase 3B phone gate had already proven successful Clear & Reuse on the real provider. This integration smoke intentionally proves only that the port still reaches the correct identity-bound confirmation and that cancel remains non-destructive while the merged Phase 7B state survives.

## Result

**PASS**

The controlled port preserved the previously validated Phase 3B and Phase 4 behavior on the current Phase 7B runtime without invalidating the Phase 7B photo state. The integration satisfies the required final automated gate plus the smallest proportional real-device/provider smoke.

The operator previously gave explicit Level-3 merge authorization: `You may merge anything that needs merged.`
