package com.inandout.fieldphotoprep;

public final class AddressFolderName {
    private AddressFolderName() {
    }

    public static String build(String rawName) {
        if (rawName == null) {
            throw new IllegalArgumentException("Address folder name is required.");
        }
        String trimmed = rawName.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Address folder name is required.");
        }
        return trimmed;
    }
}
