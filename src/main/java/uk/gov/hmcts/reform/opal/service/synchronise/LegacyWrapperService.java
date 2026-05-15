package uk.gov.hmcts.reform.opal.service.synchronise;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.common.legacy.service.GatewayService;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyBusinessUnitUserId;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyGetBusinessUnitUserIdsResponse;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyGetUserRequest;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyGetUserResponse;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.service.legacy.LegacyBusinessUnitUserService;
import uk.gov.hmcts.reform.opal.service.legacy.LegacyUserService;

import java.util.List;

import static java.util.Collections.emptyList;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "opal.LegacyUserWrapperService")
public class LegacyWrapperService {

    private final LegacyBusinessUnitUserService legacyBusinessUnitUserService;
    private final LegacyUserService legacyUserService;

    public List<LegacyBusinessUnitUserId> getBusinessUnitUserIds(UserEntity user) {
        //1. Fetch Libra user id's  from the legacy system
        GatewayService.Response<LegacyGetUserResponse> legacyGetUserGatewayResponse = legacyUserService.getUser(
            new LegacyGetUserRequest(user.getUsername()));
        LegacyGetUserResponse legacyGetUserResponse = requireSuccessfulResponse(
            legacyGetUserGatewayResponse,
            "GetSystemUserIdsByEmail"
        );
        List<String> libraUserIds = legacyGetUserResponse.getLibraUserIds() == null
            ? emptyList()
            : legacyGetUserResponse.getLibraUserIds();
        log.debug("legacyGetUserResponse: {}", legacyGetUserResponse);

        List<LegacyBusinessUnitUserId> legacyBuuList = emptyList();
        if (!libraUserIds.isEmpty()) {

            //2. Fetch Business unit user id's and business unit id's from the legacy system (PO-3442)
            //   using the Libra User id's returned previously
            GatewayService.Response<LegacyGetBusinessUnitUserIdsResponse> legacyBusinessUnitUsersGatewayResponse =
                legacyBusinessUnitUserService.getBusinessUnitUserIds(libraUserIds);
            LegacyGetBusinessUnitUserIdsResponse legacyBusinessUnitUsersResponse = requireSuccessfulResponse(
                legacyBusinessUnitUsersGatewayResponse,
                "GetBUUserIdsBySystemUserIds"
            );
            legacyBuuList = legacyBusinessUnitUsersResponse.getBusinessUnitUserIds() == null
                ? emptyList()
                : legacyBusinessUnitUsersResponse.getBusinessUnitUserIds();
            log.debug("legacyBusinessUnitUsersResponse: {}", legacyBusinessUnitUsersResponse);
        }
        return legacyBuuList;
    }

    private <T> T requireSuccessfulResponse(GatewayService.Response<T> response, String operation) {
        if (response == null) {
            throw new SynchronisePermissionsException("Legacy call returned null response: " + operation);
        }
        if (!response.isSuccessful() || response.responseEntity == null) {
            if (response.isException() && response.exception != null) {
                throw new SynchronisePermissionsException("Legacy call failed: " + operation, response.exception);
            }
            throw new SynchronisePermissionsException(
                "Legacy call failed: " + operation + " (httpCode=" + response.code + ")"
            );
        }
        return response.responseEntity;
    }

}
