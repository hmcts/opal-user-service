package uk.gov.hmcts.reform.opal.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.opal.dto.UserStateDto;
import uk.gov.hmcts.reform.opal.service.UserPermissionsService;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j(topic = "opal.UserPermissionsController")
public class UserPermissionsController {


    //The name claim of the authorised user.
    private static final String NAME_CLAIM = "name";

    //The claim used to map the authorised user to the user entity.
    private static final String AUTHORISED_USER_CLAIM = "preferred_username";

    private final UserPermissionsService userPermissionsService;

    @GetMapping("/{userId}/state")
    public ResponseEntity<UserStateDto> getUserState(
        @PathVariable Long userId,
        Authentication authentication) {

        log.debug(":GET:getUserState: userId: {}", userId);

        UserStateDto userStateDto;

        if (userId == 0) {

            String username = extractClaimAsString(authentication, AUTHORISED_USER_CLAIM);
            String name = extractClaimAsString(authentication, NAME_CLAIM);

            log.debug(":GET:getUserState: userId is 0, using username: {}", username);

            userStateDto = userPermissionsService.getUserState(username);
            userStateDto.setName(name);


        } else {
            userStateDto = userPermissionsService.getUserState(userId);
        }

        return ResponseEntity.ok(userStateDto);
    }

    private String extractClaimAsString(Authentication authentication, String claimName) {
        log.debug(":extractClaimAsString: claim name: {}", claimName);
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String claimValue = jwt.getClaimAsString(claimName);
            if (claimValue != null) {
                return claimValue;
            } else {
                log.debug(":extractClaimAsString: claim not found: {}", claimName);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Claim not found: " + claimName);
            }
        }
        log.warn(":extractClaimAsString: Authentication not of type Jwt.");
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Claim not found: " + claimName);
    }

}
