from pathlib import Path


def replace_once(path, old, new):
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one match in {path}, found {count}")
    p.write_text(text.replace(old, new, 1))


main_path = "app/src/main/java/com/inandout/fieldphotoprep/MainActivity.java"
replace_once(
    main_path,
    '''        final String addressId = selectedAddress.id();
        setBusy("Checking for " + requestedName + "…");
''',
    '''        if (selectedWorkOrder != null
                && WorkOrderFolderName.shouldRouteSelectedFolderToReuse(
                        selectedWorkOrder.name(),
                        workOrderInput.getText().toString(),
                        selectedDate.toString())) {
            prepareClearAndReuse();
            return;
        }

        final String addressId = selectedAddress.id();
        setBusy("Checking for " + requestedName + "…");
''')

replace_once(
    main_path,
    '''                if (snapshot.count() == 0) {
                    runOnUiThread(() -> showMessage(
                            "Selected folder is empty. Use Reuse Selected Empty Folder; nothing was deleted."));
                    return;
                }
''',
    '''                if (snapshot.count() == 0) {
                    runOnUiThread(this::reuseSelectedEmptyFolder);
                    return;
                }
''')

won = Path("app/src/main/java/com/inandout/fieldphotoprep/WorkOrderFolderName.java")
text = won.read_text()
marker = '''    private static String normalizeWorkOrder(String workOrderName) {
'''
if text.count(marker) != 1:
    raise SystemExit("WorkOrderFolderName insertion marker mismatch")
helper = '''    public static boolean shouldRouteSelectedFolderToReuse(
            String selectedFolderName,
            String requestedWorkOrderName,
            String requestedLocalDate) {
        String requestedName = build(requestedWorkOrderName, requestedLocalDate);
        return selectedFolderName != null
                && !selectedFolderName.equals(requestedName)
                && isOlderSameWorkOrderFolder(
                        selectedFolderName,
                        requestedWorkOrderName,
                        requestedLocalDate);
    }

'''
won.write_text(text.replace(marker, helper + marker, 1))

test = Path("app/src/test/java/com/inandout/fieldphotoprep/WorkOrderFolderNameTest.java")
text = test.read_text()
insert = '''
    @Test
    public void addWorkOrderRoutesSelectedOlderSameOccurrenceToReuse() {
        assertTrue(WorkOrderFolderName.shouldRouteSelectedFolderToReuse(
                "TREE TRIM 3 - 2026-09-17", "TREE TRIM 3", "2026-09-18"));
    }

    @Test
    public void addWorkOrderDoesNotRouteCurrentOrDifferentWorkOrderToReuse() {
        assertFalse(WorkOrderFolderName.shouldRouteSelectedFolderToReuse(
                "TREE TRIM 3 - 2026-09-18", "TREE TRIM 3", "2026-09-18"));
        assertFalse(WorkOrderFolderName.shouldRouteSelectedFolderToReuse(
                "TREE TRIM 2 - 2026-09-17", "TREE TRIM 3", "2026-09-18"));
    }
'''
idx = text.rfind('\n}')
if idx < 0:
    raise SystemExit("WorkOrderFolderNameTest closing brace not found")
test.write_text(text[:idx] + insert + text[idx:])

replace_once(
    "app/build.gradle",
    '''        versionCode 23
        versionName '0.18-reused-work-order-sequence-reset'
''',
    '''        versionCode 24
        versionName '0.19-reuse-routing-fix'
''')

contract = Path("CONTRACT.md")
text = contract.read_text()
marker = '''30. Address folders are not eligible for automatic or **Clear & Reuse** recycling in the initial model.
'''
if text.count(marker) != 1:
    raise SystemExit("CONTRACT reuse marker mismatch")
replacement = marker + '''31. When **Add Work Order** is invoked while an older occurrence of that same work-order name is selected, the app must route the request through the approved reuse workflow instead of creating a parallel new dated folder. An empty selected folder may be renamed/reused after verification; a non-empty selected folder must still require the explicit **Clear & Reuse** confirmation before any child deletion.
'''
contract.write_text(text.replace(marker, replacement, 1))

impact = Path("docs/2026-09-17_REUSED_WORK_ORDER_CAPTURE_SEQUENCE_RESET.md")
text = impact.read_text()
note = '''

## Physical gate attempt 1 — failed safely

The first disposable phone + Drive gate exposed a workflow-routing defect before merge. The operator selected an older work order intending to Clear & Reuse, but **Add Work Order** created the requested new dated folder first. The later Clear & Reuse preflight then correctly saw that the requested dated folder already existed and left the old folder and its photos unchanged.

Observed disposable evidence:

- old `TREE TRIM 3 - 2026-09-17` folder remained with its photos;
- a separate empty `TREE TRIM 3 - 2026-09-18` folder was created with a different provider ID;
- no live customer folder was used;
- no old photos were deleted by the failed path.

Corrective behavior added after this failed gate:

- if **Add Work Order** is pressed while an older same-work-order occurrence is selected, it routes into the approved reuse path instead of creating a second folder;
- an empty selected old folder is routed through verified empty-folder reuse;
- a non-empty selected old folder still reaches the explicit Clear & Reuse confirmation before deletion;
- ordinary Add Work Order behavior is unchanged when no eligible older same-work-order occurrence is selected.

The physical gate must be repeated on the corrected APK before merge approval can be requested.
'''
if "## Physical gate attempt 1 — failed safely" not in text:
    impact.write_text(text + note)

Path(".github/workflows/stage-reuse-routing-fix.yml").unlink()
Path(".github/scripts/stage_reuse_routing_fix.py").unlink()
