package uk.gov.hmcts.reform.opal.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JwtUtilTest {

    @Test
    void testExtractClaimAsString_success() {
        // Arrange
        JwtAuthenticationToken jwtAuthToken = createJwtAuthenticatedToken();

        // Act & Assert
        String claim = JwtUtil.extractClaim(jwtAuthToken.getToken(), "preferred_username");
        assertEquals("opal-user@hmcts.net", claim);

        claim = JwtUtil.extractClaim(jwtAuthToken.getToken(), "name");
        assertEquals("John Smith", claim);

        claim = JwtUtil.extractClaim(jwtAuthToken.getToken(), "sub");
        assertEquals("lkkljnwb7D1DFs", claim);

        claim = JwtUtil.extractSubject(jwtAuthToken.getToken());
        assertEquals("lkkljnwb7D1DFs", claim);
    }

    @Test
    void testExtractClaimAsString_fail_noClaimName() {
        // Arrange
        Jwt jwt = createJwtAuthenticatedToken().getToken();

        // Act & Assert
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> JwtUtil.extractClaim(jwt, "")
        );
        assertEquals("401 UNAUTHORIZED \"Claim not found: \"", ex.getMessage());
    }

    @Test
    void testExtractClaimAsString_fail_missingClaim() {
        // Arrange
        Jwt jwt = createJwtAuthenticatedToken().getToken();
        // Act & Assert
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> JwtUtil.extractClaim(jwt, "not_a_claim")
        );
        assertEquals("401 UNAUTHORIZED \"Claim not found: not_a_claim\"", ex.getMessage());
    }

    private JwtAuthenticationToken createJwtAuthenticatedToken() {
        Jwt jwt = Jwt.withTokenValue("mock-token")
            .header("alg", "RS256")
            .claim("sub", "lkkljnwb7D1DFs")
            .claim("preferred_username", "opal-user@hmcts.net")
            .claim("name", "John Smith")
            .issuedAt(Instant.now().minusSeconds(3600))
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        return new JwtAuthenticationToken(jwt);
    }
}
