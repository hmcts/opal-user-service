package uk.gov.hmcts.reform.opal.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JwtUtil {

    public static String extractSubject(final Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject != null) {
            return subject;
        } else {
            log.debug(":SubjectAction.extract: subject not found.");
            throw new BadCredentialsException("Subject not found.");
        }
    }

    public static String extractClaim(final Jwt jwt, String claimName) {
        String claimValue = jwt.getClaimAsString(claimName);
        if (claimValue != null) {
            return claimValue;
        } else {
            log.debug(":ClaimAction.extract: claim not found: {}", claimName);
            throw new BadCredentialsException(String.format("Claim not found: '%s'", claimName));
        }
    }
}
