package uk.gov.hmcts.reform.opal.mappers;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.BusinessUnitUserDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsers;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStatus;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;
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

    private final DomainEntity fines = DomainEntity.builder().name("fines").build();
    private final DomainEntity confiscations = DomainEntity.builder().name("confiscation").build();

    String permAE = Permissions.ACCOUNT_ENQUIRY.description;
    String permAEN = Permissions.ACCOUNT_ENQUIRY_NOTES.description;
    String permCVDA = Permissions.CHECK_VALIDATE_DRAFT_ACCOUNTS.description;
    String permCO = Permissions.COLLECTION_ORDER.description;
    String permSAVA = Permissions.SEARCH_AND_VIEW_ACCOUNTS.description;
    String permBadName = "BAD_NAME";

    private final LocalDateTime nowUtc = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);

    @Mock
    private UserEntity user;

    @BeforeEach
    void setUp() {
        when(user.getUserId()).thenReturn(123L);
        when(user.getTokenName()).thenReturn("token");
        when(user.getUsername()).thenReturn("username");
        when(user.getVersion()).thenReturn(BigInteger.valueOf(321L));
        when(user.getStatusFromTime(nowUtc)).thenReturn(UserStatus.ACTIVE);
    }

    @Test
    void toUserStateDto_mapsUserFieldsAndBusinessUnitUsers() {

        // Arrange
        BusinessUnitUserDto buu1 = mock(BusinessUnitUserDto.class);
        BusinessUnitUserDto buu2 = mock(BusinessUnitUserDto.class);
        List<BusinessUnitUserDto> businessUnitUsers = List.of(buu1, buu2);

        // Act
        UserStateDto dto = mapper.toUserStateDto(user, List.of(buu1, buu2), clock);

        // Assert
        assertThat(dto.getUserId()).isEqualTo(123L);
        assertThat(dto.getUsername()).isEqualTo("username");
        assertThat(dto.getName()).isEqualTo("token");
        assertThat(dto.getVersion()).isEqualTo(BigInteger.valueOf(321L));
        assertThat(dto.getStatus()).isEqualTo("active");
        assertThat(dto.getBusinessUnitUsers()).containsExactly(buu1, buu2);
    }

    @Test
    void toUserStateV2Dto() throws JacksonException {

        // Arrange
        RoleEntity role1 = buildRole("role1", List.of(permAE, permBadName, permAEN));
        RoleEntity role2 = buildRole("role2", List.of(permAE, permCVDA, permCO));
        RoleEntity role3 = buildRole("role3", List.of(permSAVA, permCO));
        RoleEntity role4 = buildRole("role4", List.of(permCO, permAEN));
        RoleEntity role5 = buildRole("role5", List.of(permSAVA, permAE));

        Set<BusinessUnitUserEntity> businessUnitUserEntityList = Set.of(
            buildBusinessUnitUserEntity("ABC123", fines, (short) 41, Set.of(role1, role2)),
            buildBusinessUnitUserEntity("DEF456", fines, (short) 42, Set.of(role3, role4)),
            buildBusinessUnitUserEntity("GHI789", confiscations, (short) 51, Set.of(role5))
        );

        when(user.getBusinessUnitUsers()).thenReturn(businessUnitUserEntityList);

        // Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user, clock);

        // Assert
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
                      "permission_name": "Search and view accounts"
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
                    },
                    {
                      "permission_id": 6,
                      "permission_name": "Search and view accounts"
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
    void toUserStateV2_populatesCacheName() {

        // Arrange
        when(user.getBusinessUnitUsers()).thenReturn(emptySet());

        // Act
        UserStateV2 result = mapper.toUserStateV2(user, clock);

        // Assert
        assertThat(result.getCacheName()).isNull();
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void toUserStateV2Dto_fromUserStateV2_mapsCacheNameAndDomains() throws JacksonException {

        // Arrange
        UserStateV2 userStateV2 = UserStateV2.builder()
            .userId(123L)
            .username("username")
            .name("token")
            .status(UserStatus.ACTIVE)
            .version(321L)
            .cacheName("USER_STATE_subject-123")
            .domains(java.util.Map.of(
                Domain.FINES, DomainBusinessUnitUsers.builder()
                    .businessUnitUsers(List.of(
                        BusinessUnitUser.builder()
                            .businessUnitUserId("ABC123")
                            .businessUnitId((short) 41)
                            .permissions(emptySet())
                            .build()
                    ))
                    .build()
            ))
            .build();

        // Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(userStateV2);

        // Assert
        assertThat(dto.getCacheName()).isNull();
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
    @MockitoSettings(strictness = Strictness.LENIENT)
    void toUserStateV2Dto_badFunctionNamesSkipped() throws JacksonException {

        // Arrange
        RoleEntity role = buildRole("role1", List.of(permAE, permBadName, permAEN));
        when(user.getBusinessUnitUsers()).thenReturn(Set.of(
            buildBusinessUnitUserEntity("ABC123", fines, (short) 41, Set.of(role))
        ));

        // Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user, clock);

        // Assert
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
                            {"permission_id": 2, "permission_name": "Account Enquiry - Account Notes"},
                            {"permission_id": 3, "permission_name": "Account Enquiry"}
                          ]
                        }
                      ]
                    }
                  }
                }
            """));
    }

    @Test
    void toUserStateV2_WhenBusinessUnitUsersNull() throws JacksonException {

        // Arrange
        when(user.getBusinessUnitUsers()).thenReturn(null);

        // Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user, clock);

        // Assert
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(dto)))
            .isEqualTo(objectMapper.readTree(expectedUserStateWithNoBusinessUnits()));
    }

    @Test
    void toUserStateV2_WhenBusinessUnitUsersEmpty() throws JacksonException {

        // Arrange
        when(user.getBusinessUnitUsers()).thenReturn(emptySet());

        // Act
        UserStateV2Dto dto = mapper.toUserStateV2Dto(user, clock);

        // Assert
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(dto)))
            .isEqualTo(objectMapper.readTree(expectedUserStateWithNoBusinessUnits()));
    }

    // ===== Helpers =====

    private RoleEntity buildRole(String name, List<String> permNames) {
        return RoleEntity.builder()
            .name(name)
            .applicationFunctionList(permNames)
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
