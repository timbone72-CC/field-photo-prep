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

Current `MainActivity.refreshWorkOrderFolders()` performs one ordinary `DriveClient.listFolders(...)` provider query for the selected address. The integration contract explicitly recognizes that cloud-backed Android DocumentsProvider state can be cached/loading and that one child query is not authoritative proof of current hierarchy.

The current state reconciliation also permits a previously saved work-order identity to remain selected if that ID appears in whatever folder list the provider returned. In the observed stale-root result, the selected address folder itself appeared in the work-order list, allowing an impossible self-child relationship to survive reconciliation and potentially re-enable Photos.

This record does not claim the Google Drive provider always returns root children for a child query. The physical-device evidence proves only that the current implementation accepted a provider result that was not the selected address's real work-order hierarchy.

## Approved scope

Narrowly harden read-only work-order discovery and capture authorization:

1. Work-order discovery for an opened property must use the existing settled/fresh provider-folder verification path instead of a single ordinary child query.
2. A returned folder list that contains the selected address's own provider document ID is invalid for work-order browsing and must fail closed.
3. A saved work-order identity may become active only after it is found in the verified child list for the exact selected address.
4. Until verified child discovery succeeds, the app must not treat any saved work order as capture-authorizing state and Photos must remain disabled.
5. If work-order discovery fails or is stale/uncertain, preserve Drive data and photo data, show the error, and require refresh/reselection. Do not substitute master/root children, create folders, move folders, rename folders, delete anything, or guess by name.
6. Preserve all address ambiguity behavior, work-order create/reuse semantics, camera behavior, queue state, upload behavior, reconciliation, and cleanup outside this identity gate.

## Read/write surfaces

Read surfaces:

- persisted master tree URI;
- selected address provider document ID;
- provider work-order child-folder metadata under that selected address;
- locally saved current work-order identity for reconciliation only after verified child discovery.

Write surfaces:

- no new Drive write;
- no folder create/rename/delete/move;
- no photo upload/delete;
- local current-work-order selection may be cleared when it cannot be verified under the exact selected address.

## Protected behavior

- provider document ID remains authoritative identity;
- display names never authorize parent/child substitution;
- the selected address can never be accepted as its own work order;
- a stale or uncertain provider read cannot enable Photos;
- previously captured/queued photos retain their immutable stored destination and are not rewritten by this UI repair;
- address-folder ambiguity PR behavior remains unchanged.

## Focused regression coverage

Add coverage proving at least:

1. a verified child list containing the selected address ID is rejected as an impossible self-child result;
2. a saved work order is restored only when its ID exists in the verified child list;
3. a missing saved work order is cleared;
4. failed/unverified work-order discovery leaves no capture-authorizing selected work order;
5. ordinary valid child lists still restore/select real work orders normally;
6. no work-order discovery path mutates Drive data.

## Safe Android/Google Drive reality gate

Use the existing disposable property `1607_CRESTVIEW_DR_CORDELL_PRESSURE_TEST`, never the live Walters folders.

1. Install the tested APK over the existing app without uninstalling.
2. Refresh Properties and open the existing 1607 Crestview test property.
3. Confirm the Work Orders screen shows the real children of that exact property: `FULL PROPERTY CONDITION INSP`, `GRASS CUT`, and `POOL CARE`.
4. Confirm root-level property addresses do not appear as work orders.
5. Confirm the property itself is not shown or persisted as its own work order.
6. Select one real work-order child and confirm Photos becomes available only after that selection.
7. Return home and reopen the same property; confirm the stored real work-order selection is retained only if it is still present under the verified address child list.
8. Confirm Drive shows no new folders and unrelated test content is unchanged.

## Dependency / merge status

This branch is stacked on `fix/address-folder-ambiguity-20260915` only so the physical phone can continue testing one cumulative APK. This defect remains a separate Level-3 change and must not be silently folded into the address-ambiguity PR.

**Not approved for merge.**