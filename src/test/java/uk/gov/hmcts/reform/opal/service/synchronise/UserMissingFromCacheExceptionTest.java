package uk.gov.hmcts.reform.opal.service.synchronise;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserMissingFromCacheExceptionTest {

    @Test
    void constructor_ShouldSetMessage() {
        // Arrange
        String message = "Nothing in cache for : test-subject";

        // Act
        UserMissingFromCacheException exception =
            new UserMissingFromCacheException(message);

        // Assert
        assertEquals(message, exception.getMessage());
    }
}
