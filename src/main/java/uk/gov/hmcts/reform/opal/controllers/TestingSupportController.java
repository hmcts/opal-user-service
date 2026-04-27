package uk.gov.hmcts.reform.opal.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.opal.common.user.authentication.model.AccessTokenResponse;
import uk.gov.hmcts.opal.common.user.authentication.service.AccessTokenService;
import uk.gov.hmcts.reform.opal.authentication.service.TestingSupportAccessTokenService;
import uk.gov.hmcts.reform.opal.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.opal.service.opal.UserService;

import java.time.OffsetDateTime;
import java.util.Set;

@RestController
@Slf4j(topic = "opal.TestingSupportController")
@RequestMapping("/testing-support")
@RequiredArgsConstructor
@Tag(name = "Testing Support Controller")
@ConditionalOnProperty(prefix = "opal.testing-support-endpoints", name = "enabled", havingValue = "true")
public class TestingSupportController {

    private static final String X_USER_EMAIL = "X-User-Email";

    private final FeatureToggleService featureToggleService;
    private final TestingSupportAccessTokenService testingSupportAccessTokenService;
    private final AccessTokenService userAccessTokenService;
    private final UserService userService;

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
    public ResponseEntity<TestingSupportTokenResponse> getToken() {
        log.debug(":getToken: for test user defined at: opal.test-user.email");
        AccessTokenResponse accessTokenResponse = testingSupportAccessTokenService.getTestUserToken();
        return ResponseEntity.ok(new TestingSupportTokenResponse(accessTokenResponse.getAccessToken()));
    }

    @GetMapping("/token/user")
    @Operation(summary = "Retrieves the token for a given user")
    public ResponseEntity<TestingSupportTokenResponse> getTokenForUser(@RequestHeader(value = X_USER_EMAIL)
                                                                       String userEmail) {
        log.debug(":getTokenForUser: user: {}", userEmail);
        AccessTokenResponse accessTokenResponse = testingSupportAccessTokenService.getTestUserToken(userEmail);
        return ResponseEntity.ok(new TestingSupportTokenResponse(accessTokenResponse.getAccessToken()));
    }

    @GetMapping("/token/parse")
    public ResponseEntity<String> parseToken(@RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(this.userAccessTokenService.extractPreferredUsername(authorization));
    }

    @PutMapping("/users/{userId}/roles/{roleId}")
    @Operation(summary = "Testing-support-only endpoint to add or replace role assignments for a user")
    public ResponseEntity<Void> addOrReplaceRoleInformationOnUser(
        @PathVariable Long userId, @PathVariable Long roleId, @RequestBody Set<Short> businessUnitIds) {
        log.debug(":addOrReplaceRoleInformationOnUser: userId: {}, roleId: {}", userId, roleId);

        userService.addOrReplaceRoleInformationOnUser(userId, roleId, businessUnitIds, userService);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{userId}")
    public ResponseEntity<Void> activateUser(
        @PathVariable Long userId,
        @RequestBody ActivateUserRequest request) {

        log.debug(":activateUser : userId: {}, activateDate: {}", userId, request.activationDate());

        userService.activateUser(
            userId,
            request.activationDate()
        );

        return ResponseEntity.noContent().build();
    }

    record TestingSupportTokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    record ActivateUserRequest(OffsetDateTime activationDate) {
    }
}
