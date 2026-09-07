package com.inandout.fieldphotoprep;

import java.util.Locale;
import java.util.Objects;

public final class DriveFolder implements Comparable<DriveFolder> {
    private final String id;
    private final String name;

    public DriveFolder(String id, String name) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    @Override
    public int compareTo(DriveFolder other) {
        int byName = name.toLowerCase(Locale.US).compareTo(other.name.toLowerCase(Locale.US));
        return byName != 0 ? byName : id.compareTo(other.id);
    }

    @Override
    public String toString() {
        return name;
    }
}
