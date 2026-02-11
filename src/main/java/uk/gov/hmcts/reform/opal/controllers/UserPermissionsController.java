package uk.gov.hmcts.reform.opal.controllers;

import static uk.gov.hmcts.reform.opal.util.HttpUtil.buildCreatedResponse;
import static uk.gov.hmcts.reform.opal.util.HttpUtil.buildResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.opal.generated.http.api.UsersStateApi;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.opal.generated.model.UserStateV1UserState;
import uk.gov.hmcts.opal.generated.model.UserStateV2UserState;
import uk.gov.hmcts.reform.opal.dto.UserDto;
import uk.gov.hmcts.reform.opal.service.UserPermissionsService;

@RestController
@RequiredArgsConstructor
@Slf4j(topic = "opal.UserPermissionsController")
public class UserPermissionsController implements UsersStateApi {

    private static final String X_NEW_LOGIN = "X-New-Login";
    private final UserPermissionsService userPermissionsService;

    @GetMapping("/users/state")
    public ResponseEntity<UserStateDto> getUserState(
        Authentication authentication,
        @RequestHeader(value = X_NEW_LOGIN, required = false) Boolean newLogin) {

        log.debug(":GET:getUserState: new login: {}", newLogin);
        return buildResponse(userPermissionsService.getUserState(authentication, userPermissionsService, newLogin));
    }

    @GetMapping("/users/{userId}/state")
    public ResponseEntity<UserStateDto> getUserState(
        @PathVariable Long userId, Authentication authentication,
        @RequestHeader(value = X_NEW_LOGIN, required = false) Boolean newLogin) {

        log.debug(":GET:getUserState: userId: {}, new login: {}", userId, newLogin);
        return buildResponse(userPermissionsService
                                 .getUserState(userId, authentication, userPermissionsService, newLogin));
    }

    @PostMapping("/users")
    public ResponseEntity<UserDto> addUser(@RequestHeader(value = "Authorization") String authHeaderValue) {
        log.debug(":POST:addUser:");
        return buildCreatedResponse(userPermissionsService.addUser(authHeaderValue));
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long userId,
                                              @RequestHeader(value = "Authorization") String authHeaderValue,
                                              @RequestHeader(value = "If-Match") String ifMatch) {
        log.debug(":PUT:updateUser:");
        return buildResponse(userPermissionsService
                                 .updateUser(userId, authHeaderValue, userPermissionsService, ifMatch));
    }

    @PutMapping("/users")
    public ResponseEntity<UserDto> updateUser(@RequestHeader(value = "Authorization") String authHeaderValue,
                                              @RequestHeader(value = "If-Match") String ifMatch) {
        log.debug(":PUT:updateUser:");
        return buildResponse(userPermissionsService.updateUser(authHeaderValue, userPermissionsService, ifMatch));
    }

    /*
     * Implementations of API generated methods
     */

    // TODO - this is a placeholder implementation before the above methods are deprecated
    @Override
    public ResponseEntity<UserStateV1UserState> getUserStateV1(final Long id, final Boolean newLogin) {
        log.debug(":GET:getUserStateV1: new login: {}", newLogin);
        return buildResponse(userPermissionsService.getUserStateV1(
            id, getAuthentication(), userPermissionsService, newLogin));
    }

    // TODO - this is a placeholder implementation before the above methods are deprecated
    @Override
    public ResponseEntity<UserStateV2UserState> getUserStateV2(final Long id, final Boolean newLogin) {
        log.debug(":GET:getUserStateV2: new login: {}", newLogin);
        return buildResponse(userPermissionsService.getUserStateV2(
            id, getAuthentication(), userPermissionsService, newLogin));
    }

    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
