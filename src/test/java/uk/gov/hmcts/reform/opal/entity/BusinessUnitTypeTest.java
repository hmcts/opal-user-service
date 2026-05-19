package uk.gov.hmcts.reform.opal.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BusinessUnitTypeTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("fromValueScenarios")
    void fromValue_returnsExpectedEnum(String scenario, String candidate, BusinessUnitType expectedType) {
        assertEquals(expectedType, BusinessUnitType.fromValue(candidate));
    }

    @Test
    void fromValue_returnsNull_whenCandidateIsNull() {
        assertNull(BusinessUnitType.fromValue(null));
    }

    @Test
    void fromValue_throwsForUnknownValue() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> BusinessUnitType.fromValue("TEST")
        );

        assertEquals("Unknown business unit type: TEST", exception.getMessage());
    }

    @Test
    void fromValue_throwsForWrongCase() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> BusinessUnitType.fromValue("area")
        );

        assertEquals("Unknown business unit type: area", exception.getMessage());
    }

    @Test
    void fromValue_throwsForEnumName() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> BusinessUnitType.fromValue("ACCOUNTING_DIVISION")
        );

        assertEquals("Unknown business unit type: ACCOUNTING_DIVISION", exception.getMessage());
    }

    private static Stream<Arguments> fromValueScenarios() {
        return Stream.of(
            arguments("maps Accounting Division label", "Accounting Division",
                      BusinessUnitType.ACCOUNTING_DIVISION),
            arguments("maps Area label", "Area", BusinessUnitType.AREA)
        );
    }
}
