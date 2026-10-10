package com.inandout.fieldphotoprep;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public final class SharedCycleGateTest {
    private final UUID phoneA = UUID.randomUUID();
    private final UUID phoneB = UUID.randomUUID();

    private SharedCycleGate paired() {
        SharedCycleGate gate = new SharedCycleGate();
        gate.enroll(phoneA);
        gate.enroll(phoneB);
        return gate;
    }

    private UUID uploaded(SharedCycleGate gate, UUID deviceId) {
        UUID photoId = UUID.randomUUID();
        gate.beginUpload(deviceId, gate.generation(), photoId);
        gate.confirmUpload(deviceId, gate.generation(), photoId);
        return photoId;
    }

    @Test
    public void bothPhonesReceiveOneSharedSequenceAndFirstConfirmedUploaderLeads() {
        SharedCycleGate gate = paired();
        SharedCycleGate.Reservation first = gate.beginUpload(
                phoneB, 1, UUID.randomUUID());
        SharedCycleGate.Reservation second = gate.beginUpload(
                phoneA, 1, UUID.randomUUID());
        assertEquals(1, first.sequence);
        assertEquals(2, second.sequence);
        assertEquals(first.cycleId, second.cycleId);
        assertNull(gate.leadDevice());
        gate.confirmUpload(phoneA, 1, second.photoId);
        assertEquals(phoneA, gate.leadDevice());
        gate.confirmUpload(phoneB, 1, first.photoId);
        assertEquals(phoneA, gate.leadDevice());
    }

    @Test
    public void preferredFirstUploaderStopsOtherPhoneUntilPreferredUpload() {
        SharedCycleGate gate = paired();
        gate.chooseFirstUploader(phoneA, 1, phoneB);
        assertThrows(IllegalStateException.class,
                () -> gate.beginUpload(phoneA, 1, UUID.randomUUID()));
        uploaded(gate, phoneB);
        assertEquals(phoneB, gate.leadDevice());
        assertEquals(2, gate.beginUpload(phoneA, 1, UUID.randomUUID()).sequence);
    }

    @Test
    public void unverifiedOrDuplicatePhotoCanNeverCreateAnotherAttempt() {
        SharedCycleGate gate = paired();
        UUID photo = UUID.randomUUID();
        gate.beginUpload(phoneA, 1, photo);
        assertThrows(IllegalStateException.class, () -> gate.beginUpload(phoneA, 1, photo));
        assertThrows(IllegalStateException.class, () -> gate.confirmUpload(phoneB, 1, photo));
        gate.confirmUpload(phoneA, 1, photo);
        assertThrows(IllegalStateException.class, () -> gate.beginUpload(phoneA, 1, photo));
    }

    @Test
    public void quiescingRequiresBothPhonesAndBlocksNewUploads() {
        SharedCycleGate gate = paired();
        uploaded(gate, phoneA);
        gate.requestClear(phoneA, 1);
        assertEquals(SharedCycleGate.Phase.QUIESCING, gate.phase());
        assertThrows(IllegalStateException.class,
                () -> gate.beginUpload(phoneB, 1, UUID.randomUUID()));
        gate.acknowledgeSafeToClear(phoneA, 1, true);
        assertThrows(IllegalStateException.class, () -> gate.beginClear(phoneA, 1));
        assertThrows(IllegalStateException.class,
                () -> gate.acknowledgeSafeToClear(phoneB, 1, false));
        gate.acknowledgeSafeToClear(phoneB, 1, true);
        assertEquals(1L, gate.generation());
        assertEquals(SharedCycleGate.Phase.CLEARING, gate.phase());
    }

    @Test
    public void stalePhoneCannotClearOrUploadIntoNewGeneration() {
        SharedCycleGate gate = paired();
        UUID oldPhoto = uploaded(gate, phoneA);
        UUID before = gate.cycleId();
        gate.requestClear(phoneA, 1);
        gate.acknowledgeSafeToClear(phoneA, 1, true);
        gate.acknowledgeSafeToClear(phoneB, 1, true);
        UUID operation = gate.beginClear(phoneA, 1);
        gate.finishClear(phoneA, 1, operation, true);
        assertEquals(2, gate.generation());
        assertNotEquals(before, gate.cycleId());
        assertEquals(1, gate.nextSequence());
        UUID newPhoto = uploaded(gate, phoneA);
        assertEquals(2, gate.nextSequence());
        assertThrows(IllegalStateException.class, () -> gate.requestClear(phoneB, 1));
        assertThrows(IllegalStateException.class,
                () -> gate.beginUpload(phoneB, 1, UUID.randomUUID()));
        assertThrows(IllegalStateException.class, () -> gate.beginUpload(phoneB, 2, oldPhoto));
        assertThrows(IllegalStateException.class, () -> gate.beginUpload(phoneA, 2, newPhoto));
        assertEquals(SharedCycleGate.Phase.OPEN, gate.phase());
    }

    @Test
    public void inFlightUploadCannotBeClearedByOtherPhone() {
        SharedCycleGate gate = paired();
        uploaded(gate, phoneA);
        UUID photoB = UUID.randomUUID();
        gate.beginUpload(phoneB, 1, photoB);
        assertThrows(IllegalStateException.class, () -> gate.requestClear(phoneA, 1));
        gate.confirmUpload(phoneB, 1, photoB);
        gate.requestClear(phoneA, 1);
        assertEquals(SharedCycleGate.Phase.QUIESCING, gate.phase());
    }

    @Test
    public void uncertainUploadBlocksTheCycleEvenAfterOtherPhoneAttempts() {
        SharedCycleGate gate = paired();
        uploaded(gate, phoneA);
        UUID photoB = UUID.randomUUID();
        gate.beginUpload(phoneB, 1, photoB);
        gate.markUploadUncertain(phoneB, 1, photoB);
        assertEquals(SharedCycleGate.Phase.RECOVERY_BLOCKED, gate.phase());
        assertThrows(IllegalStateException.class, () -> gate.requestClear(phoneA, 1));
        assertThrows(IllegalStateException.class,
                () -> gate.beginUpload(phoneA, 1, UUID.randomUUID()));
        assertThrows(IllegalStateException.class, () -> gate.beginClear(phoneA, 1));
    }

    @Test
    public void uncertainDeleteKeepsFenceAndNeverOpensNextCycle() {
        SharedCycleGate gate = paired();
        uploaded(gate, phoneA);
        gate.requestClear(phoneA, 1);
        gate.acknowledgeSafeToClear(phoneA, 1, true);
        gate.acknowledgeSafeToClear(phoneB, 1, true);
        UUID operation = gate.beginClear(phoneA, 1);
        gate.markClearUncertain(phoneA, 1, operation);
        assertEquals(1, gate.generation());
        assertEquals(SharedCycleGate.Phase.RECOVERY_BLOCKED, gate.phase());
        assertThrows(IllegalStateException.class, () -> gate.finishClear(phoneA, 1, operation, true));
        assertThrows(IllegalStateException.class,
                () -> gate.beginUpload(phoneB, 1, UUID.randomUUID()));
    }

    @Test
    public void unverifiedClearCannotAdvanceGeneration() {
        SharedCycleGate gate = paired();
        uploaded(gate, phoneA);
        gate.requestClear(phoneA, 1);
        gate.acknowledgeSafeToClear(phoneA, 1, true);
        gate.acknowledgeSafeToClear(phoneB, 1, true);
        UUID operation = gate.beginClear(phoneA, 1);
        assertThrows(IllegalStateException.class,
                () -> gate.finishClear(phoneA, 1, operation, false));
        assertEquals(1, gate.generation());
        assertEquals(SharedCycleGate.Phase.RECOVERY_BLOCKED, gate.phase());
    }

    @Test
    public void modeBackToOneRetainsManagedCycleAndGuards() {
        SharedCycleGate gate = paired();
        uploaded(gate, phoneA);
        gate.setMode(phoneA, 1, SharedCycleGate.Mode.ONE);
        assertEquals(SharedCycleGate.Mode.ONE, gate.mode());
        assertEquals(2, gate.participantCount());
        assertEquals(1, gate.generation());
        assertThrows(IllegalStateException.class,
                () -> gate.beginUpload(phoneB, 1, UUID.randomUUID()));
        gate.requestClear(phoneA, 1);
        assertThrows(IllegalStateException.class, () -> gate.beginClear(phoneA, 1));
    }

    @Test
    public void finishOnlyLeadAndDoesNotDeleteOrAdvance() {
        SharedCycleGate gate = paired();
        uploaded(gate, phoneA);
        assertThrows(IllegalStateException.class, () -> gate.finishPhotoWork(phoneB, 1));
        gate.finishPhotoWork(phoneA, 1);
        assertEquals(SharedCycleGate.Phase.FINISHED, gate.phase());
        assertEquals(1, gate.generation());
        assertThrows(IllegalStateException.class,
                () -> gate.beginUpload(phoneA, 1, UUID.randomUUID()));
        gate.requestClear(phoneA, 1); // Deliberate later reuse is still a separate action.
        assertEquals(SharedCycleGate.Phase.QUIESCING, gate.phase());
    }

    @Test
    public void staleAckCannotAuthorizeADelete() {
        SharedCycleGate gate = paired();
        uploaded(gate, phoneA);
        gate.requestClear(phoneA, 1);
        assertThrows(IllegalStateException.class,
                () -> gate.acknowledgeSafeToClear(phoneB, 0, true));
        gate.acknowledgeSafeToClear(phoneA, 1, true);
        assertThrows(IllegalStateException.class, () -> gate.beginClear(phoneA, 1));
    }
}
