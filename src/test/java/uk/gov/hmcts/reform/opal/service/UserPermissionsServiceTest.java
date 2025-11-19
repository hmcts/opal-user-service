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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private static final String TOKEN_PREFERRED_USERNAME = "opal-user@hmcts.net";
    private static final String TOKEN_NAME = "John Smith";
    private static final String TOKEN_SUBJECT = "hcv732JFVWhf3Fd";
    private UserEntity userEntity;
    private UserStateDto userDto;

    @BeforeEach
    void setUp() {
        userEntity = new UserEntity();
        userEntity.setUserId(USER_ID);
        userEntity.setUsername(TOKEN_PREFERRED_USERNAME);
        userEntity.setTokenName(TOKEN_NAME);
        userEntity.setTokenSubject(TOKEN_SUBJECT);
        userEntity.setVersion(4L);

        userDto = new UserStateDto();
        userDto.setUserId(USER_ID);
        userDto.setUsername(TOKEN_PREFERRED_USERNAME);
    }

    @Test
    @DisplayName("getUserState(Long) throws when no entitlements and user missing")
    void buildUserState_longNoEntitlements_throws() {
        when(userRepository.findById(USER_ID))
            .thenReturn(java.util.Optional.empty());

        EntityNotFoundException ex = assertThrows(
            EntityNotFoundException.class,
            () -> service.getUserState(USER_ID, null, service)
        );
        assertEquals("User not found with id: " + USER_ID, ex.getMessage());

        verify(userRepository).findById(USER_ID);
    }

    @Test
    @DisplayName("getUserState(String) delegates to getUserState(Long) after lookup")
    void getUserState_jwtAuthPrinciple() {
        // Arrange
        JwtAuthenticationToken jwtAuthToken = createJwtAuthenticatedToken();
        when(userRepository.findByTokenSubject(any())).thenReturn(java.util.Optional.of(userEntity));
        doReturn(userDto).when(service).buildUserState(any());

        UserStateDto result = service.getUserState(jwtAuthToken, service);

        assertEquals(userDto, result);
        verify(userRepository).findByTokenSubject(any());
        verify(service).buildUserState(any());
    }

    @Test
    @DisplayName("getUserState(String) throws when username not found")
    void getUserState_jwtAuthPrinciple_throws() {
        // Arrange
        JwtAuthenticationToken jwtAuthToken = createJwtAuthenticatedToken();
        when(userRepository.findByTokenSubject(any())).thenReturn(java.util.Optional.empty());

        EntityNotFoundException ex = assertThrows(
            EntityNotFoundException.class,
            () -> service.getUserState(jwtAuthToken, service)
        );
        assertEquals("User not found with subject: lkkljnwb7D1DFs", ex.getMessage());

        verify(userRepository).findByTokenSubject(any());
    }

    @Test
    void testAddUser() throws Exception {
        // Arrange
        String bearerToken = createJwtToken();
        JWT parsedJwt = tokenValidator.parse(bearerToken);

        when(tokenService.extractClaims(any())).thenReturn(parsedJwt.getJWTClaimsSet());
        when(userRepository.saveAndFlush(any())).thenReturn(userEntity);

        // Act
        UserDto response = service.addUser(bearerToken);

        // Assert
        assertNotNull(response);
        log.info(":testAddUser: response: \n{}", response.toPrettyJson());
        assertEquals(42L, response.getUserId());
        assertEquals("opal-user@hmcts.net", response.getUsername());
        assertEquals("hcv732JFVWhf3Fd", response.getSubject());
        assertNull(response.getStatus());
        assertEquals("John Smith", response.getName());
        assertEquals(4L, response.getVersion());
    }

    @Test
    void testUpdateUser() throws Exception {
        // Arrange
        String bearerToken = createJwtToken();
        JWT parsedJwt = tokenValidator.parse(bearerToken);

        when(tokenService.extractClaims(any())).thenReturn(parsedJwt.getJWTClaimsSet());
        when(userRepository.findById(any())).thenReturn(Optional.of(userEntity));
        when(userRepository.saveAndFlush(any())).thenReturn(userEntity);

        // Act
        UserDto response = service.updateUser(1L, bearerToken, service, "\"4\"");

        // Assert
        assertNotNull(response);
        log.info(":testUpdateUser: response: \n{}", response.toPrettyJson());
        assertEquals(42L, response.getUserId());
        assertEquals("j.s@example.com", response.getUsername());
        assertEquals("hcv732JFVWhf3Fd", response.getSubject());
        assertNull(response.getStatus());
        assertEquals("john.smith", response.getName());
        assertEquals(4L, response.getVersion());
    }

    @Test
    void testUpdateUser_2() throws Exception {
        // Arrange
        String bearerToken = createJwtToken();
        JWT parsedJwt = tokenValidator.parse(bearerToken);

        when(tokenService.extractClaims(any())).thenReturn(parsedJwt.getJWTClaimsSet());
        when(userRepository.findByTokenSubject(any())).thenReturn(Optional.of(userEntity));
        when(userRepository.saveAndFlush(any())).thenReturn(userEntity);

        // Act
        UserDto response = service.updateUser(bearerToken, service, "\"4\"");

        // Assert
        assertNotNull(response);
        log.info(":testUpdateUser: response: \n{}", response.toPrettyJson());
        assertEquals(42L, response.getUserId());
        assertEquals("j.s@example.com", response.getUsername());
        assertEquals("hcv732JFVWhf3Fd", response.getSubject());
        assertNull(response.getStatus());
        assertEquals("john.smith", response.getName());
        assertEquals(4L, response.getVersion());
    }

    @Test
    void testExtractClaimAsString_success() {
        // Arrange
        JwtAuthenticationToken jwtAuthToken = createJwtAuthenticatedToken();

        // Act & Assert
        String claim = service.extractClaim(jwtAuthToken.getToken(), "preferred_username");
        assertEquals("opal-user@hmcts.net", claim);

        claim = service.extractClaim(jwtAuthToken.getToken(), "name");
        assertEquals("John Smith", claim);

        claim = service.extractClaim(jwtAuthToken.getToken(), "sub");
        assertEquals("lkkljnwb7D1DFs", claim);

        claim = service.extractSubject(jwtAuthToken.getToken());
        assertEquals("lkkljnwb7D1DFs", claim);
    }

    @Test
    void testExtractClaimAsString_fail_noClaimName() {
        // Arrange
        Jwt jwt = createJwtAuthenticatedToken().getToken();

        // Act & Assert
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.extractClaim(jwt, "")
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
            () -> service.extractClaim(jwt, "not_a_claim")
        );
        assertEquals("401 UNAUTHORIZED \"Claim not found: not_a_claim\"", ex.getMessage());
    }

    @Test
    void testgetJwtToken_fail_incorrectAuthenticationToken() {
        // Arrange
        TestingAuthenticationToken testToken = new TestingAuthenticationToken(null, null);

        // Act & Assert
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.getJwtToken(testToken)
        );
        assertEquals("401 UNAUTHORIZED \"Authentication Token not of type Jwt.\"", ex.getMessage());
    }


    private String createJwtToken() throws NoSuchAlgorithmException {
        JWTCreator.Builder builder = com.auth0.jwt.JWT.create()
            .withHeader(Map.of("typ", "JWT", "alg", "RS256"))
            .withSubject("hcv732JFVWhf3Fd")
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
            .claim("sub", "lkkljnwb7D1DFs")
            .claim("preferred_username", "opal-user@hmcts.net")
            .claim("name", "John Smith")
            .issuedAt(Instant.now().minusSeconds(3600))
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        return new JwtAuthenticationToken(jwt);
    }
}
