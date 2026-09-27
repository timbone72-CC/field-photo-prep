package com.inandout.fieldphotoprep;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * Read-only adapter from existing runtime/store owners into the Phase 12I snapshot.
 */
final class AppStatusCollector {
    private final Context context;
    private final RuntimeAuthorizationManager authorizationManager;
    private final FolderPrefs folderPrefs;
    private final OrganizationDriveBindingGuard driveBindingGuard;
    private final PendingPhotoStore photoStore;
    private final PhotoPreparer photoPreparer;
    private final String appVersion;

    static AppStatusCollector create(
            Context context,
            RuntimeAuthorizationManager authorizationManager) {
        Context appContext = Objects.requireNonNull(context, "context").getApplicationContext();
        FolderPrefs folderPrefs = new FolderPrefs(appContext);
        AuthorizationActionGuard authorizationGuard =
                new AuthorizationActionGuard(authorizationManager);
        OrganizationDriveBindingGuard bindingGuard =
                new OrganizationDriveBindingGuard(
                        folderPrefs,
                        authorizationGuard,
                        treeUri -> hasPersistedReadPermission(appContext, treeUri));
        return new AppStatusCollector(
                appContext,
                authorizationManager,
                folderPrefs,
                bindingGuard,
                new PendingPhotoStore(new File(appContext.getFilesDir(), "pending_photos")),
                new PhotoPreparer(new File(appContext.getFilesDir(), "prepared_photos")),
                BuildConfig.VERSION_NAME);
    }

    AppStatusCollector(
            Context context,
            RuntimeAuthorizationManager authorizationManager,
            FolderPrefs folderPrefs,
            OrganizationDriveBindingGuard driveBindingGuard,
            PendingPhotoStore photoStore,
            PhotoPreparer photoPreparer,
            String appVersion) {
        this.context = Objects.requireNonNull(context, "context");
        this.authorizationManager =
                Objects.requireNonNull(authorizationManager, "authorizationManager");
        this.folderPrefs = Objects.requireNonNull(folderPrefs, "folderPrefs");
        this.driveBindingGuard = Objects.requireNonNull(driveBindingGuard, "driveBindingGuard");
        this.photoStore = Objects.requireNonNull(photoStore, "photoStore");
        this.photoPreparer = Objects.requireNonNull(photoPreparer, "photoPreparer");
        this.appVersion = Objects.requireNonNull(appVersion, "appVersion");
    }

    AppStatusSnapshot collect() {
        AuthorizationDecision decision = authorizationManager.currentDecision();
        AuthSessionState session = authorizationManager.storedSession();
        OrganizationDriveBindingGuard.Result binding = driveBindingGuard.current();
        DriveFolder currentCompany = binding.isUsable() ? folderPrefs.getCurrentCompany() : null;
        boolean cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;

        return AppStatusSnapshot.from(
                decision,
                session,
                binding.state(),
                currentCompany == null ? null : currentCompany.name(),
                readQueueCounts(),
                appVersion,
                cameraGranted);
    }

    private AppStatusSnapshot.QueueCounts readQueueCounts() {
        try {
            PendingPhotoStore.ScanResult scan =
                    photoStore.scanAllPersistedRecordsForProtection();
            int capturing = 0;
            int waiting = 0;
            int uploading = 0;
            int failed = 0;
            int uncertain = 0;
            int uploaded = 0;
            int protectedOriginals = 0;

            for (PendingPhotoRecord record : scan.records()) {
                switch (record.state()) {
                    case CAPTURING:
                        capturing++;
                        break;
                    case WAITING:
                        waiting++;
                        break;
                    case UPLOADING:
                        uploading++;
                        break;
                    case FAILED:
                        failed++;
                        break;
                    case UNCERTAIN:
                        uncertain++;
                        break;
                    case UPLOADED:
                        uploaded++;
                        break;
                    default:
                        throw new IOException("Unsupported local queue state.");
                }
                if (photoStore.hasImageData(record)) {
                    protectedOriginals++;
                }
            }

            ProtectedWorkGuard.Result protectedWork =
                    new ProtectedWorkGuard(photoStore, photoPreparer).inspect();

            return new AppStatusSnapshot.QueueCounts(
                    capturing,
                    waiting,
                    uploading,
                    failed,
                    uncertain,
                    uploaded,
                    protectedOriginals,
                    protectedWork.cleanupPendingCount(),
                    Math.max(
                            scan.corruptMetadataFiles().size(),
                            protectedWork.unreadableCount()));
        } catch (IOException | RuntimeException error) {
            // Diagnostics must never mutate/repair local state merely to make a status screen work.
            // Fail closed into an explicit unavailable state without copying raw error details.
            return AppStatusSnapshot.QueueCounts.unavailable();
        }
    }

    private static boolean hasPersistedReadPermission(Context context, Uri treeUri) {
        if (treeUri == null) {
            return false;
        }
        for (android.content.UriPermission permission
                : context.getContentResolver().getPersistedUriPermissions()) {
            if (treeUri.equals(permission.getUri()) && permission.isReadPermission()) {
                return true;
            }
        }
        return false;
    }
}
