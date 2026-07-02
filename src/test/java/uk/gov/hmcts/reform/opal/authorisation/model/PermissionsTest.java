package uk.gov.hmcts.reform.opal.authorisation.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PermissionsTest {

    @Test
    void toPermissionOrNull_returnsPermissionForValidDescription() {
        Permissions permission = Permissions.toPermissionOrNull(Permissions.ACCOUNT_ENQUIRY.description);
        assertEquals(Permissions.ACCOUNT_ENQUIRY, permission);
    }

    @Test
    void toPermissionOrNull_returnsProcessAndAllocatePaymentsPermission() {
        Permissions permission = Permissions.toPermissionOrNull("Process and Allocate Payments");
        assertEquals(Permissions.PROCESS_AND_ALLOCATE_PAYMENTS, permission);
    }

    @Test
    void toPermissionOrNull_returnsNullForInvalidDescription() {
        Permissions permission = Permissions.toPermissionOrNull("NOT_A_PERMISSION");
        assertNull(permission);
    }
}
