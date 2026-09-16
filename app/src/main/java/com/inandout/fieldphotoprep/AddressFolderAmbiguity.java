package com.inandout.fieldphotoprep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Conservative address-name comparison used only to stop silent duplicate-property decisions.
 *
 * A possible match is never proof that two provider document IDs represent one property. Callers
 * may use this helper to require operator choice, but must never auto-merge, rename, move, delete,
 * or substitute a provider identity from this comparison alone.
 */
final class AddressFolderAmbiguity {
    private AddressFolderAmbiguity() {
    }

    /**
     * Exact provider names retain contract precedence. Fuzzy candidates are returned only when no
     * exact provider display-name match exists, so an approved exact unique reuse is never weakened.
     */
    static List<DriveFolder> findPossibleMatches(
            List<DriveFolder> folders,
            String requestedName) {
        List<DriveFolder> exact = new ArrayList<>();
        List<DriveFolder> possible = new ArrayList<>();
        if (folders == null || requestedName == null) {
            return possible;
        }
        for (DriveFolder folder : folders) {
            if (folder == null) {
                continue;
            }
            if (folder.name().equals(requestedName)) {
                exact.add(folder);
            } else if (possibleSameProperty(folder.name(), requestedName)) {
                possible.add(folder);
            }
        }
        if (!exact.isEmpty()) {
            Collections.sort(exact);
            return exact;
        }
        Collections.sort(possible);
        return possible;
    }

    static boolean hasAmbiguousPeer(DriveFolder selected, List<DriveFolder> folders) {
        if (selected == null || folders == null) {
            return false;
        }
        for (DriveFolder other : folders) {
            if (other == null || selected.id().equals(other.id())) {
                continue;
            }
            if (possibleSameProperty(selected.name(), other.name())) {
                return true;
            }
        }
        return false;
    }

    static boolean possibleSameProperty(String first, String second) {
        if (first == null || second == null) {
            return false;
        }
        if (first.equals(second)) {
            return true;
        }

        List<String> a = tokens(first);
        List<String> b = tokens(second);
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        if (!looksLikeNumberedAddress(a) || !looksLikeNumberedAddress(b)) {
            return false;
        }
        if (!a.get(0).equals(b.get(0))) {
            return false;
        }
        if (a.equals(b)) {
            return true;
        }

        if (sameLeadingDirectionSpelling(a, b)) {
            return true;
        }

        // One source may include a leading compass qualifier that another source omitted.
        // This is ambiguity only: never authoritative identity.
        if (isLeadingDirection(a) && withoutToken(a, 1).equals(b)) {
            return true;
        }
        return isLeadingDirection(b) && withoutToken(b, 1).equals(a);
    }

    private static List<String> tokens(String raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        String normalized = raw.toUpperCase(Locale.US)
                .replace('_', ' ')
                .replaceAll("[^A-Z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }
        String[] split = normalized.split(" ");
        List<String> result = new ArrayList<>(split.length);
        Collections.addAll(result, split);
        return result;
    }

    private static boolean looksLikeNumberedAddress(List<String> value) {
        return value.size() >= 3 && value.get(0).matches("\\d+[A-Z]?");
    }

    private static boolean isLeadingDirection(List<String> value) {
        return value.size() > 2 && canonicalDirection(value.get(1)) != null;
    }

    private static boolean sameLeadingDirectionSpelling(List<String> a, List<String> b) {
        if (!isLeadingDirection(a) || !isLeadingDirection(b) || a.size() != b.size()) {
            return false;
        }
        String firstDirection = canonicalDirection(a.get(1));
        String secondDirection = canonicalDirection(b.get(1));
        if (!firstDirection.equals(secondDirection)) {
            return false;
        }
        for (int index = 2; index < a.size(); index++) {
            if (!a.get(index).equals(b.get(index))) {
                return false;
            }
        }
        return true;
    }

    private static String canonicalDirection(String token) {
        switch (token) {
            case "N":
            case "NORTH":
                return "N";
            case "S":
            case "SOUTH":
                return "S";
            case "E":
            case "EAST":
                return "E";
            case "W":
            case "WEST":
                return "W";
            case "NE":
            case "NORTHEAST":
                return "NE";
            case "NW":
            case "NORTHWEST":
                return "NW";
            case "SE":
            case "SOUTHEAST":
                return "SE";
            case "SW":
            case "SOUTHWEST":
                return "SW";
            default:
                return null;
        }
    }

    private static List<String> withoutToken(List<String> source, int indexToRemove) {
        List<String> copy = new ArrayList<>(source.size() - 1);
        for (int index = 0; index < source.size(); index++) {
            if (index != indexToRemove) {
                copy.add(source.get(index));
            }
        }
        return copy;
    }
}
