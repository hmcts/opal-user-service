package uk.gov.hmcts.reform.opal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import uk.gov.hmcts.opal.common.dto.Versioned;
import uk.gov.hmcts.reform.opal.exception.ResourceConflictException;

public class VersionUtilsTest {

    @Test
    void testVerifyVersions_success() {
        Versioned entity = new UtilVersioned(1);
        Versioned dto = new UtilVersioned(1);
        VersionUtils.verifyVersions(entity, dto, "test id", "testVerifyVersions_success");
    }

    @Test
    void testVerifyVersions_fail() {
        Versioned entity = new UtilVersioned(1);
        Versioned dto = new UtilVersioned(2);

        ObjectOptimisticLockingFailureException rte = assertThrows(ObjectOptimisticLockingFailureException.class, () ->
            VersionUtils.verifyVersions(entity, dto, "test id", "testVerifyVersions_fail")
        );

        assertEquals("uk.gov.hmcts.reform.opal.util.VersionUtilsTest$UtilVersioned",
                     rte.getPersistentClassName());
        assertEquals("test id", rte.getIdentifier());
        assertEquals(":testVerifyVersions_fail: Versions do not match for: UtilVersioned 'test id'; "
                         + "DB version: 1, supplied update version: 2", rte.getMessage());
    }

    @Test
    void testVerifyUpdated_success() {
        Versioned entity = new UtilVersioned(2);
        Versioned dto = new UtilVersioned(1);
        VersionUtils.verifyUpdated(entity, dto, "test id", "testVerifyVersions_success");
    }

    @Test
    void testVerifyUpdated_fail() {
        Versioned entity = new UtilVersioned(2);
        Versioned dto = new UtilVersioned(2);

        ResourceConflictException rte = assertThrows(ResourceConflictException.class, () ->
            VersionUtils.verifyUpdated(entity, dto, "test id", "testVerifyVersions_fail")
        );

        assertEquals("UtilVersioned", rte.getResourceType());
        assertEquals("test id", rte.getResourceId());
        assertEquals("No differences detected between DB state and requested update.",
                     rte.getConflictReason());
    }

    private class UtilVersioned implements Versioned {

        private final BigInteger version;

        public UtilVersioned(int v) {
            this.version = new BigInteger(String.valueOf(v));
        }

        @Override
        public BigInteger getVersion() {
            return version;
        }
    }
}
