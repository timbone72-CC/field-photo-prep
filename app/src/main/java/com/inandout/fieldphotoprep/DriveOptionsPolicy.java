package com.inandout.fieldphotoprep;

/**
 * Pure menu policy for the Home App & Drive overflow.
 *
 * Unsafe/unvalidated Drive state must never expose stale provider/company actions from another
 * Organization. Account recovery remains available so the operator can recheck authorization.
 */
final class DriveOptionsPolicy {
    private DriveOptionsPolicy() {}

    static CharSequence[] items(
            boolean bindingUsable,
            boolean hasWorkspace,
            boolean hasCurrentCompany) {
        if (!bindingUsable) {
            return new CharSequence[]{"Account"};
        }
        if (!hasWorkspace) {
            return new CharSequence[]{"Set Up Companies", "Account"};
        }
        if (!hasCurrentCompany) {
            return new CharSequence[]{"Choose Company", "Add Company", "Change Workspace", "Account"};
        }
        return new CharSequence[]{"Add Company", "Edit Company", "Change Workspace", "Account"};
    }
}
