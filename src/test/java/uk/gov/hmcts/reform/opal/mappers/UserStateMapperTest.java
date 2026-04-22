package uk.gov.hmcts.reform.opal.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.reform.opal.authorisation.model.Permissions;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserRoleEntity;
import uk.gov.hmcts.reform.opal.entity.DomainEntity;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

class UserStateMapperTest {

    private final UserStateMapper mapper = new UserStateMapperImplementation();

    private final ObjectMapper objectMapper = new ObjectMapper();

    DomainEntity fines = DomainEntity.builder().name("fines").build();
    DomainEntity confiscations = DomainEntity.builder().name("confiscation").build();

    String permAE = Permissions.ACCOUNT_ENQUIRY.name();
    String permAEN = Permissions.ACCOUNT_ENQUIRY_NOTES.name();
    String permCVDA = Permissions.CHECK_VALIDATE_DRAFT_ACCOUNTS.name();
    String permCO = Permissions.COLLECTION_ORDER.name();
    String permSAVA = Permissions.SEARCH_AND_VIEW_ACCOUNTS.name();
    String permBadName = "BAD_NAME";

    @Test
    void toUserStateV2Dto() throws JsonProcessingException {

        //Arrange
        RoleEntity role1 = buildRole("role1", List.of(permAE, permBadName, permAEN), true);
        RoleEntity role2 = buildRole("role2", List.of(permAE, permCVDA, permCO), true);
        RoleEntity role3inactive = buildRole("role3inactive", List.of(permSAVA, permCO), false);
        RoleEntity role4 = buildRole("role4", List.of(permCO, permAEN), true);
        RoleEntity role5 = buildRole("role5", List.of(permSAVA, permAE), true);

        Set<BusinessUnitUserEntity> businessUnitUserEntityList = Set.of(
            buildBusinessUnitUserEntity("ABC123", fines, (short) 41, Set.of(role1, role2)),
            buildBusinessUnitUserEntity("DEF456", fines, (short) 42, Set.of(role3inactive, role4)),
            buildBusinessUnitUserEntity("GHI789", confiscations, (short) 51, Set.of(role5))
        );

        UserEntity user = buildUserEntity(businessUnitUserEntityList);

        //Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user);

        //Assert
        String expected = """
            {
              "user_id": 123,
              "username": "username",
              "name": "token",
              "status": "ACTIVE",
              "version": 321,
              "cache_name": null,
              "domains": {
                "confiscation": {
                  "business_unit_users": [
                    {
                      "business_unit_user_id": "GHI789",
                      "business_unit_id": 51,
                      "permissions": [
                        {
                          "permission_id": 3,
                          "permission_name": "Account Enquiry"
                        },
                        {
                          "permission_id": 6,
                          "permission_name": "Search and View Accounts"
                        }
                      ]
                    }
                  ]
                },
                "fines": {
                  "business_unit_users": [
                    {
                      "business_unit_user_id": "ABC123",
                      "business_unit_id": 41,
                      "permissions": [
                        {
                          "permission_id": 2,
                          "permission_name": "Account Enquiry - Account Notes"
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
                        }
                      ]
                    },
                    {
                      "business_unit_user_id": "DEF456",
                      "business_unit_id": 42,
                      "permissions": [
                        {
                          "permission_id": 2,
                          "permission_name": "Account Enquiry - Account Notes"
                        },
                        {
                          "permission_id": 4,
                          "permission_name": "Collection Order"
                        }
                      ]
                    }
                  ]
                }
              }
            }
            """;

        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(dto)))
            .isEqualTo(objectMapper.readTree(expected));
        assertThat(expected).doesNotContain(permBadName);

    }

    @Test
    void toUserStateV2Dto_badFunctionNamesSkipped() throws JsonProcessingException {

        //Arrange
        RoleEntity role1 = buildRole("role1", List.of(permAE, permBadName, permAEN), true);

        BusinessUnitUserEntity businessUnitUserEntity =
            buildBusinessUnitUserEntity("ABC123", fines, (short) 41, Set.of(role1));

        UserEntity user = buildUserEntity(Set.of(businessUnitUserEntity));

        //Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user);

        //Assert
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(dto)))
            .isEqualTo(objectMapper.readTree("""
                 {
                   "user_id": 123,
                   "username": "username",
                   "name": "token",
                   "status": "ACTIVE",
                   "version": 321,
                   "cache_name": null,
                   "domains": {
                     "fines": {
                       "business_unit_users": [
                         {
                           "business_unit_user_id": "ABC123",
                           "business_unit_id": 41,
                           "permissions": [
                             {
                               "permission_id": 2,
                               "permission_name": "Account Enquiry - Account Notes"
                             },
                             {
                               "permission_id": 3,
                               "permission_name": "Account Enquiry"
                             }
                           ]
                         }
                       ]
                     }
                   }
                 }
                 """));
    }

    @Test
    void toUserStateV2Dto_WhenAllRolesInactive() throws JsonProcessingException {

        //Arrange
        RoleEntity role1 = buildRole("role1", List.of(permAE, permAEN), false);
        RoleEntity role2 = buildRole("role2", List.of(permAE, permCVDA), false);

        BusinessUnitUserEntity businessUnitUserEntity =
            buildBusinessUnitUserEntity("ABC123", fines, (short) 41, Set.of(role1, role2));

        UserEntity user = buildUserEntity(Set.of(businessUnitUserEntity));

        //Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user);

        //Assert
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(dto)))
            .isEqualTo(objectMapper.readTree("""
                {
                  "user_id": 123,
                  "username": "username",
                  "name": "token",
                  "status": "ACTIVE",
                  "version": 321,
                  "cache_name": null,
                  "domains": {
                    "fines": {
                      "business_unit_users": [
                        {
                          "business_unit_user_id": "ABC123",
                          "business_unit_id": 41,
                          "permissions": []
                        }
                      ]
                    }
                  }
                }
            """));
    }

    @Test
    void toUserStateV2_WhenBusinessUnitUsersNull() throws JsonProcessingException {

        //Arrange
        UserEntity user = buildUserEntity(null);

        //Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user);

        //Assert
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(dto)))
            .isEqualTo(objectMapper.readTree(expectedUserStateWithNoBusinessUnits()));
    }

    @Test
    void toUserStateV2_WhenBusinessUnitUsersEmpty() throws JsonProcessingException {

        //Arrange
        UserEntity user = buildUserEntity(emptySet());

        //Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user);

        //Assert
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(dto)))
            .isEqualTo(objectMapper.readTree(expectedUserStateWithNoBusinessUnits()));
    }

    @Test
    void toUserStateV2_WhenBusinessUnitUserHasNullBusinessUnitUserRoleList() throws JsonProcessingException {

        //Arrange
        BusinessUnitUserEntity buu = BusinessUnitUserEntity.builder()
            .businessUnit(BusinessUnitEntity.builder().domain(fines).businessUnitId((short)4).build())
            .businessUnitUserRoleList(null)
            .build();
        UserEntity user = buildUserEntity(Set.of(buu));

        //Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user);

        //Assert
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(dto)))
            .isEqualTo(objectMapper.readTree("""
                {
                  "user_id": 123,
                  "username": "username",
                  "name": "token",
                  "status": "ACTIVE",
                  "version": 321,
                  "cache_name": null,
                  "domains": {
                    "fines": {
                      "business_unit_users": [
                        {
                          "business_unit_user_id": null,
                          "business_unit_id": 4,
                          "permissions": []
                        }
                      ]
                    }
                  }
                }
            """));
    }

    @Test
    void toUserStateV2_WhenBusinessUnitUserHasEmptyBusinessUnitUserRoleList() throws JsonProcessingException {

        //Arrange
        Set<BusinessUnitUserEntity> businessUnitUserEntityList = Set.of(
            buildBusinessUnitUserEntity("ABC123", fines, (short) 41, Set.of())
        );
        UserEntity user = buildUserEntity(businessUnitUserEntityList);

        //Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user);

        //Assert
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(dto)))
            .isEqualTo(objectMapper.readTree("""
                     {
                       "user_id": 123,
                       "username": "username",
                       "name": "token",
                       "status": "ACTIVE",
                       "version": 321,
                       "cache_name": null,
                       "domains": {
                         "fines": {
                           "business_unit_users": [
                             {
                               "business_unit_user_id": "ABC123",
                               "business_unit_id": 41,
                               "permissions": []
                             }
                           ]
                         }
                       }
                     }
            """));
    }

    @Test
    void toUserStateV2_WhenBusinessUnitOfBusinessUnitUserEntityIsNull() throws JsonProcessingException {

        //Arrange
        BusinessUnitUserEntity buu = BusinessUnitUserEntity.builder()
            .businessUnitUserRoleList(emptySet())
            .businessUnit(null)
            .build();

        UserEntity user = buildUserEntity(Set.of(buu));

        //Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user);

        //Assert
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(dto)))
            .isEqualTo(objectMapper.readTree("""
                 {
                     "user_id": 123,
                     "username": "username",
                     "name": "token",
                     "status": "ACTIVE",
                     "version": 321,
                     "cache_name": null,
                     "domains": {}
                 }
             """));
    }

    @Test
    void toUserStateV2_WhenDomainNull() throws JsonProcessingException {

        //Arrange
        Set<BusinessUnitUserEntity> businessUnitUserEntityList = Set.of(
            buildBusinessUnitUserEntity("ABC123", null, (short)41, Set.of())
        );
        UserEntity user = buildUserEntity(businessUnitUserEntityList);

        //Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user);

        //Assert
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(dto)))
            .isEqualTo(objectMapper.readTree(expectedUserStateWithNoBusinessUnits()));
    }

    private RoleEntity buildRole(String name, List<String> permNames, boolean active) {
        return RoleEntity.builder()
            .name(name)
            .applicationFunctionList(permNames)
            .isActive(active)
            .build();
    }

    private BusinessUnitUserEntity buildBusinessUnitUserEntity(String id,
                                                               DomainEntity domain,
                                                               Short businessUnitId,
                                                               Set<RoleEntity> roles) {
        BusinessUnitEntity bu = BusinessUnitEntity.builder().domain(domain).businessUnitId(businessUnitId).build();
        BusinessUnitUserEntity buu = BusinessUnitUserEntity.builder()
            .businessUnitUserId(id)
            .businessUnit(bu)
            .build();
        for (RoleEntity role : roles) {
            buu.getBusinessUnitUserRoleList().add(
                BusinessUnitUserRoleEntity.builder().role(role).businessUnitUser(buu).build()
            );
        }
        return buu;
    }

    private UserEntity buildUserEntity(Set<BusinessUnitUserEntity> businessUnitUserEntityList) {
        return UserEntity.builder()
            .userId(123L)
            .tokenName("token")
            .tokenSubject("subject")
            .username("username")
            .businessUnitUsers(businessUnitUserEntityList)
            .versionNumber(321L)
            .build();
    }

    private String expectedUserStateWithNoBusinessUnits() {
        return """
            {
              "user_id": 123,
              "username": "username",
              "name": "token",
              "status": "ACTIVE",
              "version": 321,
              "cache_name": null,
              "domains": {}
            }
            """;
    }
}
