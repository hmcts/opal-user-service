package uk.gov.hmcts.reform.opal.service.legacy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.common.launchdarkly.service.FeatureToggleApi;
import uk.gov.hmcts.opal.common.legacy.service.GatewayService;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyGetUserRequest;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyGetUserResponse;

import java.util.Locale;
import java.util.Objects;
import uk.gov.hmcts.reform.opal.util.FeatureFlags;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "opal.LegacyUserService")
public class LegacyUserService {

    private static final String GET_USER = "getLibraSystemUserIDs";

    private final FeatureToggleApi featureToggleApi;
    private final GatewayService gatewayService;

    public GatewayService.Response<LegacyGetUserResponse> getUser(String emailAddress) {
        return getUser(LegacyGetUserRequest.builder().emailAddress(emailAddress).build());
    }

    public GatewayService.Response<LegacyGetUserResponse> getUser(LegacyGetUserRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        log.debug(":getUser: Requesting legacy user details.");

        GatewayService.Response<LegacyGetUserResponse> response = gatewayService.postToGateway(
            GET_USER,
            LegacyGetUserResponse.class,
            normalizeRequestIfFeatureEnabled(request),
            null
        );

        if (response.isError()) {
            log.error(":getUser: Legacy Gateway response: HTTP Response Code {}", response.code);

            if (response.isException()) {
                log.error(":getUser: Exception", response.exception);
            } else if (response.isLegacyFailure()) {
                log.error(":getUser: Legacy Failure: Body:\n{}", response.body);
            }
        } else if (response.isSuccessful()) {
            log.debug(":getUser: Legacy Gateway response: Success.");
        }

        return response;
    }

    private LegacyGetUserRequest normalizeRequestIfFeatureEnabled(LegacyGetUserRequest request) {
        if (featureToggleApi.isFeatureEnabledWithPropertyValueDefault(
            FeatureFlags.RELEASE_1A_1_1,
            FeatureFlags.RELEASE_1A_1_1_ENABLED_PROPERTY
        )) {
            if (request.getEmailAddress() != null) {
                return LegacyGetUserRequest.builder()
                    .emailAddress(request.getEmailAddress().toLowerCase(Locale.ROOT))
                    .build();
            }
        }
        return request;
    }
}
