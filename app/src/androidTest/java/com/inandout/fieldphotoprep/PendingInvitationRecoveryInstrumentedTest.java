package com.inandout.fieldphotoprep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class PendingInvitationRecoveryInstrumentedTest {
    private static final String ID = "2e7e7a59-7593-4cb4-92bd-099acd622d0a";

    @Test
    public void verifiedPendingInvitationDetailsAreParsed() throws Exception {
        JSONObject found = new JSONObject();
        found.put("outcome", "FOUND");
        found.put("invitation_id", ID);
        found.put("organization_name", "In And Out Cleaner Inspections LLC");
        found.put("intended_role", "MEMBER");

        SupabaseAuthClient.PendingInvitation pending =
                SupabaseAuthClient.parsePendingInvitation(found);
        assertEquals(ID, pending.invitationId());
        assertEquals("In And Out Cleaner Inspections LLC", pending.organizationName());
        assertEquals("MEMBER", pending.intendedRole());
    }

    @Test
    public void absentInvitationDoesNotGrantEntry() throws Exception {
        assertNull(SupabaseAuthClient.parsePendingInvitation(
                new JSONObject().put("outcome", "NO_PENDING")));
    }

    @Test
    public void ambiguousOrUnconfirmedInvitationsFailClosed() throws Exception {
        for (String outcome : new String[]{
                "MULTIPLE_PENDING", "EMAIL_NOT_CONFIRMED", "NOT_ELIGIBLE", "UNKNOWN"}) {
            assertThrows(SupabaseAuthClient.AuthException.class,
                    () -> SupabaseAuthClient.parsePendingInvitation(
                            new JSONObject().put("outcome", outcome)));
        }
    }

    @Test
    public void malformedReturnedInvitationCannotBeAccepted() throws Exception {
        for (String id : new String[]{"", "not-a-uuid"}) {
            JSONObject found = new JSONObject()
                    .put("outcome", "FOUND")
                    .put("invitation_id", id)
                    .put("organization_name", "Some Organization")
                    .put("intended_role", "MEMBER");
            assertThrows(SupabaseAuthClient.AuthException.class,
                    () -> SupabaseAuthClient.parsePendingInvitation(found));
        }
        JSONObject invalidRole = new JSONObject()
                .put("outcome", "FOUND")
                .put("invitation_id", ID)
                .put("organization_name", "Some Organization")
                .put("intended_role", "SUPERUSER");
        assertThrows(SupabaseAuthClient.AuthException.class,
                () -> SupabaseAuthClient.parsePendingInvitation(invalidRole));
    }
}
