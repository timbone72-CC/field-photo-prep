# Work-Order Child Discovery Fix — 2026-09-15

## Change class

**Level 3 — high risk.**

This change affects work-order discovery and the identity gate that enables photo capture. It is isolated from the address-folder ambiguity fix. It must remain fail-closed, use focused regression coverage plus full Android verification, complete the safe Google Drive reality gate, and receive explicit operator approval before merge.

## Physical-device defect

During the safe 1607 Crestview test on the operator's Samsung phone, the address ambiguity guard correctly blocked creation of a spaced-name duplicate of the existing property folder `1607_CRESTVIEW_DR_CORDELL_PRESSURE_TEST`.

When the operator then selected that existing property, the Work Orders screen was wrong:

- the address header showed `1607 CRESTVIEW DR CORDELL`;
- the work-order list showed root-level property addresses such as `1611 NW SMITH ...`, `2202 NW CHEYENN...`, and `224 S CHURCH ST ...` instead of the selected property's actual child work-order folders;
- the screen also showed `1607 CRESTVIEW DR CORDELL PRESSURE TEST` as the selected work order, which is the property folder itself and therefore cannot be a valid direct child work order of that same property.

Drive-side inspection confirms the selected test property has the expected child work-order folders `FULL PROPERTY CONDITION INSP`, `GRASS CUT`, and `POOL CARE`. The visible Android result therefore did not represent the selected property's actual child hierarchy.

## Root cause boundary

Two implementation weaknesses combined:

1. `MainActivity` uses one shared `visibleFolders` list for both the Properties and Work Orders screens. `openAddress()` previously switched screens without clearing the root property rows first, so the Work Orders adapter could temporarily retain root-level properties while child discovery was in progress or after a discovery error.
2. `DriveClient.listFolders(...)` accepted a single ordinary child query. The integration contract explicitly recognizes that cloud-backed Android DocumentsProvider state can be cached/loading and that one child query is not automatically authoritative. In the physical-device result, the selected property itself appeared among the purported work-order children, which is an impossible self-child relationship and strong evidence that the returned hierarchy was stale/wrong for the requested parent.

The prior local preference model also stored a work-order ID/name without the exact parent address ID. A previously bad selection could therefore survive as local state when reopening the same property.

This record does not claim the Google Drive provider always returns root children for a child query. The physical-device evidence proves only that the current implementation exposed/accepted state that was not the selected address's real child hierarchy.

## Approved scope

Narrowly harden read-only work-order discovery and capture authorization:

1. Clear the shared property-row list immediately when opening a property so root-level addresses are never intentionally presented as work orders while child discovery is pending.
2. For ordinary folder browsing, if the provider reports `EXTRA_LOADING` or returns a child list containing the requested parent document ID, treat that snapshot as stale/impossible and escalate to the existing settled/fresh provider verification path.
3. Settled/fresh folder verification must also reject any snapshot that contains the requested parent as its own child.
4. Bind each newly selected work-order identity to the exact current address provider document ID in local preferences.
5. Legacy saved work-order state that has no recorded parent-address binding is treated as unverified and is not restored automatically. A work-order ID equal to the selected address ID is always invalid.
6. A valid parent-bound saved work order may remain usable for offline photo capture if provider browsing is temporarily unavailable. This preserves the existing contract that camera capture does not require an active internet connection. Provider failure must not convert an unbound/invalid legacy selection into capture-authorizing state.
7. If work-order child discovery fails, block new work-order Drive writes until refresh succeeds. Do not substitute master/root children, create folders from an uncertain absence result, move folders, rename folders, delete anything, or guess by name.
8. Preserve all address ambiguity behavior, work-order create/reuse semantics, camera behavior, queue state, upload behavior, reconciliation, and cleanup outside this identity gate.

## Read/write surfaces

Read surfaces:

- persisted master tree URI;
- selected address provider document ID;
- provider work-order child-folder metadata under that selected address;
- locally saved current work-order identity plus its exact parent-address binding.

Write surfaces:

- no new Drive write;
- no folder create/rename/delete/move caused by this repair;
- no photo upload/delete caused by this repair;
- local preferences add one parent-address binding for current-work-order identity;
- legacy/unbound or impossible self-child current-work-order state may be ignored/cleared during normal reselection.

## Protected behavior

- provider document ID remains authoritative identity;
- display names never authorize parent/child substitution;
- the selected address can never be accepted as its own work order;
- root property rows are cleared before the Work Orders screen is allowed to present child results;
- a stale/impossible provider read cannot be used as evidence for a new work-order Drive write;
- a correctly parent-bound previously selected work order can still support offline camera use;
- previously captured/queued photos retain their immutable stored destination and are not rewritten by this UI repair;
- address-folder ambiguity PR behavior remains unchanged.

## Focused regression coverage

Coverage must prove at least:

1. a folder list containing the requested parent ID is recognized as an impossible self-child result;
2. legacy saved work-order state without a parent-address binding is not restored;
3. a correctly selected work order is stored with the exact current address binding;
4. changing addresses clears the prior current-work-order binding;
5. a property cannot be stored/restored as its own work order;
6. opening a property clears previously visible root property rows before work-order discovery;
7. without a valid restored/selected work order, Photos remains disabled;
8. ordinary valid child lists still restore/select real work orders normally;
9. no work-order discovery path mutates Drive data.

## Safe Android/Google Drive reality gate

Use the existing disposable property `1607_CRESTVIEW_DR_CORDELL_PRESSURE_TEST`, never the live Walters folders.

1. Install the tested APK over the existing app without uninstalling.
2. Refresh Properties and open the existing 1607 Crestview test property.
3. Confirm root-level property addresses do not flash/persist as work orders while the property opens.
4. Confirm the Work Orders screen shows the real children of that exact property: `FULL PROPERTY CONDITION INSP`, `GRASS CUT`, and `POOL CARE`.
5. Confirm the property itself is not shown or persisted as its own work order.
6. Confirm the prior legacy selected-work-order banner is gone until a real child is selected.
7. Select one real work-order child and confirm Photos becomes available only after that selection.
8. Return home and reopen the same property; confirm the stored real work-order selection is retained with the same exact address binding.
9. Confirm Drive shows no new folders and unrelated test content is unchanged.

## Dependency / merge status

This branch is stacked on `fix/address-folder-ambiguity-20260915` only so the physical phone can continue testing one cumulative APK. This defect remains a separate Level-3 change and must not be silently folded into the address-ambiguity PR.

**Not approved for merge.**