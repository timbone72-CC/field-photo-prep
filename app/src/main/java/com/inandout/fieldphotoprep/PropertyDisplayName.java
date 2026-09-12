package com.inandout.fieldphotoprep;

final class PropertyDisplayName {
    private PropertyDisplayName() {}

    static String fromDriveFolderName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace('_', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
