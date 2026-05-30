package uk.gov.hmcts.reform.opal.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
public class JwtUtil {


    public static String extractSubject(final Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject != null) {
            return subject;
        } else {
            log.debug(":SubjectAction.extract: subject not found.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Subject not found.");
        }
    }

    public static String extractClaim(final Jwt jwt, String claimName) {
        String claimValue = jwt.getClaimAsString(claimName);
        if (claimValue != null) {
            return claimValue;
        } else {
            log.debug(":ClaimAction.extract: claim not found: {}", claimName);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Claim not found: " + claimName);
        }
    }
}
