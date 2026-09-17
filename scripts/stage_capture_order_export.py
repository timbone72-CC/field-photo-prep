from pathlib import Path

activity = Path('app/src/main/java/com/inandout/fieldphotoprep/PhotoCaptureActivity.java')
text = activity.read_text()

old_import = 'import android.content.Intent;\n'
new_import = (
    'import android.content.ClipData;\n'
    'import android.content.ClipboardManager;\n'
    'import android.content.Intent;\n'
)
if old_import not in text:
    raise SystemExit('Intent import anchor not found')
text = text.replace(old_import, new_import, 1)

old = '''    private void updateReconcileAllUi(PendingPhotoStore.ScanResult scan) {
        if (reconcileAllButton == null || scan == null) {
            return;
        }
        boolean running = BATCH_RECONCILE_GATE_ID.equals(UPLOAD_GATE.activePhotoId());
        int uncertainCount = scan.uncertainPhotoIds().size();
        if (running) {
            reconcileAllButton.setVisibility(View.VISIBLE);
            reconcileAllButton.setEnabled(false);
            if (activeReconcilePosition > 0 && activeReconcileTotal > 0) {
                reconcileAllButton.setText("Reconciling " + activeReconcilePosition
                        + " of " + activeReconcileTotal + "…");
            } else {
                reconcileAllButton.setText("Starting Reconciliation…");
            }
            return;
        }
        reconcileAllButton.setText("Reconcile All Uncertain (" + uncertainCount + ")");
        reconcileAllButton.setVisibility(uncertainCount > 0 ? View.VISIBLE : View.GONE);
        reconcileAllButton.setEnabled(uncertainCount > 0
                && !PREPARATION_GATE.isBusy()
                && !UPLOAD_GATE.isBusy());
    }
'''

new = '''    private void updateReconcileAllUi(PendingPhotoStore.ScanResult scan) {
        if (reconcileAllButton == null || scan == null) {
            return;
        }
        boolean running = BATCH_RECONCILE_GATE_ID.equals(UPLOAD_GATE.activePhotoId());
        int uncertainCount = scan.uncertainPhotoIds().size();
        if (running) {
            reconcileAllButton.setVisibility(View.VISIBLE);
            reconcileAllButton.setEnabled(false);
            if (activeReconcilePosition > 0 && activeReconcileTotal > 0) {
                reconcileAllButton.setText("Reconciling " + activeReconcilePosition
                        + " of " + activeReconcileTotal + "…");
            } else {
                reconcileAllButton.setText("Starting Reconciliation…");
            }
            return;
        }

        if (uncertainCount > 0) {
            reconcileAllButton.setOnClickListener(v -> reconcileAllUncertain());
            reconcileAllButton.setText("Reconcile All Uncertain (" + uncertainCount + ")");
            reconcileAllButton.setVisibility(View.VISIBLE);
            reconcileAllButton.setEnabled(!PREPARATION_GATE.isBusy() && !UPLOAD_GATE.isBusy());
            return;
        }

        List<PendingPhotoRecord> current = currentWorkOrderRecords(scan);
        boolean captureOrderReady = !current.isEmpty();
        for (PendingPhotoRecord record : current) {
            if (record.state() != PendingPhotoRecord.State.UPLOADED
                    || record.remoteFileId() == null
                    || record.remoteFileId().isBlank()) {
                captureOrderReady = false;
                break;
            }
        }

        if (captureOrderReady) {
            reconcileAllButton.setOnClickListener(v -> copyCaptureOrderManifest());
            reconcileAllButton.setText("Copy Capture Order (" + current.size() + ")");
            reconcileAllButton.setVisibility(View.VISIBLE);
            reconcileAllButton.setEnabled(!PREPARATION_GATE.isBusy() && !UPLOAD_GATE.isBusy());
        } else {
            reconcileAllButton.setOnClickListener(v -> reconcileAllUncertain());
            reconcileAllButton.setVisibility(View.GONE);
            reconcileAllButton.setEnabled(false);
        }
    }

    private List<PendingPhotoRecord> currentWorkOrderRecords(PendingPhotoStore.ScanResult scan) {
        List<PendingPhotoRecord> current = new ArrayList<>();
        if (scan == null || address == null || workOrder == null) {
            return current;
        }
        for (PendingPhotoRecord record : scan.records()) {
            if (address.id().equals(record.addressId())
                    && workOrder.id().equals(record.workOrderId())) {
                current.add(record);
            }
        }
        return current;
    }

    private void copyCaptureOrderManifest() {
        if (address == null || workOrder == null) {
            showPhotoStatus("Choose an exact address and work order before copying capture order.");
            return;
        }
        if (PREPARATION_GATE.isBusy() || UPLOAD_GATE.isBusy()) {
            showPhotoStatus("Wait for the active photo operation to finish before copying capture order.");
            return;
        }
        try {
            PendingPhotoStore.ScanResult scan = photoStore.scan();
            CaptureOrderManifest.Result manifest = CaptureOrderManifest.build(
                    PropertyDisplayName.fromDriveFolderName(address.name()),
                    PropertyDisplayName.readableFolderName(workOrder.name()),
                    address.id(),
                    workOrder.id(),
                    currentWorkOrderRecords(scan));
            ClipboardManager clipboard = getSystemService(ClipboardManager.class);
            if (clipboard == null) {
                throw new IllegalStateException("Android clipboard service is unavailable.");
            }
            clipboard.setPrimaryClip(ClipData.newPlainText(
                    "Field Photo Prep capture order",
                    manifest.text()));
            showPhotoStatus("Copied exact capture order for " + manifest.count()
                    + " confirmed photos. Drive was unchanged.");
        } catch (Exception error) {
            showError("Could not copy a safe capture-order manifest", error);
        }
        refreshPhotoList();
    }
'''

if old not in text:
    raise SystemExit('Reconcile-all method anchor not found')
text = text.replace(old, new, 1)
activity.write_text(text)

gradle = Path('app/build.gradle')
g = gradle.read_text()
if "versionCode 20" not in g or "versionName '0.15-bulk-reconcile'" not in g:
    raise SystemExit('Expected build version anchor not found')
g = g.replace('versionCode 20', 'versionCode 21', 1)
g = g.replace("versionName '0.15-bulk-reconcile'", "versionName '0.16-capture-order-export'", 1)
gradle.write_text(g)
