package uk.gov.hmcts.reform.opal.entity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.opal.dto.businessevent.BusinessEvent;

class BusinessEventLogTypeTest {

    @ParameterizedTest
    @MethodSource("businessEventLogTypes")
    void validateEventDetails_acceptsMatchingEventType(BusinessEventLogType businessEventLogType) {
        BusinessEvent matchingEvent = instantiate(businessEventLogType.getEventDtoClass());

        assertDoesNotThrow(() -> businessEventLogType.validateEventDetails(matchingEvent));
    }

    @ParameterizedTest
    @MethodSource("businessEventLogTypes")
    void validateEventDetails_throwsWhenEventTypeDoesNotMatch(BusinessEventLogType businessEventLogType) {
        BusinessEventLogType wrongBusinessEventLogType = Arrays.stream(BusinessEventLogType.values())
            .filter(candidate -> candidate != businessEventLogType)
            .findFirst()
            .orElseThrow();
        BusinessEvent wrongEvent = instantiate(wrongBusinessEventLogType.getEventDtoClass());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> businessEventLogType.validateEventDetails(wrongEvent));

        assertEquals(
            "eventDetails must be of type " + businessEventLogType.getEventDtoClass().getSimpleName()
                + " for event type " + businessEventLogType.name(),
            exception.getMessage());
    }

    private static Stream<BusinessEventLogType> businessEventLogTypes() {
        return Arrays.stream(BusinessEventLogType.values());
    }

    private static BusinessEvent instantiate(Class<? extends BusinessEvent> eventClass) {
        try {
            return eventClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                 | NoSuchMethodException exception) {
            throw new IllegalStateException("Failed to instantiate " + eventClass.getSimpleName(), exception);
        }
    }
}
