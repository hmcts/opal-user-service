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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.opal.common.user.authentication.service.AccessTokenService;
import uk.gov.hmcts.opal.common.user.authentication.service.TokenValidator;
import uk.gov.hmcts.opal.common.logging.SecurityEventLoggingService;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.reform.opal.dto.UserDto;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.mappers.UserMapper;
import uk.gov.hmcts.reform.opal.mappers.UserMapperImpl;
import uk.gov.hmcts.reform.opal.mappers.UserStateMapper;
import uk.gov.hmcts.reform.opal.mappers.UserStateMapperImplementation;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitUserRepository;
import uk.gov.hmcts.reform.opal.repository.UserEntitlementRepository;
import uk.gov.hmcts.reform.opal.repository.UserRepository;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Slf4j(topic = "opal.UserPermissionsServiceTest")
class UserPermissionsServiceTest {

    @Mock
    private UserEntitlementRepository userEntitlementRepository;

    @Mock
    private BusinessUnitUserRepository businessUnitUserRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private UserStateMapper userStateMapper = new UserStateMapperImplementation();

    @Spy
    private UserMapper userMapper = new UserMapperImpl();

    @Mock
    private AccessTokenService tokenService;

    @Spy
    private TokenValidator tokenValidator;

    @Mock
    private SecurityEventLoggingService securityEventLoggingService;

    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-04-02T12:30:00Z"), ZoneOffset.UTC);

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
        userEntity = UserEntity.builder()
            .userId(USER_ID)
            .username(TOKEN_PREFERRED_USERNAME)
            .tokenName(TOKEN_NAME)
            .tokenSubject(TOKEN_SUBJECT)
            .versionNumber(4L)
            .build();

        userDto = UserStateDto.builder()
            .userId(USER_ID)
            .username(TOKEN_PREFERRED_USERNAME)
            .build();
    }

    @Test
    @DisplayName("getUserState(Long) throws when no entitlements and user missing")
    void testBuildUserState_longNoEntitlements_throws() {
        when(userRepository.findById(USER_ID))
            .thenReturn(java.util.Optional.empty());

        EntityNotFoundException ex = assertThrows(
            EntityNotFoundException.class,
            () -> service.getUserState(USER_ID, null, service, null)
        );
        assertEquals("User not found with id: " + USER_ID, ex.getMessage());

        verify(userRepository).findById(USER_ID);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false, true})
    @DisplayName("getUserState(String) delegates to getUserState(Long) after lookup")
    void testGetUserState_jwtAuthPrinciple(Boolean newLogin) {
        // Arrange
        final JwtAuthenticationToken jwtAuthToken = createJwtAuthenticatedToken();
        when(userRepository.findByTokenSubject(any())).thenReturn(java.util.Optional.of(userEntity));
        doReturn(userDto).when(service).buildUserState(any());
        boolean testNewLogin = Boolean.TRUE.equals(newLogin);
        if (testNewLogin) {
            doAnswer(invocation -> {
                log.info(":SecurityEventLoggingService.logEvent: <mock log message>");
                return null;
            }).when(securityEventLoggingService).logEvent(any(), any(), any(), any(), any(), any());
        }

        UserStateDto result = service.getUserState(jwtAuthToken, service, newLogin);

        assertEquals(userDto, result);
        verify(userRepository).findByTokenSubject(any());
        verify(service).buildUserState(any());
        VerificationMode mode = testNewLogin ? times(1) : never();
        verify(securityEventLoggingService, mode).logEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("getUserId(String) from Authentication object")
    void testGetUserId() {
        // Arrange
        JwtAuthenticationToken jwtAuthToken = createJwtAuthenticatedToken();
        when(userRepository.findByTokenSubject(any())).thenReturn(java.util.Optional.of(userEntity));

        // Act
        long result = service.getUserId(jwtAuthToken, service);

        // Assert
        assertEquals(USER_ID, result);
        verify(userRepository).findByTokenSubject(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false, true})
    @DisplayName("getUserState(String) throws when username not found")
    void testGetUserState_jwtAuthPrinciple_throws(Boolean newLogin) {
        // Arrange
        JwtAuthenticationToken jwtAuthToken = createJwtAuthenticatedToken();
        when(userRepository.findByTokenSubject(any())).thenReturn(java.util.Optional.empty());

        EntityNotFoundException ex = assertThrows(
            EntityNotFoundException.class,
            () -> service.getUserState(jwtAuthToken, service, newLogin)
        );
        assertEquals("User not found with subject: lkkljnwb7D1DFs", ex.getMessage());

        verify(userRepository).findByTokenSubject(any());
        verify(securityEventLoggingService, never()).logEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    void buildUserState_returnsEmptyPermissionsWhenEntitlementsMissing() {
        BusinessUnitUserEntity firstBusinessUnitUser = BusinessUnitUserEntity.builder()
            .businessUnitUserId("L081JG")
            .businessUnit(BusinessUnitEntity.builder().businessUnitId((short) 67).build())
            .build();
        BusinessUnitUserEntity secondBusinessUnitUser = BusinessUnitUserEntity.builder()
            .businessUnitUserId("L082JG")
            .businessUnit(BusinessUnitEntity.builder().businessUnitId((short) 69).build())
            .build();

        when(userEntitlementRepository.findAllByUserIdWithFullJoins(USER_ID))
            .thenReturn(java.util.Collections.emptySet());
        when(businessUnitUserRepository.findAllByUser_UserId(USER_ID))
            .thenReturn(List.of(firstBusinessUnitUser, secondBusinessUnitUser));

        UserStateDto result = service.buildUserState(userEntity);

        assertNotNull(result);
        assertEquals(USER_ID, result.getUserId());
        assertNotNull(result.getBusinessUnitUsers());
        assertEquals(2, result.getBusinessUnitUsers().size());
        assertTrue(result.getBusinessUnitUsers().stream().allMatch(buu -> buu.getPermissions().isEmpty()));
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
        assertEquals("John Smith", response.getName());
        assertEquals("active", response.getStatus());
        assertEquals(BigInteger.valueOf(4L), response.getVersion());
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
        assertEquals("john.smith", response.getName());
        assertEquals("active", response.getStatus());
        assertEquals(BigInteger.valueOf(4L), response.getVersion());
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
        assertEquals("john.smith", response.getName());
        assertEquals("active", response.getStatus());
        assertEquals(BigInteger.valueOf(4L), response.getVersion());
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
