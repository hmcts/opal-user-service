package uk.gov.hmcts.reform.opal.authorisation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.opal.authentication.model.SecurityToken;
import uk.gov.hmcts.reform.opal.authentication.service.AccessTokenService;
import uk.gov.hmcts.reform.opal.authorisation.model.UserState;
import uk.gov.hmcts.reform.opal.service.opal.UserService;

import java.util.Optional;

@Slf4j(topic = "opal.AuthorisationService")
@Service
@RequiredArgsConstructor
public class AuthorisationService {

    private final UserService userService;
    private final AccessTokenService accessTokenService;

    public UserState getAuthorisation(String username) {
        return userService.getUserStateByUsername(username);
    }

    public SecurityToken getSecurityToken(String accessToken) {
        var securityTokenBuilder = SecurityToken.builder()
            .accessToken(accessToken);
        Optional<String> preferredUsernameOptional = Optional.ofNullable(
            accessTokenService.extractPreferredUsername(accessToken));

        if (preferredUsernameOptional.isPresent()) {
            UserState userStateOptional = this.getAuthorisation(preferredUsernameOptional.get());
            securityTokenBuilder.userState(userStateOptional);
        }
        return securityTokenBuilder.build();
    }
}
