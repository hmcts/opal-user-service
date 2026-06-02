package uk.gov.hmcts.reform.opal.authentication.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsers;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.mappers.UserStateMapper;
import uk.gov.hmcts.reform.opal.repository.UserRepository;
import uk.gov.hmcts.reform.opal.util.JwtUtil;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserOpalJwtAuthenticationProviderTest {

    @Mock
    private JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter;
    @Mock
    private JwtDecoder jwtDecoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserStateMapper userStateMapper;
    @Mock
    private Domain domain;

    @Mock
    private Clock clock;

    @InjectMocks
    @Spy
    private UserOpalJwtAuthenticationProvider userOpalJwtAuthenticationProvider;


    @Test
    void authenticate_shouldFail_whenBadJwtIsGiven() {
        final String token = "test-token";
        BearerTokenAuthenticationToken authenticationToken = mock(BearerTokenAuthenticationToken.class);
        when(authenticationToken.getToken()).thenReturn(token);
        BadJwtException simulatedException = mock(BadJwtException.class);
        when(simulatedException.getMessage()).thenReturn("test-msg");
        doThrow(simulatedException).when(jwtDecoder).decode(token);

        InvalidBearerTokenException actualException = assertThrows(InvalidBearerTokenException.class,
            () -> userOpalJwtAuthenticationProvider.authenticate(authenticationToken));

        assertEquals("test-msg", actualException.getMessage());
        assertEquals(simulatedException, actualException.getCause());
    }

    @Test
    void authenticate_shouldFail_OnAnyUnexpectedJwtDecodeErrors() {
        final String token = "test-token";
        BearerTokenAuthenticationToken authenticationToken = mock(BearerTokenAuthenticationToken.class);
        when(authenticationToken.getToken()).thenReturn(token);
        RuntimeException simulatedException = mock(RuntimeException.class);
        when(simulatedException.getMessage()).thenReturn("test-msg");
        doThrow(simulatedException).when(jwtDecoder).decode(token);

        AuthenticationServiceException actualException = assertThrows(AuthenticationServiceException.class,
            () -> userOpalJwtAuthenticationProvider.authenticate(authenticationToken));

        assertEquals("test-msg", actualException.getMessage());
        assertEquals(simulatedException, actualException.getCause());
    }

    @Test
    void authenticate_shouldFail_whenUserStateCanNotBeLocated() {
        //Arrange
        final String token = "test-token";
        final BearerTokenAuthenticationToken authenticationToken = mock(BearerTokenAuthenticationToken.class);
        final Jwt jwt = mock(Jwt.class);
        final Collection<GrantedAuthority> authorityCollection = List.of(
            mock(GrantedAuthority.class), mock(GrantedAuthority.class), mock(GrantedAuthority.class)
        );

        when(authenticationToken.getToken()).thenReturn(token);
        when(jwtDecoder.decode(token)).thenReturn(jwt);
        when(jwtGrantedAuthoritiesConverter.convert(jwt)).thenReturn(authorityCollection);
        doReturn(Optional.empty()).when(userOpalJwtAuthenticationProvider).getUserState(jwt);

        //Act
        InvalidBearerTokenException actualException = assertThrows(InvalidBearerTokenException.class,
            () -> userOpalJwtAuthenticationProvider.authenticate(authenticationToken));

        //Assert
        assertEquals("User state not found for authenticated user", actualException.getMessage());
    }

    @Test
    void authenticate_shouldReturnValidAuthToken_whenValidUserAndJwtProvided() {
        //Arrange
        final String token = "test-token";
        final BearerTokenAuthenticationToken authenticationToken = mock(BearerTokenAuthenticationToken.class);
        final Object someDetailsObject = mock(Object.class);
        final Jwt jwt = mock(Jwt.class);
        final UserStateV2 userState = mock(UserStateV2.class);
        final DomainBusinessUnitUsers  domainBusinessUnitUsers = mock(DomainBusinessUnitUsers.class);
        final Collection<GrantedAuthority> authorityCollection = List.of(
            mock(GrantedAuthority.class), mock(GrantedAuthority.class), mock(GrantedAuthority.class)
        );
        final String subClaimValue = "test-subject";

        //Setup
        when(authenticationToken.getToken()).thenReturn(token);
        when(authenticationToken.getDetails()).thenReturn(someDetailsObject);
        when(jwtDecoder.decode(token)).thenReturn(jwt);
        when(jwtGrantedAuthoritiesConverter.convert(jwt)).thenReturn(authorityCollection);
        doReturn(Optional.of(userState)).when(userOpalJwtAuthenticationProvider).getUserState(jwt);
        when(userState.getDomainBusinessUnitUsers(domain)).thenReturn(domainBusinessUnitUsers);
        when(domainBusinessUnitUsers.getBusinessUnitUsers()).thenReturn(List.of());
        when(jwt.getClaimAsString(JwtClaimNames.SUB)).thenReturn(subClaimValue);
        //Act
        OpalJwtAuthenticationToken result = userOpalJwtAuthenticationProvider.authenticate(authenticationToken);

        //Assert
        assertThat(result.getUserState())
            .isEqualTo(userState);
        assertThat(result.getToken())
            .isEqualTo(jwt);
        assertThat(result.getAuthorities())
            .isEqualTo(authorityCollection);
        assertThat(result.getName())
            .isEqualTo(subClaimValue);
        assertThat(result.getDetails())
            .isEqualTo(someDetailsObject);
    }

    @Test
    void supports_shouldReturnTrue_whenTypeIsBearerTokenAuthenticationToken() {
        assertTrue(
            userOpalJwtAuthenticationProvider.supports(BearerTokenAuthenticationToken.class)
        );
    }

    @Test
    void supports_shouldReturnFalse_whenTypeIsNotBearerTokenAuthenticationToken() {
        assertFalse(
            userOpalJwtAuthenticationProvider.supports(AbstractAuthenticationToken.class)
        );
    }

    @Test
    void getUserState_shouldReturnEmpty_whenUserNotFound() {
        try (MockedStatic<JwtUtil> jwtUtilMockedStatic = mockStatic(JwtUtil.class)) {
            final String subject = "test-subject";
            final Jwt jwt = mock(Jwt.class);
            jwtUtilMockedStatic.when(() -> JwtUtil.extractSubject(jwt)).thenReturn(subject);
            when(userRepository.findByTokenSubject(subject)).thenReturn(Optional.empty());
            assertEquals(Optional.empty(), userOpalJwtAuthenticationProvider.getUserState(jwt));
            verifyNoInteractions(userStateMapper);
        }
    }

    @Test
    void getUserState_shouldReturnValidUserState_whenUserFound() {
        try (MockedStatic<JwtUtil> jwtUtilMockedStatic = mockStatic(JwtUtil.class)) {
            final String subject = "test-subject";
            final UserEntity userEntity = mock(UserEntity.class);
            final Jwt jwt = mock(Jwt.class);
            final UserStateV2 userState = mock(UserStateV2.class);
            jwtUtilMockedStatic.when(() -> JwtUtil.extractSubject(jwt)).thenReturn(subject);
            when(userRepository.findByTokenSubject(subject)).thenReturn(Optional.of(userEntity));
            when(userStateMapper.toUserStateV2(userEntity, clock)).thenReturn(userState);

            assertEquals(Optional.of(userState), userOpalJwtAuthenticationProvider.getUserState(jwt));
        }
    }
}
