package com.inandout.fieldphotoprep;

/**
 * Pure menu policy for the Home App & Drive overflow.
 *
 * Unsafe/unvalidated Drive state must never expose stale provider/company actions from another
 * Organization. Local app settings and account recovery remain available because they do not use
 * provider identity or mutate Drive.
 */
final class DriveOptionsPolicy {
    private DriveOptionsPolicy() {}

    static CharSequence[] items(
            boolean bindingUsable,
            boolean hasWorkspace,
            boolean hasCurrentCompany) {
        if (!bindingUsable) {
            return new CharSequence[]{"Suggest Archive After", "App Status", "Account"};
        }
        if (!hasWorkspace) {
            return new CharSequence[]{"Set Up Companies", "Suggest Archive After", "App Status", "Account"};
        }
        if (!hasCurrentCompany) {
            return new CharSequence[]{"Choose Company", "Add Company", "Change Workspace", "Suggest Archive After", "App Status", "Account"};
        }
        return new CharSequence[]{"Add Company", "Edit Company", "Change Workspace", "Suggest Archive After", "App Status", "Account"};
    }
}
