package com.inandout.fieldphotoprep;

/**
 * Decides whether Home must reload provider-backed navigation after an authorization/binding
 * transition. This avoids both stale cross-Organization UI and unnecessary Drive reads on every
 * ordinary Activity resume.
 */
final class DriveBindingResumePolicy {
    private DriveBindingResumePolicy() {}

    static boolean shouldReload(
            OrganizationDriveBindingGuard.State previous,
            OrganizationDriveBindingGuard.State current) {
        if (current != OrganizationDriveBindingGuard.State.USABLE) {
            return false;
        }
        if (previous == null
                || previous == OrganizationDriveBindingGuard.State.USABLE
                || previous == OrganizationDriveBindingGuard.State.NO_WORKSPACE) {
            return false;
        }
        return true;
    }
}
