package uk.gov.hmcts.reform.opal.authorisation.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PermissionsTest {

    @Test
    void toPermissionOrNull_returnsPermissionForValidFunctionName() {
        Permissions permission = Permissions.toPermissionOrNull("ACCOUNT_ENQUIRY");
        assertEquals(Permissions.ACCOUNT_ENQUIRY, permission);
    }

    @Test
    void toPermissionOrNull_returnsNullForInvalidFunctionName() {
        Permissions permission = Permissions.toPermissionOrNull("NOT_A_PERMISSION");
        assertNull(permission);
    }
}
