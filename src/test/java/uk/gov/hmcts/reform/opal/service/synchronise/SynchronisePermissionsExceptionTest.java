package uk.gov.hmcts.reform.opal.service.synchronise;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.opal.entity.UserEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SynchronisePermissionsExceptionTest {

    @Test
    void constructorWithoutCause_ShouldBuildMessageWithUserId() {
        // Arrange
        UserEntity user = UserEntity.builder().userId(123L).build();

        // Act
        SynchronisePermissionsException exception =
            new SynchronisePermissionsException(user, "sync roles", "legacy payload invalid");

        // Assert
        assertEquals(
            "Could not synchronise permissions for user 123 at stage: sync roles. Reason: legacy payload invalid",
            exception.getMessage()
        );
    }

    @Test
    void constructorWithCause_ShouldBuildMessageAndSetCause() {
        // Arrange
        UserEntity user = UserEntity.builder().userId(321L).build();
        RuntimeException cause = new RuntimeException("db boom");

        // Act
        SynchronisePermissionsException exception =
            new SynchronisePermissionsException(user, "sync business units", "unexpected runtime exception", cause);

        // Assert
        assertEquals(
            "Could not synchronise permissions for user 321 at stage: sync business units. "
                + "Reason: unexpected runtime exception",
            exception.getMessage()
        );
        assertSame(cause, exception.getCause());
    }

    @Test
    void constructor_ShouldUseUnknownWhenUserIsNull() {
        // Arrange
        // no arranged user required

        // Act
        SynchronisePermissionsException exception =
            new SynchronisePermissionsException(null, "sync cache", "user missing from cache");

        // Assert
        assertEquals(
            "Could not synchronise permissions for user unknown at stage: sync cache. Reason: user missing from cache",
            exception.getMessage()
        );
    }
}
