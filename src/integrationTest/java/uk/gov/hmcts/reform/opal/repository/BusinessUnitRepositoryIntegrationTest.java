package uk.gov.hmcts.reform.opal.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

@ActiveProfiles({"integration"})
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_CLASS)
@DisplayName("BusinessUnitRepository integration tests")
class BusinessUnitRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BusinessUnitRepository businessUnitRepository;

    @ParameterizedTest(name = "{1} -> {2}")
    @CsvSource(textBlock = """
        120, 'Area', AREA
        121, 'Accounting Division', ACCOUNTING_DIVISION
        """)
    @DisplayName("Should map persisted business unit type labels to enum values")
    void findById_mapsDatabaseLabelsToEnum(short businessUnitId, String databaseValue, BusinessUnitType expectedType) {
        insertBusinessUnit(businessUnitId, databaseValue);

        BusinessUnitEntity result = businessUnitRepository.findById(businessUnitId).orElseThrow();

        assertEquals(expectedType, result.getBusinessUnitType());
    }

    @ParameterizedTest(name = "{2} -> {3}")
    @CsvSource(textBlock = """
        122, 'Accounting Division', AREA, 'Area'
        123, 'Area', ACCOUNTING_DIVISION, 'Accounting Division'
        """)
    @DisplayName("Should persist enum values using the database enum labels")
    void save_persistsEnumValuesAsDatabaseEnumLabels(
        short businessUnitId, String initialDatabaseValue, BusinessUnitType updatedType, String expectedDatabaseValue) {
        insertBusinessUnit(businessUnitId, initialDatabaseValue);

        BusinessUnitEntity businessUnit = businessUnitRepository.findById(businessUnitId).orElseThrow();
        businessUnit.setBusinessUnitType(updatedType);

        businessUnitRepository.saveAndFlush(businessUnit);

        assertEquals(expectedDatabaseValue, getStoredBusinessUnitType(businessUnitId));
    }

    private void insertBusinessUnit(short businessUnitId, String databaseValue) {
        jdbcTemplate.update("DELETE FROM business_units WHERE business_unit_id = ?", businessUnitId);
        jdbcTemplate.update(
            """
                INSERT INTO business_units (
                    business_unit_id,
                    business_unit_name,
                    business_unit_code,
                    business_unit_type,
                    opal_domain_id
                ) VALUES (?, ?, ?, CAST(? AS t_business_unit_type_enum), ?)
                """,
            businessUnitId,
            "Integration BU " + businessUnitId,
            String.format("I%03d", businessUnitId % 1000),
            databaseValue,
            1
        );
    }

    private String getStoredBusinessUnitType(short businessUnitId) {
        return jdbcTemplate.queryForObject(
            "SELECT business_unit_type::text FROM business_units WHERE business_unit_id = ?",
            String.class,
            businessUnitId
        );
    }
}
