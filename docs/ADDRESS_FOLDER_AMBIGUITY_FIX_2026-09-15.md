# Address Folder Ambiguity Fix — 2026-09-15

## Change class

**Level 3 — high risk.**

This change affects address-folder discovery/create decisions and therefore can affect which stable Drive folder identity becomes the parent of future work orders and photos. It must remain isolated, use focused tests plus one final complete automated verification, complete the affected safe SAF/Google Drive reality gate, and receive explicit operator approval before merge.

## Field defect

During the 2026-09-15 field visit for 509 South Boundary, Walters, photos were split across two separate stable Drive address-folder identities under the approved `HNP Jobs` master:

- `509_W_SOUTH_BOUNDARY_WALTERS_OK` — provider/Drive identity `1YFAx3V-4Ce1WXbK_zD1SQzaQ-j2lVgcJ`
- `509 SOUTH BOUNDARY WALTERS OK` — provider/Drive identity `16h_YZITReuc6f896RP4PY0bk89nzWMIx`

The FPP dated work order `Grass cut - 2026-09-15` exists beneath the first identity and contains 23 FPP-created `field-photo-*.jpg` files. A separate `GRASS CUT` folder beneath the second identity contains 43 Samsung-camera photos from approximately 11:00:21 through 11:42:05 local time. At least 66 visit photos are therefore accounted for, but the property exists twice in the master tree.

This record does **not** claim that FPP created either duplicate address folder. The older underscore-named folder predates this field visit. The exact creator of the later spaced-name folder has not been proven from the available evidence.

## Root cause in current app behavior

The current address create preflight performs only literal display-name equality. `AddressFolderName.build()` preserves operator formatting and `DriveClient.findExactNameMatches()` compares `folder.name().equals(requestedName)`. Therefore formatting variants are treated as unrelated candidates for create purposes.

The home property list also prettifies provider names for display by replacing underscores with spaces. It shows a disambiguating ID only when two folders produce exactly the same cleaned display string. Near-equivalent address-folder names can therefore appear as ordinary separate properties even though they may refer to one physical property.

## Approved scope

Narrowly harden address-folder ambiguity handling without changing existing remote identities or Drive content:

1. Keep exact provider document IDs authoritative.
2. Keep exact-name matching as the only automatic reuse path, with exact unique matches retaining precedence exactly as the existing contract requires.
3. Add a conservative **possible-same-property** comparison used only as a safety guard when there is no exact provider-name match and for visible ambiguity labeling.
4. A possible match may block a new address-folder creation and require operator choice, but it must never auto-select, merge, rename, move, delete, or rewrite a provider identity.
5. Surface ambiguous address rows with their raw Drive folder names and short provider-ID context so the operator can deliberately choose the intended stable identity.
6. Preserve unrelated camera, preparation, queue, work-order, upload, retry, cleanup, Drive permission, and master-tree behavior.

## Possible-same-property comparison

The guard is intentionally conservative and non-authoritative. It may identify candidates that require human choice; it never proves identity.

For comparison only:

- compare case-insensitively;
- treat underscores, hyphens, punctuation, and repeated whitespace as presentation separators;
- when an address begins with a house-number token, allow one standalone compass-direction token immediately after that number (`N`, `S`, `E`, `W`, `NE`, `NW`, `SE`, `SW`, or full-word equivalent) to be omitted for ambiguity detection.

No street/city parsing, geocoding, abbreviation expansion, fuzzy edit distance, or automatic folder substitution is authorized.

Example: `509_W_SOUTH_BOUNDARY_WALTERS_OK` and `509 SOUTH BOUNDARY WALTERS OK` may be flagged as possible aliases because the only material comparison difference after separator normalization is the first post-number `W`. The app must still require operator choice when a new folder would otherwise be created because software cannot safely prove those two provider identities represent the same property.

If the operator requests a name that already has one exact provider-name match, that exact match retains precedence and is reused through the existing approved path. A fuzzy candidate never overrides an exact provider-name match.

## Read surfaces

- provider-returned address folder names and stable document IDs under the already approved master tree;
- operator-entered proposed address folder name;
- existing locally persisted current address identity only through existing flows.

## Write surfaces

- no new persisted schema;
- no existing Drive folder modification;
- no Drive photo modification;
- address folder create remains the only remote write in this path, and the new guard can only prevent that write when no exact match exists and ambiguity is detected.

## Protected behavior

- existing provider IDs remain authoritative;
- no automatic migration between the two live 509 South Boundary identities;
- no move, rename, delete, permission change, or merge of live Drive folders;
- queued/captured photos keep their immutable stored work-order destination;
- an exact unique address name continues to reuse its exact existing identity, even when a separate fuzzy candidate exists;
- unique non-ambiguous new names may still create exactly one folder under the selected master;
- provider freshness/fail-closed behavior remains unchanged;
- no changes to photo capture, compression, upload, reconciliation, or cleanup.

## Focused automated coverage

Add tests proving at least:

1. case/separator-only numbered-address variants are possible aliases but not automatic exact matches;
2. the observed `509_W_SOUTH_BOUNDARY_WALTERS_OK` vs `509 SOUTH BOUNDARY WALTERS OK` pair is treated as ambiguous;
3. clearly different house numbers remain distinct;
4. opposite explicit directions (`509 E ...` vs `509 W ...`) are not automatically equated as one property when both directions are present;
5. exact provider-name matching retains precedence over a fuzzy candidate;
6. a no-exact-match create request with one or more possible aliases is blocked for operator choice;
7. property-row ambiguity labeling does not mutate folder IDs or names;
8. non-address labels are not fuzzy matched merely because punctuation differs.

## Physical-device observation — first Walters refresh

On the operator's Samsung phone, the first CI-tested ambiguity build successfully detected both live 509 South Boundary folders and visibly labeled both rows `Possible duplicate`. This proves the conservative matching logic reached the real Google Drive-backed property list without changing Drive data.

The same screenshot exposed a presentation defect: `property_disambiguator` was limited to one line, so the text clipped after `Possible duplicate · Drive:` and hid the raw provider folder name and short ID that the operator needs to distinguish stable identities.

Follow-up patch on the same isolated branch:

- split the ambiguity detail into separate `Possible duplicate`, `Drive: <raw folder name>`, and `ID …<short id>` lines;
- allow the disambiguator view to grow to four lines;
- make no change to matching, provider IDs, Drive writes, photo behavior, or selection behavior.

The first device observation is therefore **logic PASS / presentation FAIL**, requiring one updated APK check before the row-presentation requirement can pass.

## Safe Drive reality gate

Use only a disposable address-folder fixture under the approved FPP test master, never the live 509 South Boundary folders.

1. Create or reuse safe test folders whose names reproduce separator/directional ambiguity.
2. Refresh through the real Android/Google Drive DocumentsProvider path.
3. Attempt a new address create using an ambiguous variant for which no exact name exists.
4. Confirm FPP performs **no create** and instead requires operator choice.
5. Select one existing candidate and confirm its exact provider identity is retained for subsequent work-order discovery.
6. Confirm an unrelated unique test address can still be created once under the correct master.
7. Confirm an exact unique provider-name request still reuses that exact identity.
8. Confirm unrelated test content is unchanged.

## Rollback

Baseline: `6c73909a0c00948643379b4fce9456c0b1d24ece` (`main` before this isolated fix).

Rollback is a narrow revert of this ambiguity guard and UI labeling. It must not delete or alter any queued photo, existing Drive folder, or provider identity.

## Merge status

**Not approved for merge.** Level 3 requires the final code/tests, safe real-provider gate, and explicit operator pre-merge approval.
