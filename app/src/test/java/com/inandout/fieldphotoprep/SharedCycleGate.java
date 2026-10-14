package com.inandout.fieldphotoprep;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Testable reference policy for Phase 14 coordinated work cycles.
 *
 * NOT the authority for live two-phone work: the authoritative implementation must run
 * transactionally in the FPP Supabase RPC, retain its state durably, and bind both actual
 * Android SAF selections to one proven remote folder. No Android caller uses this class.
 */
final class SharedCycleGate {
    enum Mode { ONE, TWO }
    enum Phase { OPEN, QUIESCING, CLEARING, FINISHING, FINISHED, RECOVERY_BLOCKED }
    enum UploadState { IN_FLIGHT, UNCERTAIN, CONFIRMED }

    static final class Reservation {
        final long generation;
        final UUID cycleId;
        final UUID photoId;
        final UUID deviceId;
        final int sequence;
        UploadState state;

        Reservation(long generation, UUID cycleId, UUID photoId, UUID deviceId, int sequence) {
            this.generation = generation;
            this.cycleId = cycleId;
            this.photoId = photoId;
            this.deviceId = deviceId;
            this.sequence = sequence;
            this.state = UploadState.IN_FLIGHT;
        }
    }

    private final Set<UUID> participants = new HashSet<>();
    private final Set<UUID> clearAcknowledgements = new HashSet<>();
    private final Map<UUID, Reservation> reservations = new HashMap<>();
    private UUID cycleId = UUID.randomUUID();
    private long generation = 1;
    private int nextSequence = 1;
    private Mode mode = Mode.TWO;
    private Phase phase = Phase.OPEN;
    private UUID preferredLead;
    private UUID lead;
    private UUID clearingOperation;

    synchronized void enroll(UUID deviceId) {
        require(deviceId);
        requirePhase(Phase.OPEN);
        // A participant cannot be silently added while one phone's upload is in flight.
        if (hasUnsettledUpload()) {
            throw new IllegalStateException("An in-flight upload blocks enrollment.");
        }
        participants.add(deviceId);
    }

    synchronized void setMode(UUID deviceId, long expectedGeneration, Mode requestedMode) {
        requireParticipant(deviceId, expectedGeneration);
        requirePhase(Phase.OPEN);
        if (hasUnsettledUpload()) {
            throw new IllegalStateException("Changing photographers waits for uploads to settle.");
        }
        mode = Objects.requireNonNull(requestedMode, "mode");
        // Enrollment, durable generation and existing photo reservations are never removed.
    }

    synchronized void chooseFirstUploader(
            UUID deviceId, long expectedGeneration, UUID preferredDevice) {
        requireParticipant(deviceId, expectedGeneration);
        requirePhase(Phase.OPEN);
        if (!participants.contains(require(preferredDevice)) || lead != null
                || hasAnyReservationInCurrentGeneration()) {
            throw new IllegalStateException(
                    "Choose a registered first uploader before the first reservation.");
        }
        preferredLead = preferredDevice;
    }

    synchronized Reservation beginUpload(
            UUID deviceId, long expectedGeneration, UUID photoId) {
        requireParticipant(deviceId, expectedGeneration);
        requirePhase(Phase.OPEN);
        if (mode != Mode.TWO) {
            throw new IllegalStateException("Shared numbering requires two-photographer mode.");
        }
        UUID photo = require(photoId);
        Reservation existing = reservations.get(photo);
        if (existing != null) {
            // Never create a second remote file for an in-flight, uncertain, confirmed,
            // or prior-generation UUID. The caller must reconcile it instead.
            throw new IllegalStateException("Photo is already reserved; reconcile before retry.");
        }
        if (preferredLead != null && lead == null && !preferredLead.equals(deviceId)) {
            throw new IllegalStateException("Selected first uploader has not uploaded yet.");
        }
        if (nextSequence == Integer.MAX_VALUE) {
            throw new IllegalStateException("Shared sequence exhausted.");
        }
        Reservation created = new Reservation(
                generation, cycleId, photo, deviceId, nextSequence++);
        reservations.put(photo, created);
        return created;
    }

    synchronized void confirmUpload(UUID deviceId, long expectedGeneration, UUID photoId) {
        requireParticipant(deviceId, expectedGeneration);
        Reservation r = requireReservation(deviceId, photoId);
        if (r.generation != generation || (phase != Phase.OPEN && phase != Phase.QUIESCING)
                || r.state != UploadState.IN_FLIGHT) {
            throw new IllegalStateException("Upload confirmation does not match active flight.");
        }
        r.state = UploadState.CONFIRMED;
        if (lead == null) {
            lead = deviceId;
        }
    }

    synchronized void markUploadUncertain(UUID deviceId, long expectedGeneration, UUID photoId) {
        requireParticipant(deviceId, expectedGeneration);
        Reservation r = requireReservation(deviceId, photoId);
        if (r.generation != generation || r.state != UploadState.IN_FLIGHT) {
            throw new IllegalStateException("No matching in-flight upload.");
        }
        r.state = UploadState.UNCERTAIN;
        phase = Phase.RECOVERY_BLOCKED;
    }

    synchronized void requestClear(UUID deviceId, long expectedGeneration) {
        requireLead(deviceId, expectedGeneration);
        if (phase != Phase.OPEN && phase != Phase.FINISHED) {
            throw new IllegalStateException("Clear cannot start during another operation.");
        }
        if (hasUnsettledUpload()) {
            throw new IllegalStateException("Outstanding upload blocks destructive reuse.");
        }
        phase = Phase.QUIESCING;
        clearAcknowledgements.clear();
    }

    synchronized void acknowledgeSafeToClear(UUID deviceId, long expectedGeneration,
                                              boolean localQueueSettled) {
        requireParticipant(deviceId, expectedGeneration);
        requirePhase(Phase.QUIESCING);
        if (!localQueueSettled) {
            throw new IllegalStateException("Protected local photos block clearing.");
        }
        clearAcknowledgements.add(deviceId);
    }

    synchronized UUID beginClear(UUID deviceId, long expectedGeneration) {
        requireLead(deviceId, expectedGeneration);
        requirePhase(Phase.QUIESCING);
        if (hasUnsettledUpload() || !clearAcknowledgements.containsAll(participants)
                || participants.isEmpty()) {
            throw new IllegalStateException("All enrolled phones must be safely settled.");
        }
        phase = Phase.CLEARING;
        clearingOperation = UUID.randomUUID();
        return clearingOperation;
    }

    synchronized void finishClear(UUID deviceId, long expectedGeneration, UUID operationId,
                                  boolean exactProviderVerifiedEmpty) {
        requireLead(deviceId, expectedGeneration);
        requirePhase(Phase.CLEARING);
        if (!Objects.equals(clearingOperation, require(operationId))
                || !exactProviderVerifiedEmpty) {
            // Even a reported incomplete/ambiguous provider delete stays durably fenced.
            phase = Phase.RECOVERY_BLOCKED;
            throw new IllegalStateException("Clear result is not conclusively verified.");
        }
        generation++;
        cycleId = UUID.randomUUID();
        nextSequence = 1;
        phase = Phase.OPEN;
        clearingOperation = null;
        clearAcknowledgements.clear();
        lead = null;
        preferredLead = null;
    }

    synchronized void markClearUncertain(UUID deviceId, long expectedGeneration,
                                         UUID operationId) {
        requireLead(deviceId, expectedGeneration);
        requirePhase(Phase.CLEARING);
        if (!Objects.equals(clearingOperation, require(operationId))) {
            throw new IllegalStateException("Wrong clearing-operation identity.");
        }
        phase = Phase.RECOVERY_BLOCKED;  // Never auto-unlock on a timeout.
    }

    synchronized void requestFinishPhotoWork(UUID deviceId, long expectedGeneration) {
        requireLead(deviceId, expectedGeneration);
        requirePhase(Phase.OPEN);
        if (hasUnsettledUpload()) {
            throw new IllegalStateException("Cannot finish with unresolved uploads.");
        }
        phase = Phase.FINISHING;
        clearAcknowledgements.clear();
    }

    synchronized void acknowledgeSafeToFinish(UUID deviceId, long expectedGeneration,
                                              boolean localQueueSettled) {
        requireParticipant(deviceId, expectedGeneration);
        requirePhase(Phase.FINISHING);
        if (!localQueueSettled) {
            throw new IllegalStateException("Protected local photos block finishing.");
        }
        clearAcknowledgements.add(deviceId);
    }

    synchronized void finishPhotoWork(UUID deviceId, long expectedGeneration) {
        requireLead(deviceId, expectedGeneration);
        requirePhase(Phase.FINISHING);
        if (hasUnsettledUpload() || participants.isEmpty()
                || !clearAcknowledgements.containsAll(participants)) {
            throw new IllegalStateException("Both phones must acknowledge settled work.");
        }
        phase = Phase.FINISHED; // No Drive deletion: Clear & Reuse remains separate.
        clearAcknowledgements.clear();
    }

    synchronized long generation() { return generation; }
    synchronized UUID cycleId() { return cycleId; }
    synchronized int nextSequence() { return nextSequence; }
    synchronized UUID leadDevice() { return lead; }
    synchronized Phase phase() { return phase; }
    synchronized Mode mode() { return mode; }
    synchronized int participantCount() { return participants.size(); }

    private boolean hasAnyReservationInCurrentGeneration() {
        for (Reservation reservation : reservations.values()) {
            if (reservation.generation == generation) {
                return true;
            }
        }
        return false;
    }

    private boolean hasUnsettledUpload() {
        for (Reservation r : reservations.values()) {
            if (r.generation == generation && r.state != UploadState.CONFIRMED) {
                return true;
            }
        }
        return false;
    }

    private Reservation requireReservation(UUID deviceId, UUID photoId) {
        Reservation result = reservations.get(require(photoId));
        if (result == null || !result.deviceId.equals(deviceId)) {
            throw new IllegalStateException("This device does not own that photo.");
        }
        return result;
    }

    private void requireLead(UUID deviceId, long expectedGeneration) {
        requireParticipant(deviceId, expectedGeneration);
        if (!deviceId.equals(lead)) {
            throw new IllegalStateException("Only the current lead can finish or clear.");
        }
    }

    private void requireParticipant(UUID deviceId, long expectedGeneration) {
        if (expectedGeneration != generation || !participants.contains(require(deviceId))) {
            throw new IllegalStateException("Stale cycle or unregistered phone.");
        }
    }

    private void requirePhase(Phase expected) {
        if (phase != expected) {
            throw new IllegalStateException("Work cycle is " + phase + ", not " + expected);
        }
    }

    private static UUID require(UUID id) {
        return Objects.requireNonNull(id, "identity");
    }
}
