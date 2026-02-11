package uk.gov.hmcts.reform.opal.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.BusinessUnitUserDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.PermissionDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.reform.opal.dto.UserStateV1;
import uk.gov.hmcts.reform.opal.dto.UserStateV2;

class UserStateMapperTest {

    private final UserStateMapper mapper = new UserStateMapperImplementation();

    @Test
    @DisplayName("toV1 should map version and business unit users")
    void toV1_shouldMapVersionAndBusinessUnitUsers() {
        UserStateDto source = buildUserStateDto();

        UserStateV1 result = mapper.toV1(source);

        assertNotNull(result);
        assertEquals(123L, result.getUserId());
        assertEquals("opal-test@hmcts.net", result.getUsername());
        assertEquals("Test User", result.getName());
        assertEquals(BigInteger.valueOf(7L), result.getVersion());
        assertNotNull(result.getBusinessUnitUsers());
        assertEquals(1, result.getBusinessUnitUsers().size());
        assertEquals(70, result.getBusinessUnitUsers().getFirst().getBusinessUnitId());
        assertEquals(List.of("Account Enquiry", "Account Notes"), result.getBusinessUnitUsers().getFirst().getRoles());
    }

    @Test
    @DisplayName("toV2 should wrap business unit users under domains fines")
    void toV2_shouldWrapBusinessUnitUsersUnderDomainsFines() {
        UserStateDto source = buildUserStateDto();

        UserStateV2 result = mapper.toV2(source);

        assertNotNull(result);
        assertEquals(123L, result.getUserId());
        assertEquals("opal-test@hmcts.net", result.getUsername());
        assertEquals("opal-test@hmcts.net", result.getCacheName());
        assertEquals(UserStateV2.StatusEnum.ACTIVE, result.getStatus());
        assertEquals(BigInteger.valueOf(7L), result.getVersion());
        assertNotNull(result.getDomains());
        assertNotNull(result.getDomains().getFines());
        assertNotNull(result.getDomains().getFines().getBusinessUnitUsers());
        assertEquals(1, result.getDomains().getFines().getBusinessUnitUsers().size());
        assertEquals(70, result.getDomains().getFines().getBusinessUnitUsers().getFirst().getBusinessUnitId());
        assertEquals(
            List.of("Account Enquiry", "Account Notes"),
            result.getDomains().getFines().getBusinessUnitUsers().getFirst().getRoles()
        );
    }

    @Test
    @DisplayName("toPermissionName should return null for null permission")
    void toPermissionName_shouldReturnNullForNullPermission() {
        assertNull(mapper.toPermissionName(null));
    }

    private static UserStateDto buildUserStateDto() {
        PermissionDto accountEnquiry = new PermissionDto();
        accountEnquiry.setPermissionId(10L);
        accountEnquiry.setPermissionName("Account Enquiry");

        PermissionDto accountNotes = new PermissionDto();
        accountNotes.setPermissionId(11L);
        accountNotes.setPermissionName("Account Notes");

        BusinessUnitUserDto businessUnitUser = new BusinessUnitUserDto();
        businessUnitUser.setBusinessUnitId((short) 70);
        businessUnitUser.setPermissions(List.of(accountEnquiry, accountNotes));

        UserStateDto userStateDto = new UserStateDto();
        userStateDto.setUserId(123L);
        userStateDto.setUsername("opal-test@hmcts.net");
        userStateDto.setName("Test User");
        userStateDto.setStatus("ACTIVE");
        userStateDto.setVersion(7L);
        userStateDto.setBusinessUnitUsers(List.of(businessUnitUser));
        return userStateDto;
    }
}
