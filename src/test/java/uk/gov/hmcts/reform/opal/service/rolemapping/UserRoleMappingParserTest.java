package uk.gov.hmcts.reform.opal.service.rolemapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserRoleMappingParserTest {

    private final UserRoleMappingParser parser = new UserRoleMappingParser();

    @Test
    @DisplayName("Parses contiguous rows for a user into one mapping")
    void parsesContiguousRowsForUser() throws Exception {

        // ARRANGE
        String csv = """
                email_address,business_unit_id,role_id
                user1@test.com,BU1,R1
                user1@test.com,BU1,R2
                user1@test.com,BU2,R3
                user2@test.com,BU1,R4
                user2@test.com,BU2,R5
                """;

        // ACT
        MappingFileProcessingResult result = parser.parse(new StringReader(csv));

        // ASSERT
        assertEquals(2, result.validUsers().size());
        assertTrue(result.invalidEmails().isEmpty());

        ParsedUserMapping user1 = result.validUsers().stream()
            .filter(u -> u.emailAddress().equals("user1@test.com"))
            .findFirst()
            .orElseThrow();

        assertEquals(Map.of(
            "BU1", Set.of("R1", "R2"),
            "BU2", Set.of("R3")
        ), user1.businessUnitToRoles());

        ParsedUserMapping user2 = result.validUsers().stream()
            .filter(u -> u.emailAddress().equals("user2@test.com"))
            .findFirst()
            .orElseThrow();

        assertEquals(Map.of(
            "BU1", Set.of("R4"),
            "BU2", Set.of("R5")
        ), user2.businessUnitToRoles());
    }

    @Test
    @DisplayName("Marks a user invalid when the same email appears again after another user")
    void marksUserInvalidWhenRowsAreNotContiguous() throws Exception {

        // ARRANGE
        String csv = """
                email_address,business_unit_id,role_id
                user1@test.com,BU1,R1
                user1@test.com,BU1,R2
                user2@test.com,BU2,R3
                user1@test.com,BU2,R4
                """;

        // ACT
        MappingFileProcessingResult result = parser.parse(new StringReader(csv));

        // ASSERT
        assertEquals(1, result.validUsers().size());
        assertEquals(Set.of("user1@test.com"), result.invalidEmails());

        ParsedUserMapping user2 = result.validUsers().stream()
            .filter(u -> u.emailAddress().equals("user2@test.com"))
            .findFirst()
            .orElseThrow();

        assertEquals(Map.of(
            "BU2", Set.of("R3")
        ), user2.businessUnitToRoles());

        assertTrue(result.validUsers().stream()
                       .noneMatch(u -> u.emailAddress().equals("user1@test.com")));
    }

    @Test
    @DisplayName("Normalizes email addresses and deduplicates role mappings")
    void normalizesEmailAndDeduplicatesRoles() throws Exception {

        // ARRANGE
        String csv = """
                email_address,business_unit_id,role_id
                USER1@Test.com,BU1,R1
                user1@test.com,BU1,R1
                user1@test.com,BU1,R2
                """;

        // ACT
        MappingFileProcessingResult result = parser.parse(new StringReader(csv));

        // ASSERT
        assertEquals(1, result.validUsers().size());
        assertTrue(result.invalidEmails().isEmpty());

        ParsedUserMapping user = result.validUsers().get(0);

        assertEquals("user1@test.com", user.emailAddress());
        assertEquals(Map.of(
            "BU1", Set.of("R1", "R2")
        ), user.businessUnitToRoles());
    }

    @Test
    @DisplayName("Skips rows with missing columns instead of throwing")
    void skipsRowsWithMissingColumns() throws Exception {

        // ARRANGE
        String csv = """
                email_address,business_unit_id,role_id
                user1@test.com,BU1
                user2@test.com,BU2,R2
                """;

        // ACT
        MappingFileProcessingResult result = parser.parse(new StringReader(csv));

        // ASSERT
        assertEquals(1, result.validUsers().size());

        ParsedUserMapping user2 = result.validUsers().get(0);

        assertEquals("user2@test.com", user2.emailAddress());
        assertEquals(Map.of(
            "BU2", Set.of("R2")
        ), user2.businessUnitToRoles());
    }

    @Test
    @DisplayName("Ignores rows for email already marked invalid even if contiguous afterwards")
    void ignoresRowsForEmailAlreadyMarkedInvalidEvenIfContiguousAfterwards() throws Exception {

        // ARRANGE
        String csv = """
                email_address,business_unit_id,role_id
                user1@test.com,BU1,R1
                user1@test.com,BU1,R2
                user2@test.com,BU2,R3
                user1@test.com,BU3,R4
                user1@test.com,BU3,R5
                """;

        Reader reader = new StringReader(csv);

        // ACT
        MappingFileProcessingResult result = parser.parse(reader);

        // ASSERT
        assertTrue(result.invalidEmails().contains("user1@test.com"));

        assertTrue(result.validUsers().stream()
                       .noneMatch(u -> u.emailAddress().equals("user1@test.com")));

        ParsedUserMapping user2 = result.validUsers().stream()
            .filter(u -> u.emailAddress().equals("user2@test.com"))
            .findFirst()
            .orElseThrow();

        assertEquals(
            Map.of("BU2", Set.of("R3")),
            user2.businessUnitToRoles()
        );
    }
}
