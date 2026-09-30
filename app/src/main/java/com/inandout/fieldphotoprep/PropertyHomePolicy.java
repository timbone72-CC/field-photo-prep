package com.inandout.fieldphotoprep;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class PropertyHomePolicy {
    private PropertyHomePolicy() {}

    static List<DriveFolder> visibleProperties(
            List<DriveFolder> discovered,
            Map<String, PropertyLifecycleStore.Snapshot> lifecycleByAddressId,
            String rawQuery) {
        String query = normalize(rawQuery);
        boolean searching = !query.isEmpty();
        List<DriveFolder> result = new ArrayList<>();

        if (discovered != null) {
            for (DriveFolder folder : discovered) {
                if (folder == null) {
                    continue;
                }
                PropertyLifecycleStore.Snapshot snapshot = snapshot(
                        lifecycleByAddressId,
                        folder.id());
                if (snapshot.state() == PropertyLifecycleStore.State.DELETED) {
                    continue;
                }
                if (!searching && snapshot.state() != PropertyLifecycleStore.State.ACTIVE) {
                    continue;
                }
                if (searching && !matches(folder, query)) {
                    continue;
                }
                result.add(folder);
            }
        }

        result.sort(propertyComparator(lifecycleByAddressId));
        return result;
    }

    static boolean isArchived(
            Map<String, PropertyLifecycleStore.Snapshot> lifecycleByAddressId,
            String addressId) {
        return snapshot(lifecycleByAddressId, addressId).state()
                == PropertyLifecycleStore.State.ARCHIVED;
    }

    private static Comparator<DriveFolder> propertyComparator(
            Map<String, PropertyLifecycleStore.Snapshot> lifecycleByAddressId) {
        return (first, second) -> {
            long firstLastUsed = snapshot(lifecycleByAddressId, first.id()).lastUsedEpochMs();
            long secondLastUsed = snapshot(lifecycleByAddressId, second.id()).lastUsedEpochMs();
            if (firstLastUsed != secondLastUsed) {
                return Long.compare(secondLastUsed, firstLastUsed);
            }

            String firstDisplay = PropertyDisplayName.fromDriveFolderName(first.name())
                    .toLowerCase(Locale.US);
            String secondDisplay = PropertyDisplayName.fromDriveFolderName(second.name())
                    .toLowerCase(Locale.US);
            int byDisplay = firstDisplay.compareTo(secondDisplay);
            return byDisplay != 0 ? byDisplay : first.id().compareTo(second.id());
        };
    }

    private static PropertyLifecycleStore.Snapshot snapshot(
            Map<String, PropertyLifecycleStore.Snapshot> lifecycleByAddressId,
            String addressId) {
        if (lifecycleByAddressId == null) {
            return new PropertyLifecycleStore.Snapshot(
                    PropertyLifecycleStore.State.ACTIVE,
                    0L);
        }
        PropertyLifecycleStore.Snapshot snapshot = lifecycleByAddressId.get(addressId);
        return snapshot == null
                ? new PropertyLifecycleStore.Snapshot(PropertyLifecycleStore.State.ACTIVE, 0L)
                : snapshot;
    }

    private static boolean matches(DriveFolder folder, String normalizedQuery) {
        String display = normalize(PropertyDisplayName.fromDriveFolderName(folder.name()));
        String raw = normalize(folder.name().replace('_', ' '));
        return display.contains(normalizedQuery) || raw.contains(normalizedQuery);
    }

    private static String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.US).replaceAll("\\s+", " ");
    }
}
