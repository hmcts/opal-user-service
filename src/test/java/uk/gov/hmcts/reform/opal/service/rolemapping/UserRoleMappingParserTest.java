package uk.gov.hmcts.reform.opal.service.rolemapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        String csv = """
                email_address,business_unit_id,role_id
                user1@test.com,BU1,R1
                user1@test.com,BU1,R2
                user1@test.com,BU2,R3
                user2@test.com,BU1,R4
                user2@test.com,BU2,R5
                """;

        ParseResult result = parser.parse(new StringReader(csv));

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
        String csv = """
                email_address,business_unit_id,role_id
                user1@test.com,BU1,R1
                user1@test.com,BU1,R2
                user2@test.com,BU2,R3
                user1@test.com,BU2,R4
                """;

        ParseResult result = parser.parse(new StringReader(csv));

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
        String csv = """
                email_address,business_unit_id,role_id
                USER1@Test.com,BU1,R1
                user1@test.com,BU1,R1
                user1@test.com,BU1,R2
                """;

        ParseResult result = parser.parse(new StringReader(csv));

        assertEquals(1, result.validUsers().size());
        assertTrue(result.invalidEmails().isEmpty());

        ParsedUserMapping user = result.validUsers().get(0);

        assertEquals("user1@test.com", user.emailAddress());
        assertEquals(Map.of(
            "BU1", Set.of("R1", "R2")
        ), user.businessUnitToRoles());
    }

    @Test
    @DisplayName("Rejects rows with missing required columns")
    void rejectsRowsWithMissingColumns() {
        String csv = """
                email_address,business_unit_id,role_id
                user1@test.com,BU1
                """;

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> parser.parse(new StringReader(csv))
        );

        assertTrue(ex.getMessage().contains("Invalid CSV row"));
    }
}
