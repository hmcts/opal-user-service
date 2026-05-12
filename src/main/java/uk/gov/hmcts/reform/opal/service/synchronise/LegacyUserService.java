package uk.gov.hmcts.reform.opal.service.synchronise;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyBusinessUnitUser;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyBusinessUnitUsersRequest;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyBusinessUnitUsersResponse;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyGetUserRequest;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyGetUserResponse;

import java.util.List;

@Service
public class LegacyUserService {

    public LegacyGetUserResponse getUserIds(LegacyGetUserRequest requestDto) throws SynchronisePermissionsException {
        LegacyGetUserResponse responseDto = new LegacyGetUserResponse();
        responseDto.setLibraUserIds(List.of("123", "456"));
        responseDto.setCount(2);
        return responseDto;
    }

    public LegacyBusinessUnitUsersResponse getBusinessUnitUsers(LegacyBusinessUnitUsersRequest requestDto)
        throws SynchronisePermissionsException {
        LegacyBusinessUnitUsersResponse responseDto = new LegacyBusinessUnitUsersResponse();
        responseDto.setBusinessUnitUsers(List.of(
            LegacyBusinessUnitUser.builder().businessUnitId("111").businessUnitId("A").build(),
            LegacyBusinessUnitUser.builder().businessUnitId("222").businessUnitId("B").build()
            )
        );
        return responseDto;
    }
}
