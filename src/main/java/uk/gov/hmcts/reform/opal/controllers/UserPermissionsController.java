package uk.gov.hmcts.reform.opal.controllers;

import static uk.gov.hmcts.reform.opal.util.HttpUtil.buildCreatedResponse;
import static uk.gov.hmcts.reform.opal.util.HttpUtil.buildResponse;
import static uk.gov.hmcts.reform.opal.util.FeatureFlags.RELEASE_1A;
import static uk.gov.hmcts.reform.opal.util.FeatureFlags.RELEASE_1A_ENABLED_PROPERTY;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureToggle;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.reform.opal.dto.UserDto;
import uk.gov.hmcts.reform.opal.service.UserPermissionsService;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j(topic = "opal.UserPermissionsController")
public class UserPermissionsController {

    private static final String X_NEW_LOGIN = "X-New-Login";
    private final UserPermissionsService userPermissionsService;

    @GetMapping("/state")
    @Deprecated //Replaced by UserPermissionsV2Controller.getUserStateV2
    @FeatureToggle(feature = RELEASE_1A, defaultValueProperty = RELEASE_1A_ENABLED_PROPERTY)
    public ResponseEntity<UserStateDto> getUserState(
        Authentication authentication,
        @RequestHeader(value = X_NEW_LOGIN, required = false) Boolean newLogin) {

        log.debug(":GET:getUserState: new login: {}", newLogin);
        return buildResponse(userPermissionsService.getUserState(authentication, newLogin));
    }

    @GetMapping("/{userId}/state")
    @Deprecated  //Replaced by UserPermissionsV2Controller.getUserStateV2
    @FeatureToggle(feature = RELEASE_1A, defaultValueProperty = RELEASE_1A_ENABLED_PROPERTY)
    public ResponseEntity<UserStateDto> getUserState(
        @PathVariable Long userId, Authentication authentication,
        @RequestHeader(value = X_NEW_LOGIN, required = false) Boolean newLogin) {
        log.debug(":GET:getUserState: userId: {}, new login: {}", userId, newLogin);
        return buildResponse(userPermissionsService
                                 .getUserState(userId, authentication, newLogin));
    }

    @PostMapping()
    @FeatureToggle(feature = RELEASE_1A, defaultValueProperty = RELEASE_1A_ENABLED_PROPERTY)
    public ResponseEntity<UserDto> addUser(@RequestHeader(value = "Authorization") String authHeaderValue) {
        log.debug(":POST:addUser:");
        return buildCreatedResponse(userPermissionsService.addUser(authHeaderValue));
    }

    @PutMapping("/{userId}")
    @FeatureToggle(feature = RELEASE_1A, defaultValueProperty = RELEASE_1A_ENABLED_PROPERTY)
    public ResponseEntity<UserDto> updateUser(@PathVariable Long userId,
                                              @RequestHeader(value = "Authorization") String authHeaderValue,
                                              @RequestHeader(value = "If-Match") String ifMatch) {
        log.debug(":PUT:updateUser:");
        return buildResponse(userPermissionsService
                                 .updateUser(userId, authHeaderValue, ifMatch));
    }

    @PutMapping()
    @FeatureToggle(feature = RELEASE_1A, defaultValueProperty = RELEASE_1A_ENABLED_PROPERTY)
    public ResponseEntity<UserDto> updateUser(@RequestHeader(value = "Authorization") String authHeaderValue,
                                              @RequestHeader(value = "If-Match") String ifMatch) {
        log.debug(":PUT:updateUser:");
        return buildResponse(userPermissionsService.updateUser(authHeaderValue, ifMatch));
    }

}
