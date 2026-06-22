package uk.gov.hmcts.reform.opal.service.legacy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.common.legacy.service.GatewayService;
import uk.gov.hmcts.opal.common.legacy.service.GatewayService.Response;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyGetBusinessUnitUserIdsRequest;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyGetBusinessUnitUserIdsResponse;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "opal.LegacyBusinessUnitUserService")
public class LegacyBusinessUnitUserService {

    private static final String GET_BUSINESS_UNIT_USER_IDS = "getBusinessUnitUserIDs";

    private final GatewayService gatewayService;

    public Response<LegacyGetBusinessUnitUserIdsResponse> getBusinessUnitUserIds(List<String> libraUserIds) {
        return getBusinessUnitUserIds(LegacyGetBusinessUnitUserIdsRequest.builder()
                                          .libraUserIds(libraUserIds)
                                          .build());
    }

    public Response<LegacyGetBusinessUnitUserIdsResponse> getBusinessUnitUserIds(
        LegacyGetBusinessUnitUserIdsRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        log.debug(":getBusinessUnitUserIds: Requesting legacy business unit user ids.");

        Response<LegacyGetBusinessUnitUserIdsResponse> response = gatewayService
            .postToGateway(GET_BUSINESS_UNIT_USER_IDS, LegacyGetBusinessUnitUserIdsResponse.class, request, null);

        if (response.isError()) {
            log.error(
                ":getBusinessUnitUserIds: Legacy Gateway response: HTTP Response Code {}", response.code);

            if (response.isException()) {
                log.error(":getBusinessUnitUserIds: Exception", response.exception);
            } else if (response.isLegacyFailure()) {
                log.error(":getBusinessUnitUserIds: Legacy Failure: Body:\n{}", response.body);
            }
        } else if (response.isSuccessful()) {
            log.debug(":getBusinessUnitUserIds: Legacy Gateway response: Success.");
        }

        return response;
    }
}
