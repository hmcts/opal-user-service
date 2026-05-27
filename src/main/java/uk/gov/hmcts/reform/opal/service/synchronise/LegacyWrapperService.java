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

    private static final String SYNC_STAGE = "retrieve business unit users from Legacy API";

    private final LegacyBusinessUnitUserService legacyBusinessUnitUserService;
    private final LegacyUserService legacyUserService;

    public List<LegacyBusinessUnitUserId> getBusinessUnitUserIds(UserEntity user) {
        try {
            List<String> libraUserIds = fetchLibraUserIds(user);
            if (libraUserIds.isEmpty()) {
                return emptyList();
            }
            return fetchBusinessUnitUsers(user, libraUserIds);
        } catch (RuntimeException exception) {
            if (exception instanceof SynchronisePermissionsException synchronisePermissionsException) {
                throw synchronisePermissionsException;
            }
            throw new SynchronisePermissionsException(user, SYNC_STAGE, "unexpected runtime exception", exception);
        }
    }

    //1. Fetch Libra user id's from the legacy system
    private List<String> fetchLibraUserIds(UserEntity user) {
        GatewayService.Response<LegacyGetUserResponse> legacyGetUserGatewayResponse = legacyUserService.getUser(
            new LegacyGetUserRequest(user.getUsername()));
        LegacyGetUserResponse legacyGetUserResponse = requireSuccessfulResponse(
            user,
            legacyGetUserGatewayResponse,
            "GetSystemUserIdsByEmail"
        );
        log.debug("legacyGetUserResponse: {}", legacyGetUserResponse);
        return legacyGetUserResponse.getLibraUserIds() == null
            ? emptyList()
            : legacyGetUserResponse.getLibraUserIds();
    }

    //2. Fetch business unit user ids from the legacy system using Libra user ids
    private List<LegacyBusinessUnitUserId> fetchBusinessUnitUsers(UserEntity user, List<String> libraUserIds) {
        GatewayService.Response<LegacyGetBusinessUnitUserIdsResponse> legacyBusinessUnitUsersGatewayResponse =
            legacyBusinessUnitUserService.getBusinessUnitUserIds(libraUserIds);
        LegacyGetBusinessUnitUserIdsResponse legacyBusinessUnitUsersResponse = requireSuccessfulResponse(
            user,
            legacyBusinessUnitUsersGatewayResponse,
            "GetBUUserIdsBySystemUserIds"
        );
        log.debug("legacyBusinessUnitUsersResponse: {}", legacyBusinessUnitUsersResponse);
        return legacyBusinessUnitUsersResponse.getBusinessUnitUserIds() == null
            ? emptyList()
            : legacyBusinessUnitUsersResponse.getBusinessUnitUserIds();
    }

    private <T> T requireSuccessfulResponse(UserEntity user, GatewayService.Response<T> response, String operation) {
        if (response == null) {
            throw new SynchronisePermissionsException(user, SYNC_STAGE,
                                                      "legacy call returned null response: " + operation);
        }
        if (!response.isSuccessful() || response.responseEntity == null) {
            if (response.isException() && response.exception != null) {
                throw new SynchronisePermissionsException(user, SYNC_STAGE,
                                                          "legacy call failed: " + operation, response.exception);
            }
            throw new SynchronisePermissionsException(
                user, SYNC_STAGE,
                "legacy call failed: " + operation + " (httpCode=" + response.code + ")"
            );
        }
        return response.responseEntity;
    }

}
