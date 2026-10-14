package com.inandout.fieldphotoprep;

/** Displays confirmed provider counts separately from local queued-photo history. */
final class SharedDrivePhotoCountLabel {
    private SharedDrivePhotoCountLabel() {}

    static String format(Integer verifiedPhotoCount, boolean providerUnavailable) {
        if (verifiedPhotoCount != null && verifiedPhotoCount >= 0) {
            return "In Drive: " + verifiedPhotoCount;
        }
        return providerUnavailable ? "In Drive: unavailable" : "In Drive: —";
    }
}
