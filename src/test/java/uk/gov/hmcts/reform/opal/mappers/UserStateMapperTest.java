package uk.gov.hmcts.reform.opal.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.BusinessUnitUserDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.reform.opal.authorisation.model.Permissions;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserRoleEntity;
import uk.gov.hmcts.reform.opal.entity.DomainEntity;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;

import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserStateMapperTest {

    private final UserStateMapper mapper = new UserStateMapperImplementation();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private final ObjectMapper objectMapper = new ObjectMapper();

    DomainEntity fines = DomainEntity.builder().name("fines").build();
    DomainEntity confiscations = DomainEntity.builder().name("confiscation").build();

    String permAE = Permissions.ACCOUNT_ENQUIRY.name();
    String permAEN = Permissions.ACCOUNT_ENQUIRY_NOTES.name();
    String permCVDA = Permissions.CHECK_VALIDATE_DRAFT_ACCOUNTS.name();
    String permCO = Permissions.COLLECTION_ORDER.name();
    String permSAVA = Permissions.SEARCH_AND_VIEW_ACCOUNTS.name();
    String permBadName = "BAD_NAME";

    LocalDateTime nowUtc = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);

    @Mock
    UserEntity user;

    @BeforeEach
    void setUp() {
        when(user.getUserId()).thenReturn(123L);
        when(user.getTokenName()).thenReturn("token");
        when(user.getUsername()).thenReturn("username");
        when(user.getVersion()).thenReturn(BigInteger.valueOf(321L));
        when(user.getStatusFromTime(nowUtc)).thenReturn(UserEntity.Status.ACTIVE);
    }

    @Test
    void toUserStateDto_mapsUserFieldsAndBusinessUnitUsers() {
        // Arrange
        BusinessUnitUserDto buu1 = mock(BusinessUnitUserDto.class);
        BusinessUnitUserDto buu2 = mock(BusinessUnitUserDto.class);
        List<BusinessUnitUserDto> businessUnitUsers = List.of(buu1, buu2);

        // Act
        UserStateDto dto = mapper.toUserStateDto(user, businessUnitUsers, clock);

        // Assert
        assertThat(dto.getUserId()).isEqualTo(123L);
        assertThat(dto.getUsername()).isEqualTo("username");
        assertThat(dto.getName()).isEqualTo("token");
        assertThat(dto.getVersion()).isEqualTo(BigInteger.valueOf(321L));
        assertThat(dto.getStatus()).isEqualTo("active");
        assertThat(dto.getBusinessUnitUsers()).containsExactly(buu1, buu2);
    }

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

        when(user.getBusinessUnitUsers()).thenReturn(businessUnitUserEntityList);

        //Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user, clock);

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

        when(user.getBusinessUnitUsers()).thenReturn(Set.of(businessUnitUserEntity));

        //Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user, clock);

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

        when(user.getBusinessUnitUsers()).thenReturn(Set.of(businessUnitUserEntity));

        //Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user, clock);

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
        when(user.getBusinessUnitUsers()).thenReturn(null);;

        //Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user, clock);

        //Assert
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(dto)))
            .isEqualTo(objectMapper.readTree(expectedUserStateWithNoBusinessUnits()));
    }

    @Test
    void toUserStateV2_WhenBusinessUnitUsersEmpty() throws JsonProcessingException {

        //Arrange
        when(user.getBusinessUnitUsers()).thenReturn(emptySet());

        //Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user, clock);

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
        when(user.getBusinessUnitUsers()).thenReturn(Set.of(buu));

        //Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user, clock);

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
        when(user.getBusinessUnitUsers()).thenReturn(businessUnitUserEntityList);

        //Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user, clock);

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

        when(user.getBusinessUnitUsers()).thenReturn(Set.of(buu));

        //Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user, clock);

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
        when(user.getBusinessUnitUsers()).thenReturn(businessUnitUserEntityList);

        //Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user, clock);

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
