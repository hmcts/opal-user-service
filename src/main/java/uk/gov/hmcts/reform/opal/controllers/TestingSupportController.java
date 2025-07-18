package uk.gov.hmcts.reform.opal.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.opal.authentication.model.SecurityToken;
import uk.gov.hmcts.reform.opal.authentication.service.AccessTokenService;
import uk.gov.hmcts.reform.opal.authorisation.service.AuthorisationService;
import uk.gov.hmcts.reform.opal.launchdarkly.FeatureToggleService;

@RestController
@RequestMapping("/testing-support")
@RequiredArgsConstructor
@Tag(name = "Testing Support Controller")
@ConditionalOnProperty(prefix = "opal.testing-support-endpoints", name = "enabled", havingValue = "true")
public class TestingSupportController {

    private static final String X_USER_EMAIL = "X-User-Email";

    private final FeatureToggleService featureToggleService;
    private final AccessTokenService accessTokenService;
    private final AuthorisationService authorisationService;
    

    @GetMapping("/launchdarkly/bool/{featureKey}")
    public ResponseEntity<Boolean> isFeatureEnabled(@PathVariable String featureKey) {
        return ResponseEntity.ok(this.featureToggleService.isFeatureEnabled(featureKey));
    }

    @GetMapping("/launchdarkly/string/{featureKey}")
    public ResponseEntity<String> getFeatureValue(@PathVariable String featureKey) {
        return ResponseEntity.ok(this.featureToggleService.getFeatureValue(featureKey));
    }

    @GetMapping("/token/test-user")
    @Operation(summary = "Retrieves the token for default test user")
    public ResponseEntity<SecurityToken> getToken() {
        var accessTokenResponse = this.accessTokenService.getTestUserToken();
        var securityToken = authorisationService.getSecurityToken(accessTokenResponse.getAccessToken());
        return ResponseEntity.ok(securityToken);
    }

    @GetMapping("/token/user")
    @Operation(summary = "Retrieves the token for a given user")
    public ResponseEntity<SecurityToken> getTokenForUser(@RequestHeader(value = X_USER_EMAIL) String userEmail) {
        var accessTokenResponse = this.accessTokenService.getTestUserToken(userEmail);
        var securityToken = authorisationService.getSecurityToken(accessTokenResponse.getAccessToken());
        return ResponseEntity.ok(securityToken);
    }

    @GetMapping("/token/parse")
    public ResponseEntity<String> parseToken(@RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(this.accessTokenService.extractPreferredUsername(authorization));
    }
}
