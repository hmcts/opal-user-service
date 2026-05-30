package uk.gov.hmcts.reform.opal.authentication.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.util.Assert;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;
import uk.gov.hmcts.reform.opal.mappers.UserStateMapper;
import uk.gov.hmcts.reform.opal.repository.UserRepository;
import uk.gov.hmcts.reform.opal.util.JwtUtil;

import java.time.Clock;
import java.util.Collection;
import java.util.Optional;

@Slf4j
public class UserOpalJwtAuthenticationProvider implements AuthenticationProvider {

    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter;
    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;
    private final UserStateMapper userStateMapper;
    private final Domain domain;
    private final Clock clock;

    public UserOpalJwtAuthenticationProvider(JwtDecoder jwtDecoder,
                                             UserRepository userRepository,
                                             UserStateMapper userStateMapper,
                                             Clock clock,
                                             JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter,
                                             Domain domain) {
        Assert.notNull(jwtDecoder, "jwtDecoder cannot be null");
        this.jwtDecoder = jwtDecoder;
        this.userRepository = userRepository;
        this.userStateMapper = userStateMapper;
        this.clock = clock;
        this.jwtGrantedAuthoritiesConverter = jwtGrantedAuthoritiesConverter;
        this.domain = domain;
    }

    /**
     * Decode and validate the
     * <a href="https://tools.ietf.org/html/rfc6750#section-1.2" target="_blank">Bearer
     * Token</a>.
     *
     * @param authentication the authentication request object.
     * @return A successful authentication
     * @throws AuthenticationException if authentication failed for some reason
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        BearerTokenAuthenticationToken bearer = (BearerTokenAuthenticationToken) authentication;
        Jwt jwt;
        try {
            jwt = this.jwtDecoder.decode(bearer.getToken());
        } catch (BadJwtException failed) {
            log.debug("Failed to authenticate since the JWT was invalid");
            throw new InvalidBearerTokenException(failed.getMessage(), failed);
        } catch (JwtException failed) {
            throw new AuthenticationServiceException(failed.getMessage(), failed);
        }
        Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);

        UserStateV2 userState = getUserState(jwt)
            .orElseThrow(() -> new InvalidBearerTokenException("User state not found for authenticated user"));

        return new OpalJwtAuthenticationToken(userState, domain, jwt, authorities, bearer.getDetails());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private Optional<UserStateV2> getUserState(Jwt jwt) {
        String subject = JwtUtil.extractSubject(jwt);
        return userRepository.findByTokenSubject(subject)
            .map(userEntity -> userStateMapper.toUserStateV2(userEntity, clock));
    }

}
