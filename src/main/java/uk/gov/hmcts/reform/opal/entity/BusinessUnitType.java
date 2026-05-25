package uk.gov.hmcts.reform.opal.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum BusinessUnitType {
    ACCOUNTING_DIVISION("Accounting Division"),
    AREA("Area");

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static BusinessUnitType fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (BusinessUnitType businessUnitType : values()) {
            if (businessUnitType.value.equals(value)) {
                return businessUnitType;
            }
        }

        throw new IllegalArgumentException("Unknown business unit type: " + value);
    }
}
