package com.inandout.fieldphotoprep;

public final class CompanyFolderName {
    private CompanyFolderName() {
    }

    public static String build(String rawName) {
        if (rawName == null) {
            throw new IllegalArgumentException("Company name is required.");
        }
        String trimmed = rawName.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Company name is required.");
        }
        return trimmed;
    }
}
