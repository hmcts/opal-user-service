package uk.gov.hmcts.reform.opal.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.jaas.JaasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.opal.common.logging.SecurityEventLoggingService;
import uk.gov.hmcts.opal.common.user.authentication.service.AccessTokenService;
import uk.gov.hmcts.opal.common.user.authentication.service.TokenValidator;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.mappers.UserMapper;
import uk.gov.hmcts.reform.opal.mappers.UserStateMapper;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitUserRepository;
import uk.gov.hmcts.reform.opal.repository.UserEntitlementRepository;
import uk.gov.hmcts.reform.opal.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPermissionsServiceV2Test {

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
    private UserPermissionsProxy proxy;

    @InjectMocks
    private UserPermissionsService service;

    private static final long USER_ID = 42L;
    private static final String TOKEN_PREFERRED_USERNAME = "opal-user@hmcts.net";
    private static final String TOKEN_NAME = "John Smith";
    private static final String TOKEN_SUBJECT = "hcv732JFVWhf3Fd";

    private UserEntity userEntity;
    private UserStateV2Dto dto;

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
    }

    @Test
    void getUserV2_longReturnsUserWhenFound() {
        // Arrange
        when(userRepository.findIdWithPermissions(USER_ID)).thenReturn(Optional.of(userEntity));

        // Act
        UserEntity result = service.getUserV2(USER_ID);

        // Assert
        assertEquals(userEntity, result);
        verify(userRepository).findIdWithPermissions(USER_ID);
    }

    @Test
    void getUserV2_longThrowsWhenUserMissing() {
        // Arrange
        when(userRepository.findIdWithPermissions(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException ex = assertThrows(
            EntityNotFoundException.class,
            () -> service.getUserV2(USER_ID)
        );
        assertEquals("User not found with id: " + USER_ID, ex.getMessage());
    }

    @Test
    void getUserV2_stringReturnsUserWhenFound() {
        // Arrange
        when(userRepository.findByTokenSubjectWithPermissions(TOKEN_SUBJECT)).thenReturn(Optional.of(userEntity));

        // Act
        UserEntity result = service.getUserV2(TOKEN_SUBJECT);

        // Assert
        assertEquals(userEntity, result);
    }

    @Test
    void getUserV2_stringThrowsWhenUserMissing() {
        // Arrange
        when(userRepository.findByTokenSubjectWithPermissions(TOKEN_SUBJECT)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException ex = assertThrows(
            EntityNotFoundException.class,
            () -> service.getUserV2(TOKEN_SUBJECT)
        );
        assertEquals("User not found with subject: " + TOKEN_SUBJECT, ex.getMessage());
    }

    @Test
    void getUserStateV2_NewLogin() {

        // Arrange
        JwtAuthenticationToken authentication = mock(JwtAuthenticationToken.class);
        when(authentication.getToken()).thenReturn(jwt);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(jwt.getSubject()).thenReturn(TOKEN_SUBJECT);
        when(proxy.getUserV2(TOKEN_SUBJECT)).thenReturn(userEntity);
        when(jwt.getClaimAsString("preferred_username")).thenReturn(TOKEN_PREFERRED_USERNAME);
        when(jwt.getClaimAsString("name")).thenReturn(TOKEN_NAME);
        when(userStateMapper.toUserStateV2Dto(userEntity)).thenReturn(dto);

        // Act
        UserStateV2Dto result = service.getUserStateV2(proxy, true);

        // Assert
        assertThat(result).isEqualTo(dto);
        verify(securityEventLoggingService).logEvent(
            eq("User Authentication"),
            eq("Success"),
            isNull(),
            eq("Authentication"),
            any(),
            argThat(data ->
                        data != null
                            && data.size() == 1
                            && Long.valueOf(USER_ID).equals(data.get("UserIdentifier"))
            )
        );
    }

    @Test
    void getUserStateV2_NotNewLogin() {

        // Arrange
        JwtAuthenticationToken authentication = mock(JwtAuthenticationToken.class);
        when(authentication.getToken()).thenReturn(jwt);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(jwt.getSubject()).thenReturn(TOKEN_SUBJECT);
        when(proxy.getUserV2(TOKEN_SUBJECT)).thenReturn(userEntity);
        when(jwt.getClaimAsString("preferred_username")).thenReturn(TOKEN_PREFERRED_USERNAME);
        when(jwt.getClaimAsString("name")).thenReturn(TOKEN_NAME);
        when(userStateMapper.toUserStateV2Dto(userEntity)).thenReturn(dto);

        // Act
        UserStateV2Dto result = service.getUserStateV2(proxy, false);

        // Assert
        assertThat(result).isEqualTo(dto);
        verifyNoInteractions(securityEventLoggingService);
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
            () -> service.getUserStateV2(proxy, true)
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
            () -> service.getUserStateV2(proxy, true)
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
        when(proxy.getUserV2(TOKEN_SUBJECT)).thenReturn(userEntity);
        when(jwt.getClaimAsString("preferred_username")).thenReturn(null);

        // Act & Assert
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.getUserStateV2(proxy, true)
        );
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertEquals("401 UNAUTHORIZED \"Claim not found: preferred_username\"", ex.getMessage());
    }

    @Test
    void getUserStateV2IdMethod_NewLoginWhenIdIsNonZero() {

        // Arrange
        Authentication authentication = mock(JwtAuthenticationToken.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(proxy.getUserV2(USER_ID)).thenReturn(userEntity);
        when(userStateMapper.toUserStateV2Dto(userEntity)).thenReturn(dto);
        Long clientUserId = 123L;
        when(proxy.getUserId(authentication, proxy)).thenReturn(clientUserId);

        // Act
        UserStateV2Dto result = service.getUserStateV2(USER_ID, proxy, true);

        // Assert
        assertThat(result).isEqualTo(dto);
        verify(securityEventLoggingService).logEvent(
            eq("User Authentication"),
            eq("Success"),
            isNull(),
            eq("Authentication"),
            any(),
            argThat(data ->
                        data != null
                            && data.size() == 1
                            && clientUserId.equals(data.get("UserIdentifier"))
            )
        );
    }

    @Test
    void getUserStateV2IdMethod_NonNewLoginWhenIdIsNonZero() {

        // Arrange
        when(proxy.getUserV2(USER_ID)).thenReturn(userEntity);
        when(userStateMapper.toUserStateV2Dto(userEntity)).thenReturn(dto);

        // Act
        UserStateV2Dto result = service.getUserStateV2(USER_ID, proxy, false);

        // Assert
        assertThat(result).isEqualTo(dto);
        verifyNoInteractions(securityEventLoggingService);
    }

    @Test
    void getUserStateV2IdMethod_WhenIdIsZero() {

        // Arrange
        when(proxy.getUserStateV2(proxy, true)).thenReturn(dto);

        // Act
        UserStateV2Dto result = service.getUserStateV2(0L, proxy, true);

        // Assert
        assertThat(result).isEqualTo(dto);
        verifyNoInteractions(securityEventLoggingService);
    }
}
