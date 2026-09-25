package com.inandout.fieldphotoprep;

final class AuthSessionState {
    private final String accessToken;
    private final String refreshToken;
    private final long expiresAtEpochSeconds;
    private final String userId;
    private final String email;
    private final String organizationId;
    private final String organizationName;
    private final String membershipId;
    private final String role;
    private final String membershipStatus;
    private final long lastMembershipValidatedAtEpochSeconds;

    AuthSessionState(
            String accessToken,
            String refreshToken,
            long expiresAtEpochSeconds,
            String userId,
            String email,
            String organizationId,
            String organizationName,
            String membershipId,
            String role,
            String membershipStatus,
            long lastMembershipValidatedAtEpochSeconds) {
        this.accessToken = require(accessToken, "accessToken");
        this.refreshToken = require(refreshToken, "refreshToken");
        this.expiresAtEpochSeconds = expiresAtEpochSeconds;
        this.userId = require(userId, "userId");
        this.email = email == null ? "" : email;
        this.organizationId = require(organizationId, "organizationId");
        this.organizationName = require(organizationName, "organizationName");
        this.membershipId = require(membershipId, "membershipId");
        this.role = require(role, "role");
        this.membershipStatus = require(membershipStatus, "membershipStatus");
        this.lastMembershipValidatedAtEpochSeconds = lastMembershipValidatedAtEpochSeconds;
    }

    String accessToken() { return accessToken; }
    String refreshToken() { return refreshToken; }
    long expiresAtEpochSeconds() { return expiresAtEpochSeconds; }
    String userId() { return userId; }
    String email() { return email; }
    String organizationId() { return organizationId; }
    String organizationName() { return organizationName; }
    String membershipId() { return membershipId; }
    String role() { return role; }
    String membershipStatus() { return membershipStatus; }
    long lastMembershipValidatedAtEpochSeconds() { return lastMembershipValidatedAtEpochSeconds; }

    boolean isActiveOwnerOrMember() {
        return "ACTIVE".equals(membershipStatus)
                && ("OWNER".equals(role) || "MEMBER".equals(role));
    }

    private static String require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
