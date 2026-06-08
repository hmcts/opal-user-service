package uk.gov.hmcts.reform.opal.service;

import jakarta.persistence.EntityNotFoundException;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.jaas.JaasAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import uk.gov.hmcts.common.exceptions.standard.UnauthorizedException;
import uk.gov.hmcts.opal.common.logging.SecurityEventLoggingService;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.reform.opal.config.properties.AppModeConfiguration;
import uk.gov.hmcts.reform.opal.config.properties.CacheConfiguration;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.exception.ResourceConflictException;
import uk.gov.hmcts.reform.opal.mappers.UserStateMapper;
import uk.gov.hmcts.reform.opal.repository.UserRepository;
import uk.gov.hmcts.reform.opal.service.opal.UserService;
import uk.gov.hmcts.reform.opal.service.synchronise.SynchronisePermissionsService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.opal.common.dto.ToJsonString.objectToPrettyJson;

@ExtendWith(MockitoExtension.class)
class UserPermissionsServiceV2Test {

    private static final long USER_ID = 42L;
    private static final long CACHE_TIMEOUT_MINUTES = 30L;
    private static final String TOKEN_PREFERRED_USERNAME = "opal-user@hmcts.net";
    private static final String TOKEN_NAME = "John Smith";
    private static final String TOKEN_SUBJECT = "hcv732JFVWhf3Fd";

    @Mock
    SecurityContext securityContext;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserStateMapper userStateMapper;

    @Mock
    private SecurityEventLoggingService securityEventLoggingService;

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

        setJwtAuthentication(TOKEN_SUBJECT, TOKEN_PREFERRED_USERNAME, TOKEN_NAME);
        when(userService.getUser(USER_ID)).thenReturn(userEntity);
        when(userRepository.findIdWithPermissions(USER_ID)).thenReturn(Optional.of(userEntity));
        when(userStateMapper.toUserStateV2Dto(userEntity, clock)).thenReturn(dto);

        // Act
        UserStateV2Dto result = service.getUserStateV2(true);

        // Assert
        assertThat(result).isEqualTo(dto);
        assertThat(userEntity.getLastLoginDate())
            .isEqualTo(LocalDateTime.ofInstant(fixedInstant, fixedZone));
        verify(userRepository).saveAndFlush(userEntity);
        verify(securityEventLoggingService).logEvent(
            eq("User Authentication"),
            eq("Success"),
            isNull(),
            eq("Authentication"),
            notNull(),
            argThat(data ->
                data != null
                    && data.size() == 1
                    && Long.valueOf(USER_ID).equals(data.get("UserIdentifier"))
            )
        );
        assertDtoWasCachedForSubject(TOKEN_SUBJECT);
    }

    @Test
    void getUserStateV2_NotNewLogin() {

        // Arrange
        setJwtAuthentication(TOKEN_SUBJECT, TOKEN_PREFERRED_USERNAME, TOKEN_NAME);
        when(userService.getUser(USER_ID)).thenReturn(userEntity);
        when(userRepository.findIdWithPermissions(USER_ID)).thenReturn(Optional.of(userEntity));
        when(userStateMapper.toUserStateV2Dto(userEntity, clock)).thenReturn(dto);

        // Act
        UserStateV2Dto result = service.getUserStateV2(false);

        // Assert
        assertThat(result).isEqualTo(dto);
        verifyNoInteractions(securityEventLoggingService);
        assertDtoWasCachedForSubject(TOKEN_SUBJECT);
    }

    @Test
    void getUserStateV2_whenRedisCachingFails_doesNotThrowAndReturnsDto() {

        // arrange
        setJwtAuthentication(TOKEN_SUBJECT, TOKEN_PREFERRED_USERNAME, TOKEN_NAME);
        when(userService.getUser(USER_ID)).thenReturn(userEntity);
        when(userRepository.findIdWithPermissions(USER_ID)).thenReturn(Optional.of(userEntity));
        when(userStateMapper.toUserStateV2Dto(userEntity, clock)).thenReturn(dto);

        doThrow(new DataAccessResourceFailureException("Redis unavailable"))
            .when(valueOperations)
            .set(eq("USER_STATE_" + TOKEN_SUBJECT), anyString(), eq(CACHE_TIMEOUT_MINUTES), eq(TimeUnit.MINUTES));

        // act & assert
        UserStateV2Dto result = assertDoesNotThrow(() -> service.getUserStateV2(false));

        assertThat(result).isEqualTo(dto);
        assertThat(result.getCacheName()).isEqualTo("USER_STATE_" + TOKEN_SUBJECT);
        verifyNoInteractions(securityEventLoggingService);
    }

    @Test
    void getUserStateV2_whenLegacySynchronisationSucceeds_callsSynchroniseAndRefreshUser() {

        // Arrange
        setJwtAuthentication(TOKEN_SUBJECT, TOKEN_PREFERRED_USERNAME, TOKEN_NAME);
        when(appModeConfiguration.getAppMode()).thenReturn("legacy");
        when(userRepository.findIdWithPermissions(USER_ID)).thenReturn(Optional.of(userEntity));
        when(userStateMapper.toUserStateV2Dto(userEntity, clock)).thenReturn(dto);
        when(userService.getUser(USER_ID)).thenReturn(userEntity);

        // Act
        UserStateV2Dto result = service.getUserStateV2(false);

        // Assert
        assertThat(result).isEqualTo(dto);
        verify(synchronisePermissionsService).synchronise(userEntity);
        verify(userService).refreshUser(userEntity);
        assertDtoWasCachedForSubject(TOKEN_SUBJECT);
    }

    @Test
    void getUserStateV2_whenLegacySynchronisationReloadsUserState_mapsReloadedUser() {

        // Arrange
        setJwtAuthentication(TOKEN_SUBJECT, TOKEN_PREFERRED_USERNAME, TOKEN_NAME);
        when(appModeConfiguration.getAppMode()).thenReturn("legacy");
        when(userService.getUser(USER_ID)).thenReturn(userEntity);
        UserEntity synchronisedUser = UserEntity.builder()
            .userId(USER_ID)
            .username(TOKEN_PREFERRED_USERNAME)
            .tokenName(TOKEN_NAME)
            .tokenSubject(TOKEN_SUBJECT)
            .versionNumber(5L)
            .build();
        when(userRepository.findIdWithPermissions(USER_ID)).thenReturn(Optional.of(synchronisedUser));
        when(userStateMapper.toUserStateV2Dto(synchronisedUser, clock)).thenReturn(dto);

        // Act
        UserStateV2Dto result = service.getUserStateV2(false);

        // Assert
        assertThat(result).isEqualTo(dto);
        verify(synchronisePermissionsService).synchronise(userEntity);
        verify(userService).refreshUser(userEntity);
        verify(userStateMapper).toUserStateV2Dto(synchronisedUser, clock);
    }

    @Test
    void getUserStateV2_whenLegacySynchronisationThrowsRuntime_propagatesRuntimeException() {

        // Arrange
        setJwtAuthentication(TOKEN_SUBJECT, TOKEN_PREFERRED_USERNAME, TOKEN_NAME);
        RuntimeException runtimeException = new RuntimeException("sync failed");
        when(appModeConfiguration.getAppMode()).thenReturn("legacy");
        doThrow(runtimeException).when(synchronisePermissionsService).synchronise(userEntity);
        when(userService.getUser(USER_ID)).thenReturn(userEntity);

        // Act
        RuntimeException thrown = assertThrows(
            RuntimeException.class,
            () -> service.getUserStateV2(false)
        );

        // Assert
        assertThat(thrown).isSameAs(runtimeException);
        verifyNoMoreInteractions(userService);
    }

    @Test
    void getUserStateV2_whenAppModeIsOpal_doesNotCallLegacySynchronisationServices() {

        // Arrange
        setJwtAuthentication(TOKEN_SUBJECT, TOKEN_PREFERRED_USERNAME, TOKEN_NAME);
        when(appModeConfiguration.getAppMode()).thenReturn("opal");
        when(userRepository.findIdWithPermissions(USER_ID)).thenReturn(Optional.of(userEntity));
        when(userStateMapper.toUserStateV2Dto(userEntity, clock)).thenReturn(dto);
        when(userService.getUser(USER_ID)).thenReturn(userEntity);

        // Act
        UserStateV2Dto result = service.getUserStateV2(false);

        // Assert
        assertThat(result).isEqualTo(dto);
        verifyNoInteractions(synchronisePermissionsService);
        verifyNoMoreInteractions(userService);
        assertDtoWasCachedForSubject(TOKEN_SUBJECT);
    }

    @Test
    void getUserStateV2_NewLogin_NotJwtAuthenticationToken() {

        // Arrange
        JaasAuthenticationToken authentication = mock(JaasAuthenticationToken.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        UnauthorizedException ex = assertThrows(
            UnauthorizedException.class,
            () -> service.getUserStateV2(true)
        );
        assertEquals("Current user is not authenticated with OpalJwtAuthenticationToken", ex.getMessage());
    }

    @Test
    void getUserStateV2_WhenAuthenticatedUserNotFound_ThrowsEntityNotFoundException() {

        // Arrange
        setJwtAuthentication(TOKEN_SUBJECT, TOKEN_PREFERRED_USERNAME, TOKEN_NAME);
        when(userService.getUser(USER_ID)).thenThrow(new EntityNotFoundException("User not found with id: 123"));
        // Act
        EntityNotFoundException ex = assertThrows(
            EntityNotFoundException.class,
            () -> service.getUserStateV2(false)
        );

        // Assert
        assertThat(ex).hasMessage("User not found with id: 123");
        verify(userRepository, never()).findIdWithPermissions(anyLong());
        verifyNoInteractions(userStateMapper);
    }

    @Test
    void getUserStateV2_WhenRequestedUserNotFound_ThrowsEntityNotFoundException() {

        // Arrange
        setJwtAuthentication(TOKEN_SUBJECT, TOKEN_PREFERRED_USERNAME, TOKEN_NAME);
        when(userService.getUser(USER_ID)).thenReturn(userEntity);
        when(userRepository.findIdWithPermissions(USER_ID)).thenReturn(Optional.empty());

        // Act
        EntityNotFoundException ex = assertThrows(
            EntityNotFoundException.class,
            () -> service.getUserStateV2(false)
        );

        // Assert
        assertThat(ex).hasMessage("User not found with id: " + USER_ID);
        verify(userRepository).findIdWithPermissions(USER_ID);
        verifyNoInteractions(userStateMapper);
    }

    @Test
    void getUserStateV2_PreferredUsernameClaimMissing() {

        // Arrange
        setJwtAuthentication(TOKEN_SUBJECT, null, TOKEN_NAME);
        when(userService.getUser(USER_ID)).thenReturn(userEntity);

        // Act & Assert
        BadCredentialsException ex = assertThrows(
            BadCredentialsException.class,
            () -> service.getUserStateV2(true)
        );
        assertEquals("Claim not found: 'preferred_username'", ex.getMessage());
    }

    @Test
    void getUserStateV2_NameClaimMissing() {

        // Arrange
        setJwtAuthentication(TOKEN_SUBJECT, TOKEN_PREFERRED_USERNAME, null);
        when(userService.getUser(USER_ID)).thenReturn(userEntity);

        // Act & Assert
        BadCredentialsException ex = assertThrows(
            BadCredentialsException.class,
            () -> service.getUserStateV2(true)
        );
        assertEquals("Claim not found: 'name'", ex.getMessage());
    }

    @Test
    void getUserStateV2_PreferredUsernameMismatch_ThrowsResourceConflictException() {

        // Arrange
        setJwtAuthentication(TOKEN_SUBJECT, "different-user@hmcts.net", TOKEN_NAME);
        when(userService.getUser(USER_ID)).thenReturn(userEntity);

        // Act
        ResourceConflictException ex = assertThrows(
            ResourceConflictException.class,
            () -> service.getUserStateV2(false)
        );

        // Assert
        assertEquals("User", ex.getResourceType());
        assertEquals(String.valueOf(USER_ID), ex.getResourceId());
        assertThat(ex.getConflictReason()).startsWith("Preferred Username mismatch:");
    }

    private void setJwtAuthentication(String subject, String preferredUsername, String name) {
        OpalJwtAuthenticationToken authentication = mock(OpalJwtAuthenticationToken.class);
        Jwt jwt = createJwt(subject, preferredUsername, name);
        lenient().when(authentication.getToken()).thenReturn(jwt);
        lenient().when(authentication.getUsername()).thenReturn(preferredUsername);
        lenient().when(authentication.getUserId()).thenReturn(USER_ID);

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private Jwt createJwt(String subject, String preferredUsername, String name) {
        Map<String, Object> claims = new HashMap<>();
        if (subject != null) {
            claims.put("sub", subject);
        }
        if (preferredUsername != null) {
            claims.put("preferred_username", preferredUsername);
        }
        if (name != null) {
            claims.put("name", name);
        }
        return new Jwt(
            "token",
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T01:00:00Z"),
            Map.of("alg", "none"),
            claims
        );
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
}
