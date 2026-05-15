package uk.gov.hmcts.reform.opal.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.jaas.JaasAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.opal.common.logging.SecurityEventLoggingService;
import uk.gov.hmcts.opal.common.user.authentication.service.AccessTokenService;
import uk.gov.hmcts.opal.common.user.authentication.service.TokenValidator;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.reform.opal.config.properties.AppModeConfiguration;
import uk.gov.hmcts.reform.opal.config.properties.CacheConfiguration;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.mappers.UserMapper;
import uk.gov.hmcts.reform.opal.mappers.UserStateMapper;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitUserRepository;
import uk.gov.hmcts.reform.opal.repository.UserEntitlementRepository;
import uk.gov.hmcts.reform.opal.repository.UserRepository;
import uk.gov.hmcts.reform.opal.service.opal.UserService;
import uk.gov.hmcts.reform.opal.service.synchronise.LegacySyncException;
import uk.gov.hmcts.reform.opal.service.synchronise.SynchronisePermissionsService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static uk.gov.hmcts.opal.common.dto.ToJsonString.objectToPrettyJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPermissionsServiceV2Test {

    private static final long USER_ID = 42L;
    private static final long AUTHENTICATED_USER_ID = 84L;
    private static final long CACHE_TIMEOUT_MINUTES = 30L;
    private static final String TOKEN_PREFERRED_USERNAME = "opal-user@hmcts.net";
    private static final String TOKEN_NAME = "John Smith";
    private static final String TOKEN_SUBJECT = "hcv732JFVWhf3Fd";
    private static final String AUTHENTICATED_TOKEN_SUBJECT = "auth-subject-84";

    @Mock
    SecurityContext securityContext;

    @Mock
    private UserEntitlementRepository userEntitlementRepository;

    @Mock
    private BusinessUnitUserRepository businessUnitUserRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserStateMapper userStateMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AccessTokenService tokenService;

    @Mock
    private TokenValidator tokenValidator;

    @Mock
    private SecurityEventLoggingService securityEventLoggingService;

    @Mock
    private Jwt jwt;

    @Mock
    private Clock clock;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UserPermissionsService service;

    private UserEntity userEntity;
    private UserStateV2Dto dto;

    @Mock
    private CacheConfiguration cacheConfiguration;

    @Mock
    private SynchronisePermissionsService synchronisePermissionsService;

    @Mock
    private AppModeConfiguration appModeConfiguration;

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        userEntity = UserEntity.builder()
            .userId(USER_ID)
            .username(TOKEN_PREFERRED_USERNAME)
            .tokenName(TOKEN_NAME)
            .tokenSubject(TOKEN_SUBJECT)
            .versionNumber(4L)
            .build();
        dto = UserStateV2Dto.builder().build();
        SecurityContextHolder.clearContext();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(cacheConfiguration.getUserStateTimeoutMinutes()).thenReturn(CACHE_TIMEOUT_MINUTES);
        lenient().when(appModeConfiguration.getAppMode()).thenReturn("opal");
    }

    @Test
    void getUserStateV2_NewLogin() {

        // Arrange
        Instant fixedInstant = Instant.parse("2026-04-14T10:15:30Z");
        ZoneId fixedZone = ZoneId.of("UTC");
        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(fixedZone);

        JwtAuthenticationToken authentication = mock(JwtAuthenticationToken.class);
        when(authentication.getToken()).thenReturn(jwt);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(jwt.getSubject()).thenReturn(TOKEN_SUBJECT);
        when(userRepository.findByTokenSubject(TOKEN_SUBJECT)).thenReturn(Optional.of(userEntity));
        when(userRepository.findIdWithPermissions(USER_ID)).thenReturn(Optional.of(userEntity));
        when(jwt.getClaimAsString("preferred_username")).thenReturn(TOKEN_PREFERRED_USERNAME);
        when(jwt.getClaimAsString("name")).thenReturn(TOKEN_NAME);
        when(userStateMapper.toUserStateV2Dto(userEntity, clock)).thenReturn(dto);

        // Act
        UserStateV2Dto result = service.getUserStateV2(0L, true);

        // Assert
        assertThat(result).isEqualTo(dto);
        assertThat(userEntity.getLastLoginDate())
            .isEqualTo(LocalDateTime.ofInstant(fixedInstant, fixedZone));
        verify(userRepository).saveAndFlush(userEntity);
        assertDtoWasCachedForSubject(TOKEN_SUBJECT);
    }

    @Test
    void getUserStateV2_NotNewLogin() {

        // Arrange
        JwtAuthenticationToken authentication = mock(JwtAuthenticationToken.class);
        when(authentication.getToken()).thenReturn(jwt);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(jwt.getSubject()).thenReturn(TOKEN_SUBJECT);
        when(userRepository.findByTokenSubject(TOKEN_SUBJECT)).thenReturn(Optional.of(userEntity));
        when(userRepository.findIdWithPermissions(USER_ID)).thenReturn(Optional.of(userEntity));
        when(jwt.getClaimAsString("preferred_username")).thenReturn(TOKEN_PREFERRED_USERNAME);
        when(jwt.getClaimAsString("name")).thenReturn(TOKEN_NAME);
        when(userStateMapper.toUserStateV2Dto(userEntity, clock)).thenReturn(dto);

        // Act
        UserStateV2Dto result = service.getUserStateV2(0L, false);

        // Assert
        assertThat(result).isEqualTo(dto);
        verifyNoInteractions(securityEventLoggingService);
        assertDtoWasCachedForSubject(TOKEN_SUBJECT);
    }

    @Test
    void getUserStateV2_whenRedisCachingFails_doesNotThrowAndReturnsDto() {

        // arrange
        JwtAuthenticationToken authentication = mock(JwtAuthenticationToken.class);
        when(authentication.getToken()).thenReturn(jwt);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(jwt.getSubject()).thenReturn(TOKEN_SUBJECT);
        when(userRepository.findByTokenSubject(TOKEN_SUBJECT)).thenReturn(Optional.of(userEntity));
        when(userRepository.findIdWithPermissions(USER_ID)).thenReturn(Optional.of(userEntity));
        when(jwt.getClaimAsString("preferred_username")).thenReturn(TOKEN_PREFERRED_USERNAME);
        when(jwt.getClaimAsString("name")).thenReturn(TOKEN_NAME);
        when(userStateMapper.toUserStateV2Dto(userEntity, clock)).thenReturn(dto);

        doThrow(new DataAccessResourceFailureException("Redis unavailable"))
            .when(valueOperations)
            .set(eq("USER_STATE_" + TOKEN_SUBJECT), anyString(), eq(CACHE_TIMEOUT_MINUTES), eq(TimeUnit.MINUTES));

        // act & assert
        UserStateV2Dto result = assertDoesNotThrow(() -> service.getUserStateV2(0L, false));

        assertThat(result).isEqualTo(dto);
        assertThat(result.getCacheName()).isEqualTo("USER_STATE_" + TOKEN_SUBJECT);
        verifyNoInteractions(securityEventLoggingService);
    }

    @Test
    void getUserStateV2IdMethod_whenRedisCachingFails_doesNotThrowAndReturnsDto() {

        // arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userEntity));
        when(userRepository.findIdWithPermissions(USER_ID)).thenReturn(Optional.of(userEntity));
        when(userStateMapper.toUserStateV2Dto(userEntity, clock)).thenReturn(dto);

        doThrow(new DataAccessResourceFailureException("Redis unavailable"))
            .when(valueOperations)
            .set(eq("USER_STATE_" + TOKEN_SUBJECT), anyString(), eq(CACHE_TIMEOUT_MINUTES), eq(TimeUnit.MINUTES));

        // act & assert
        UserStateV2Dto result = assertDoesNotThrow(() -> service.getUserStateV2(USER_ID, false));

        assertThat(result).isEqualTo(dto);
        assertThat(result.getCacheName()).isEqualTo("USER_STATE_" + TOKEN_SUBJECT);
        verifyNoInteractions(securityEventLoggingService);
    }

    @Test
    void getUserStateV2_whenLegacySynchronisationSucceeds_callsSynchroniseAndRefreshUser() {

        // Arrange
        when(appModeConfiguration.getAppMode()).thenReturn("legacy");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userEntity));
        when(userRepository.findIdWithPermissions(USER_ID)).thenReturn(Optional.of(userEntity));
        when(userStateMapper.toUserStateV2Dto(userEntity, clock)).thenReturn(dto);

        // Act
        UserStateV2Dto result = service.getUserStateV2(USER_ID, false);

        // Assert
        assertThat(result).isEqualTo(dto);
        verify(synchronisePermissionsService).synchronise(userEntity);
        verify(userService).refreshUser(userEntity);
        assertDtoWasCachedForSubject(TOKEN_SUBJECT);
    }

    @Test
    void getUserStateV2_whenLegacySynchronisationThrowsRuntime_wrapsAsLegacySyncException() {

        // Arrange
        RuntimeException runtimeException = new RuntimeException("sync failed");
        when(appModeConfiguration.getAppMode()).thenReturn("legacy");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userEntity));
        doThrow(runtimeException).when(synchronisePermissionsService).synchronise(userEntity);

        // Act
        LegacySyncException thrown = assertThrows(
            LegacySyncException.class,
            () -> service.getUserStateV2(USER_ID, false)
        );

        // Assert
        assertThat(thrown.getMessage()).isEqualTo("Permissions synchronisation failed for userId: " + USER_ID);
        assertThat(thrown.getCause()).isEqualTo(runtimeException);
        verifyNoInteractions(userService);
    }

    @Test
    void getUserStateV2_whenAppModeIsOpal_doesNotCallLegacySynchronisationServices() {

        // Arrange
        when(appModeConfiguration.getAppMode()).thenReturn("opal");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userEntity));
        when(userRepository.findIdWithPermissions(USER_ID)).thenReturn(Optional.of(userEntity));
        when(userStateMapper.toUserStateV2Dto(userEntity, clock)).thenReturn(dto);

        // Act
        UserStateV2Dto result = service.getUserStateV2(USER_ID, false);

        // Assert
        assertThat(result).isEqualTo(dto);
        verifyNoInteractions(synchronisePermissionsService, userService);
        assertDtoWasCachedForSubject(TOKEN_SUBJECT);
    }

    @Test
    void getUserStateV2_NewLogin_NotJwtAuthenticationToken() {

        // Arrange
        JaasAuthenticationToken authentication = mock(JaasAuthenticationToken.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.getUserStateV2(0L, true)
        );
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertEquals("401 UNAUTHORIZED \"Authentication Token not of type Jwt.\"", ex.getMessage());
    }

    @Test
    void getUserStateV2_SubjectClaimMissing() {

        // Arrange
        JwtAuthenticationToken authentication = mock(JwtAuthenticationToken.class);
        when(authentication.getToken()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(null);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.getUserStateV2(0L, true)
        );
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertEquals("401 UNAUTHORIZED \"Subject not found.\"", ex.getMessage());
    }

    @Test
    void getUserStateV2_PreferredUsernameClaimMissing() {

        // Arrange
        JwtAuthenticationToken authentication = mock(JwtAuthenticationToken.class);
        when(authentication.getToken()).thenReturn(jwt);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(jwt.getSubject()).thenReturn(TOKEN_SUBJECT);
        when(userRepository.findByTokenSubject(TOKEN_SUBJECT)).thenReturn(Optional.of(userEntity));
        when(jwt.getClaimAsString("preferred_username")).thenReturn(null);

        // Act & Assert
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.getUserStateV2(0L, true)
        );
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertEquals("401 UNAUTHORIZED \"Claim not found: preferred_username\"", ex.getMessage());
    }

    @Test
    void getUserStateV2IdMethod_NewLoginWhenIdIsNonZero() {

        // Arrange
        Instant fixedInstant = Instant.parse("2026-04-14T10:15:30Z");
        ZoneId fixedZone = ZoneId.of("UTC");
        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(fixedZone);
        setAuthenticatedCaller(AUTHENTICATED_USER_ID, AUTHENTICATED_TOKEN_SUBJECT);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userEntity));
        when(userRepository.findIdWithPermissions(USER_ID)).thenReturn(Optional.of(userEntity));
        when(userStateMapper.toUserStateV2Dto(userEntity, clock)).thenReturn(dto);

        // Act
        UserStateV2Dto result = service.getUserStateV2(USER_ID, true);

        // Assert
        assertThat(result).isEqualTo(dto);
        assertThat(userEntity.getLastLoginDate())
            .isEqualTo(LocalDateTime.ofInstant(fixedInstant, fixedZone));
        verify(userRepository).saveAndFlush(userEntity);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> eventDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(securityEventLoggingService).logEvent(
            eq("User Authentication"),
            eq("Success"),
            isNull(),
            eq("Authentication"),
            any(),
            eventDataCaptor.capture()
        );
        assertThat(eventDataCaptor.getValue()).containsEntry("UserIdentifier", AUTHENTICATED_USER_ID);
        assertDtoWasCachedForSubject(TOKEN_SUBJECT);
    }

    @Test
    void getUserStateV2IdMethod_NonNewLoginWhenIdIsNonZero() {

        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userEntity));
        when(userRepository.findIdWithPermissions(USER_ID)).thenReturn(Optional.of(userEntity));
        when(userStateMapper.toUserStateV2Dto(userEntity, clock)).thenReturn(dto);

        // Act
        UserStateV2Dto result = service.getUserStateV2(USER_ID, false);

        // Assert
        assertThat(result).isEqualTo(dto);
        verifyNoInteractions(securityEventLoggingService);
        assertDtoWasCachedForSubject(TOKEN_SUBJECT);
    }

    @Test
    void getUserStateV2IdMethod_WhenIdIsZero() {

        // Arrange
        Instant fixedInstant = Instant.parse("2026-04-14T10:15:30Z");
        ZoneId fixedZone = ZoneId.of("UTC");
        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(fixedZone);

        JwtAuthenticationToken authentication = mock(JwtAuthenticationToken.class);
        when(authentication.getToken()).thenReturn(jwt);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(jwt.getSubject()).thenReturn(TOKEN_SUBJECT);
        when(userRepository.findByTokenSubject(TOKEN_SUBJECT)).thenReturn(Optional.of(userEntity));
        when(userRepository.findIdWithPermissions(USER_ID)).thenReturn(Optional.of(userEntity));
        when(jwt.getClaimAsString("preferred_username")).thenReturn(TOKEN_PREFERRED_USERNAME);
        when(jwt.getClaimAsString("name")).thenReturn(TOKEN_NAME);
        when(userStateMapper.toUserStateV2Dto(userEntity, clock)).thenReturn(dto);

        // Act
        UserStateV2Dto result = service.getUserStateV2(0L, true);

        // Assert
        assertThat(result).isEqualTo(dto);
        verify(userRepository).saveAndFlush(userEntity);
        assertDtoWasCachedForSubject(TOKEN_SUBJECT);
    }

    @Test
    void getUserStateV2_NewLogin_UpdatesLastLogin() {
        // Arrange
        Instant fixedInstant = Instant.parse("2026-04-14T10:15:30Z");
        ZoneId fixedZone = ZoneId.of("UTC");
        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(fixedZone);

        JwtAuthenticationToken authentication = mock(JwtAuthenticationToken.class);
        when(authentication.getToken()).thenReturn(jwt);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(jwt.getSubject()).thenReturn(TOKEN_SUBJECT);
        when(userRepository.findByTokenSubject(TOKEN_SUBJECT)).thenReturn(Optional.of(userEntity));
        when(userRepository.findIdWithPermissions(USER_ID)).thenReturn(Optional.of(userEntity));
        when(jwt.getClaimAsString("preferred_username")).thenReturn(TOKEN_PREFERRED_USERNAME);
        when(jwt.getClaimAsString("name")).thenReturn(TOKEN_NAME);
        when(userStateMapper.toUserStateV2Dto(userEntity, clock)).thenReturn(dto);

        // Act
        UserStateV2Dto result = service.getUserStateV2(0L, true);

        // Assert
        assertThat(result).isEqualTo(dto);
        assertThat(userEntity.getLastLoginDate())
            .isEqualTo(LocalDateTime.ofInstant(fixedInstant, fixedZone));
        verify(userRepository).saveAndFlush(userEntity);
        assertDtoWasCachedForSubject(TOKEN_SUBJECT);
    }

    @Test
    void getUserStateV2IdMethod_NewLogin_UpdatesLastLogin() {
        // Arrange
        Instant fixedInstant = Instant.parse("2026-04-14T10:15:30Z");
        ZoneId fixedZone = ZoneId.of("UTC");
        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(fixedZone);
        setAuthenticatedCaller(AUTHENTICATED_USER_ID, AUTHENTICATED_TOKEN_SUBJECT);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userEntity));
        when(userRepository.findIdWithPermissions(USER_ID)).thenReturn(Optional.of(userEntity));
        when(userStateMapper.toUserStateV2Dto(userEntity, clock)).thenReturn(dto);

        // Act
        UserStateV2Dto result = service.getUserStateV2(USER_ID, true);

        // Assert
        assertThat(result).isEqualTo(dto);
        assertThat(userEntity.getLastLoginDate())
            .isEqualTo(LocalDateTime.ofInstant(fixedInstant, fixedZone));
        verify(userRepository).saveAndFlush(userEntity);
        assertDtoWasCachedForSubject(TOKEN_SUBJECT);
    }

    private void assertDtoWasCachedForSubject(String subject) {
        String cacheKey = "USER_STATE_" + subject;
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        verify(valueOperations).set(
            eq(cacheKey),
            payloadCaptor.capture(),
            eq(CACHE_TIMEOUT_MINUTES),
            eq(TimeUnit.MINUTES)
        );
        assertThat(dto.getCacheName()).isEqualTo(cacheKey);
        assertThat(payloadCaptor.getValue()).isEqualTo(objectToPrettyJson(dto));
    }

    private void setAuthenticatedCaller(long callerUserId, String callerSubject) {
        JwtAuthenticationToken authentication = mock(JwtAuthenticationToken.class);
        when(authentication.getToken()).thenReturn(jwt);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        UserEntity callerUser = UserEntity.builder()
            .userId(callerUserId)
            .tokenSubject(callerSubject)
            .build();
        when(jwt.getSubject()).thenReturn(callerSubject);
        when(userRepository.findByTokenSubject(callerSubject)).thenReturn(Optional.of(callerUser));
    }
}
