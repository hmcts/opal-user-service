package uk.gov.hmcts.reform.opal.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class BusinessUnitTypeConverter implements AttributeConverter<BusinessUnitType, String> {

    @Override
    public String convertToDatabaseColumn(BusinessUnitType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public BusinessUnitType convertToEntityAttribute(String dbData) {
        return BusinessUnitType.fromValue(dbData);
    }
}
