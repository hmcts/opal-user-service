package uk.gov.hmcts.reform.opal.service;

import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.nimbusds.jwt.JWT;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.opal.authentication.service.AccessTokenService;
import uk.gov.hmcts.reform.opal.authentication.service.TokenValidator;
import uk.gov.hmcts.reform.opal.dto.UserDto;
import uk.gov.hmcts.reform.opal.dto.UserStateDto;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.mappers.UserMapper;
import uk.gov.hmcts.reform.opal.mappers.UserStateMapper;
import uk.gov.hmcts.reform.opal.repository.UserEntitlementRepository;
import uk.gov.hmcts.reform.opal.repository.UserRepository;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Slf4j(topic = "opal.UserPermissionsServiceTest")
class UserPermissionsServiceTest {

    @Mock
    private UserEntitlementRepository userEntitlementRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private UserStateMapper userStateMapper = Mappers.getMapper(UserStateMapper.class);

    @Spy
    private UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Mock
    private AccessTokenService tokenService;

    @Spy
    private TokenValidator tokenValidator;

    @Spy
    @InjectMocks
    private UserPermissionsService service;

    private static final long USER_ID = 42L;
    private static final String USERNAME = "opal-user";
    private UserEntity userEntity;
    private UserStateDto userDto;

    @BeforeEach
    void setUp() {
        userEntity = new UserEntity();
        userEntity.setUserId(USER_ID);
        userEntity.setUsername(USERNAME);

        userDto = new UserStateDto();
        userDto.setUserId(USER_ID);
        userDto.setUsername(USERNAME);
    }

    @Test
    @DisplayName("getUserState(Long) throws when no entitlements and user missing")
    void getUserState_longNoEntitlements_throws() {
        when(userEntitlementRepository.findAllByUserIdWithFullJoins(USER_ID))
            .thenReturn(Collections.emptySet());
        when(userRepository.findById(USER_ID))
            .thenReturn(java.util.Optional.empty());

        EntityNotFoundException ex = assertThrows(
            EntityNotFoundException.class,
            () -> service.getUserState(USER_ID)
        );
        assertEquals("User not found with ID: " + USER_ID, ex.getMessage());

        verify(userEntitlementRepository).findAllByUserIdWithFullJoins(USER_ID);
        verify(userRepository).findById(USER_ID);
    }

    @Test
    @DisplayName("getUserState(String) delegates to getUserState(Long) after lookup")
    void getUserState_stringDelegatesToLong() {
        when(userRepository.findOptionalByUsername(USERNAME))
            .thenReturn(java.util.Optional.of(userEntity));
        doReturn(userDto).when(service).getUserState(USER_ID);

        UserStateDto result = service.getUserState(USERNAME);

        assertEquals(userDto, result);
        verify(userRepository).findOptionalByUsername(USERNAME);
        verify(service).getUserState(USER_ID);
    }

    @Test
    @DisplayName("getUserState(String) throws when username not found")
    void getUserState_stringNotFound_throws() {
        when(userRepository.findOptionalByUsername(USERNAME))
            .thenReturn(java.util.Optional.empty());

        EntityNotFoundException ex = assertThrows(
            EntityNotFoundException.class,
            () -> service.getUserState(USERNAME)
        );
        assertEquals("User not found with username: " + USERNAME, ex.getMessage());

        verify(userRepository).findOptionalByUsername(USERNAME);
    }

    @Test
    void testAddUser() throws Exception {
        // Arrange
        String bearerToken = createJwtToken();
        JWT parsedJwt = tokenValidator.parse(bearerToken);

        when(tokenService.extractClaims(any())).thenReturn(parsedJwt.getJWTClaimsSet());
        when(userRepository.saveAndFlush(any())).thenReturn(userEntity);

        // Act
        UserDto response = service.createUser(bearerToken);

        // Assert
        assertNotNull(response);
        log.info(":testAddUser: response: \n{}", response.toPrettyJson());
        assertEquals("opal-user", response.getUsername());
        assertEquals(42, response.getUserId());
    }

    @Test
    void testExtractClaimAsString_success() {
        // Arrange
        JwtAuthenticationToken jwtAuthToken = createJwtAuthenticatedToken();

        // Act & Assert
        String claim = service.extractClaimAsString(jwtAuthToken, "preferred_username");
        assertEquals("j.s@example.com", claim);

        claim = service.extractClaimAsString(jwtAuthToken, "name");
        assertEquals("john.smith", claim);

        claim = service.extractClaimAsString(jwtAuthToken, "sub");
        assertEquals("Fines", claim);
    }

    @Test
    void testExtractClaimAsString_fail_noClaimName() {
        // Arrange
        JwtAuthenticationToken jwtAuthToken = createJwtAuthenticatedToken();

        // Act & Assert
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.extractClaimAsString(jwtAuthToken, "")
        );
        assertEquals("401 UNAUTHORIZED \"Claim not found: \"", ex.getMessage());
    }

    @Test
    void testExtractClaimAsString_fail_missingClaim() {
        // Arrange
        JwtAuthenticationToken jwtAuthToken = createJwtAuthenticatedToken();

        // Act & Assert
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.extractClaimAsString(jwtAuthToken, "not_a_claim")
        );
        assertEquals("401 UNAUTHORIZED \"Claim not found: not_a_claim\"", ex.getMessage());
    }

    @Test
    void testExtractClaimAsString_fail_incorrectAuthenticationToken() {
        // Arrange
        TestingAuthenticationToken testToken = new TestingAuthenticationToken(null, null);

        // Act & Assert
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.extractClaimAsString(testToken, "preferred_username")
        );
        assertEquals("401 UNAUTHORIZED \"Authentication Token not of type Jwt.\"", ex.getMessage());
    }


    private String createJwtToken() throws NoSuchAlgorithmException {
        JWTCreator.Builder builder = com.auth0.jwt.JWT.create()
            .withHeader(Map.of("typ", "JWT", "alg", "RS256"))
            .withSubject("Fines")
            .withIssuer("Opal")
            .withExpiresAt(Instant.now().plusSeconds(3600))
            .withIssuedAt(Instant.now().minusSeconds(3600))
            .withClaim("name", "john.smith")
            .withClaim("preferred_username", "j.s@example.com");

        KeyPair keyPair = generateKeyPair();
        return builder
            .sign(Algorithm.RSA256((RSAPublicKey)keyPair.getPublic(), (RSAPrivateKey)keyPair.getPrivate()));
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    private JwtAuthenticationToken createJwtAuthenticatedToken() {
        Jwt jwt = Jwt.withTokenValue("mock-token")
            .header("alg", "RS256")
            .claim("sub", "Fines")
            .claim("preferred_username", "j.s@example.com")
            .claim("name", "john.smith")
            .issuedAt(Instant.now().minusSeconds(3600))
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        return new JwtAuthenticationToken(jwt);
    }
}
