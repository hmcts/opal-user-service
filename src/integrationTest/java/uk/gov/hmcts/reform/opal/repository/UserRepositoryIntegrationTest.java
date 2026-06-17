package uk.gov.hmcts.reform.opal.repository;

import tools.jackson.core.JacksonException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.reform.opal.BaseIntegrationTest;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.mappers.UserStateMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

@ActiveProfiles({"integration"})
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("UserRepository integration tests")
class UserRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserStateMapper mapper;

    Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-04-14T10:15:30Z"), ZoneId.of("UTC"));
    }

    @Test
    @DisplayName("Test UserStateV2Dto production in isolation")
    @Sql(
        scripts = "classpath:db.insertData/insert_authorisation_data_with_duplicate_buu.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    void testUserStateV2DtoProductionInIsolation() throws JacksonException {
        UserEntity user = userRepository.findIdWithPermissions(500000000L).orElseThrow();
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user, clock);
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(dto)))
            .isEqualTo(objectMapper.readTree(EXPECTED_V2_USER_STATE));
    }

    @Test
    @DisplayName("Test UserStateV2Dto production in isolation when user has no business units")
    void testUserStateV2DtoProductionInIsolationWhenUserHasNoBusinessUnitUsers() throws JacksonException {
        UserEntity user = userRepository.findIdWithPermissions(500000001L).orElseThrow();
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user, clock);
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(dto)))
            .isEqualTo(objectMapper.readTree("""
                 {
                     "user_id": 500000001,
                     "username": "opal-test-2@HMCTS.NET",
                     "name": "token name",
                     "status": "PENDING",
                     "version": 0,
                     "cache_name": null,
                     "domains": {}
                   }
             """));
    }

    private static final String EXPECTED_V2_USER_STATE =
        """
            {
              "user_id": 500000000,
              "username": "opal-test@HMCTS.NET",
              "name": "Pablo",
              "status": "PENDING",
              "version": 0,
              "cache_name": null,
              "domains": {
                "fines": {
                  "business_unit_users": [
                    {
                      "business_unit_user_id": "L066JG",
                      "business_unit_id": 68,
                      "permissions": []
                    },
                    {
                      "business_unit_user_id": "L067JG",
                      "business_unit_id": 73,
                      "permissions": []
                    },
                    {
                      "business_unit_user_id": "L073JG",
                      "business_unit_id": 71,
                      "permissions": []
                    },
                    {
                      "business_unit_user_id": "L077JG",
                      "business_unit_id": 67,
                      "permissions": []
                    },
                    {
                      "business_unit_user_id": "L078JG",
                      "business_unit_id": 69,
                      "permissions": []
                    },
                    {
                      "business_unit_user_id": "L080JG",
                      "business_unit_id": 61,
                      "permissions": []
                    }
                  ]
                }
              }
            }
        """;

}
