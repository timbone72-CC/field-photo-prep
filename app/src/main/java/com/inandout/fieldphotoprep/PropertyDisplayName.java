package com.inandout.fieldphotoprep;

final class PropertyDisplayName {
    private PropertyDisplayName() {}

    static String fromDriveFolderName(String raw) {
        String display = readableFolderName(raw);
        // Only the operator-identified terminal task suffix is known metadata.
        // Never guess city/street boundaries or alter provider names/identities.
        if (display.matches("^\\d.*")) {
            display = display.replaceFirst("(?i)(?:\\s+[-–]\\s*)?\\s+PRESSURE TEST$", "").trim();
        }
        return display;
    }

    static String readableFolderName(String raw) {
        return raw == null ? "" : raw.replace('_', ' ').replaceAll("\\s+", " ").trim();
    }
}
