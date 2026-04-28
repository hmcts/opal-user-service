package uk.gov.hmcts.reform.opal.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class UserEntityTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 10, 0);

    @ParameterizedTest(name = "{0}")
    @MethodSource("statusScenarios")
    void getStatusFromTime_returnsExpectedStatus(String scenario,
                                                 UserEntity user,
                                                 UserEntity.Status expectedStatus) {
        assertEquals(expectedStatus, user.getStatusFromTime(NOW));
    }

    private static Stream<Arguments> statusScenarios() {
        return Stream.of(
            arguments(
                "PO-2834 AC1: pending when activation date is missing",
                UserEntity.builder().build(),
                UserEntity.Status.PENDING
            ),
            arguments(
                "PO-2834 AC2: pending when activation date is in the future",
                UserEntity.builder()
                    .activationDate(NOW.plusDays(1))
                    .build(),
                UserEntity.Status.PENDING
            ),
            arguments(
                "PO-2834 AC3: active when activation date is today and no suspension or deactivation exists",
                UserEntity.builder()
                    .activationDate(NOW)
                    .build(),
                UserEntity.Status.ACTIVE
            ),
            arguments(
                "PO-2834 AC4: active when activation date is in the past and no suspension or deactivation exists",
                UserEntity.builder()
                    .activationDate(NOW.minusDays(1))
                    .build(),
                UserEntity.Status.ACTIVE
            ),
            arguments(
                "PO-2834 AC5: active when suspension start date is in the future",
                UserEntity.builder()
                    .activationDate(NOW.minusDays(1))
                    .suspensionStartDate(NOW.plusHours(1))
                    .build(),
                UserEntity.Status.ACTIVE
            ),
            arguments(
                "PO-2834 AC6: active when suspension period has fully ended",
                UserEntity.builder()
                    .activationDate(NOW.minusDays(1))
                    .suspensionStartDate(NOW.minusHours(2))
                    .suspensionEndDate(NOW.minusHours(1))
                    .build(),
                UserEntity.Status.ACTIVE
            ),
            arguments(
                "PO-2834 AC7: active when deactivation date is in the future",
                UserEntity.builder()
                    .activationDate(NOW.minusDays(1))
                    .deactivationDate(NOW.plusDays(1))
                    .build(),
                UserEntity.Status.ACTIVE
            ),
            arguments(
                "PO-2834 AC8: deactivated when deactivation date is today",
                UserEntity.builder()
                    .activationDate(NOW.minusDays(1))
                    .deactivationDate(NOW)
                    .build(),
                UserEntity.Status.DEACTIVATED
            ),
            arguments(
                "PO-2834 AC9: deactivated when deactivation date is in the past",
                UserEntity.builder()
                    .activationDate(NOW.minusDays(1))
                    .deactivationDate(NOW.minusMinutes(1))
                    .build(),
                UserEntity.Status.DEACTIVATED
            ),
            arguments(
                "PO-2834 AC10: suspended when suspension has started and has no end date",
                UserEntity.builder()
                    .activationDate(NOW.minusDays(1))
                    .suspensionStartDate(NOW.minusHours(1))
                    .build(),
                UserEntity.Status.SUSPENDED
            ),
            arguments(
                "PO-2834 AC11: suspended when suspension has started and ends in the future or now",
                UserEntity.builder()
                    .activationDate(NOW.minusDays(1))
                    .suspensionStartDate(NOW.minusHours(1))
                    .suspensionEndDate(NOW)
                    .build(),
                UserEntity.Status.SUSPENDED
            )
        );
    }
}
