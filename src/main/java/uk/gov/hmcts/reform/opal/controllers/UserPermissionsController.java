package uk.gov.hmcts.reform.opal.controllers;

import static uk.gov.hmcts.reform.opal.util.HttpUtil.buildCreatedResponse;
import static uk.gov.hmcts.reform.opal.util.HttpUtil.buildResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.opal.dto.UserDto;
import uk.gov.hmcts.reform.opal.dto.UserStateDto;
import uk.gov.hmcts.reform.opal.service.UserPermissionsService;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j(topic = "opal.UserPermissionsController")
public class UserPermissionsController {

    private final UserPermissionsService userPermissionsService;

    @GetMapping("/{userId}/state")
    public ResponseEntity<UserStateDto> getUserState(@PathVariable Long userId, Authentication authentication) {
        log.debug(":GET:getUserState: userId: {}", userId);
        return buildResponse(userPermissionsService.getUserState(userId, authentication));
    }

    @PostMapping
    public ResponseEntity<UserDto> addUser(@RequestHeader(value = "Authorization") String authHeaderValue) {
        log.debug(":POST:addUser:");
        return buildCreatedResponse(userPermissionsService.createUser(authHeaderValue));
    }
}
