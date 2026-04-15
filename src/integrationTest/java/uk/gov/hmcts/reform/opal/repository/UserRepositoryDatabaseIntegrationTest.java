package uk.gov.hmcts.reform.opal.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.reform.opal.BaseIntegrationTest;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.mappers.UserStateMapper;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

@ActiveProfiles({"integration"})
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:db.insertData/insert_user_state_data.sql", executionPhase = BEFORE_TEST_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("UserRepository database integration tests")
class UserRepositoryDatabaseIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserStateMapper mapper;

    @Test
    @DisplayName("Test UserStateV2Dto production in isolation")
    void testUserStateV2DtoProductionInIsolation() throws JsonProcessingException {
        UserEntity user = userRepository.findIdWithPermissions(500000000L).orElseThrow();
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user);
        ObjectMapper objectMapper = new ObjectMapper();
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(dto)))
            .isEqualTo(objectMapper.readTree(EXPECTED_V2_USER_STATE));
    }

    @Test
    @DisplayName("Test UserStateV2Dto production in isolation when user has no business units")
    void testUserStateV2DtoProductionInIsolationWhenUserHasNoBusinessUnitUsers() throws JsonProcessingException {
        UserEntity user = userRepository.findIdWithPermissions(500000001L).orElseThrow();
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user);
        ObjectMapper objectMapper = new ObjectMapper();
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(dto)))
            .isEqualTo(objectMapper.readTree("""
                 {
                     "user_id": 500000001,
                     "username": "opal-test-2@HMCTS.NET",
                     "name": null,
                     "status": "ACTIVE",
                     "version": 0,
                     "cache_name": null,
                     "domains": {}
                   }
             """));
    }

    @Test
    @DisplayName("Test roles view only brings back latest versions")
    void testRolesViewOnlyBringsBackLatestVersions() {

        RoleEntity finesRole1 = findRoleByName(1L, "Fines_Role_1");
        RoleEntity finesRole2 = findRoleByName(1L, "Fines_Role_2");
        RoleEntity confiscationRole3 = findRoleByName(2L, "Confiscation_Role_3");

        assertThat(finesRole1.getVersionNumber()).isEqualTo(2);
        assertThat(finesRole1.getApplicationFunctionList()).containsExactly(
            "CREATE_MANAGE_DRAFT_ACCOUNTS", "ACCOUNT_ENQUIRY");
        assertThat(finesRole2.getVersionNumber()).isEqualTo(3);
        assertThat(finesRole2.getApplicationFunctionList()).containsExactly(
            "COLLECTION_ORDER", "CHECK_VALIDATE_DRAFT_ACCOUNTS", "SEARCH_AND_VIEW_ACCOUNTS");
        assertThat(confiscationRole3.getVersionNumber()).isEqualTo(2);
        assertThat(confiscationRole3.getApplicationFunctionList()).containsExactly(
            "CREATE_MANAGE_DRAFT_ACCOUNTS", "COLLECTION_ORDER");

    }

    private RoleEntity findRoleByName(Long domainId, String roleName) {
        Set<RoleEntity> matchingRoles = roleRepository.findAllByDomainId(domainId)
            .stream()
            .filter(role -> role.getName().equals(roleName))
            .collect(Collectors.toSet());
        assertThat(matchingRoles).hasSize(1);
        return matchingRoles.iterator().next();
    }

    public static final String EXPECTED_V2_USER_STATE =
        """
            {
              "user_id": 500000000,
              "username": "opal-test@HMCTS.NET",
              "name": "Pablo",
              "status": "ACTIVE",
              "version": 0,
              "cache_name": null,
              "domains": {
                "fines": {
                  "business_unit_users": [
                    {
                      "business_unit_user_id": "L065JG",
                      "business_unit_id": 70,
                      "permissions": [
                        {
                          "permission_id": 1,
                          "permission_name": "Create and Manage Draft Accounts"
                        },
                        {
                          "permission_id": 3,
                          "permission_name": "Account Enquiry"
                        },
                        {
                          "permission_id": 4,
                          "permission_name": "Collection Order"
                        },
                        {
                          "permission_id": 5,
                          "permission_name": "Check and Validate Draft Accounts"
                        },
                        {
                          "permission_id": 6,
                          "permission_name": "Search and View Accounts"
                        }
                      ]
                    },
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
