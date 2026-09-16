package com.inandout.fieldphotoprep;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AddressFolderAmbiguityTest {
    @Test
    public void caseAndSeparatorsArePossibleAliases() {
        assertTrue(AddressFolderAmbiguity.possibleSameProperty(
                "1607_CRESTVIEW_DR_CORDELL_OK",
                "1607 crestview dr, cordell ok"));
    }

    @Test
    public void observedWaltersPairIsAmbiguousButNotProvenIdentical() {
        assertTrue(AddressFolderAmbiguity.possibleSameProperty(
                "509_W_SOUTH_BOUNDARY_WALTERS_OK",
                "509 SOUTH BOUNDARY WALTERS OK"));
    }

    @Test
    public void differentHouseNumbersStayDistinct() {
        assertFalse(AddressFolderAmbiguity.possibleSameProperty(
                "509 W SOUTH BOUNDARY WALTERS OK",
                "510 SOUTH BOUNDARY WALTERS OK"));
    }

    @Test
    public void oppositeExplicitDirectionsStayDistinct() {
        assertFalse(AddressFolderAmbiguity.possibleSameProperty(
                "509 E MAIN ST",
                "509 W MAIN ST"));
    }

    @Test
    public void equivalentLeadingDirectionSpellingsAreAmbiguous() {
        assertTrue(AddressFolderAmbiguity.possibleSameProperty(
                "509 WEST MAIN ST",
                "509 W MAIN ST"));
    }

    @Test
    public void nonAddressLabelsAreNotFuzzyMatched() {
        assertFalse(AddressFolderAmbiguity.possibleSameProperty(
                "FIELD PHOTO PREP TEST",
                "FIELD-PHOTO-PREP TEST"));
    }

    @Test
    public void possibleMatchesKeepRealProviderIdentities() {
        DriveFolder oldFolder = new DriveFolder("old-provider-id", "509_W_SOUTH_BOUNDARY_WALTERS_OK");
        DriveFolder other = new DriveFolder("other-provider-id", "824 48TH ST LAWTON OK");
        List<DriveFolder> matches = AddressFolderAmbiguity.findPossibleMatches(
                Arrays.asList(oldFolder, other),
                "509 SOUTH BOUNDARY WALTERS OK");

        assertEquals(1, matches.size());
        assertEquals("old-provider-id", matches.get(0).id());
        assertEquals("509_W_SOUTH_BOUNDARY_WALTERS_OK", matches.get(0).name());
    }

    @Test
    public void ambiguousPeerRequiresDifferentProviderIdentity() {
        DriveFolder first = new DriveFolder("id-a", "509_W_SOUTH_BOUNDARY_WALTERS_OK");
        DriveFolder sameIdentity = new DriveFolder("id-a", "509 SOUTH BOUNDARY WALTERS OK");
        DriveFolder secondIdentity = new DriveFolder("id-b", "509 SOUTH BOUNDARY WALTERS OK");

        assertFalse(AddressFolderAmbiguity.hasAmbiguousPeer(
                first, Arrays.asList(first, sameIdentity)));
        assertTrue(AddressFolderAmbiguity.hasAmbiguousPeer(
                first, Arrays.asList(first, secondIdentity)));
    }
}
