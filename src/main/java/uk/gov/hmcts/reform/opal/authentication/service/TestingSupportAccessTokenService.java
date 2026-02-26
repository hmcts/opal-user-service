package uk.gov.hmcts.reform.opal.authentication.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.common.user.authentication.model.AccessTokenRequest;
import uk.gov.hmcts.opal.common.user.authentication.model.AccessTokenResponse;
import uk.gov.hmcts.reform.opal.authentication.client.AzureTokenClient;
import uk.gov.hmcts.reform.opal.authentication.config.internal.InternalAuthConfigurationProperties;
import uk.gov.hmcts.reform.opal.config.properties.TestUser;

@Slf4j(topic = "opal.AccessTokenHelper")
@Service("accessTokenHelper")
@RequiredArgsConstructor
public class TestingSupportAccessTokenService {

    private final TestUser testUser;

    private final InternalAuthConfigurationProperties configuration;

    private final AzureTokenClient azureTokenClient;

    public AccessTokenResponse getTestUserToken() {
        return getAccessToken(testUser.getEmail(), testUser.getPassword());
    }

    public AccessTokenResponse getTestUserToken(String userEmail) {
        return getAccessToken(userEmail, testUser.getPassword());
    }

    public AccessTokenResponse getAccessToken(String userName, String password) {

        AccessTokenRequest tokenRequest = AccessTokenRequest.builder()
            .grantType("password")
            .clientId(configuration.getClientId())
            .clientSecret(configuration.getClientSecret())
            .scope(configuration.getScope())
            .username(userName)
            .password(password)
            .build();

        return azureTokenClient.getAccessToken(
            tokenRequest
        );
    }
}
