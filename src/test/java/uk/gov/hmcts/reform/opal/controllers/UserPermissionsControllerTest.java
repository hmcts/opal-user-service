package uk.gov.hmcts.reform.opal.controllers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.reform.opal.dto.UserDto;
import uk.gov.hmcts.reform.opal.service.UserPermissionsService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@Slf4j(topic = "opal.UserPermissionsControllerTest")
@ExtendWith(MockitoExtension.class)
class UserPermissionsControllerTest {

    @Mock
    private UserPermissionsService userPermissionsService;

    @InjectMocks
    private UserPermissionsController controller;

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should get UserState when userId=0 uses principal name with preferred_username claim")
    void testGetUserState_whenUserIdZero() {
        // Arrange
        String expectedUsername = "opal-test@HMCTS.NET";

        UserStateDto returnedDto = UserStateDto.builder()
            .userId(123L)
            .username(expectedUsername)
            .build();

        given(userPermissionsService.getUserState(anyLong(), any(), any(), any())).willReturn(returnedDto);

        Authentication auth = createAuthentication(expectedUsername);

        // Act
        ResponseEntity<UserStateDto> response = controller.getUserState(0L, auth, null);
        UserStateDto result = response.getBody();

        // Assert
        assertNotNull(result);
        assertEquals(expectedUsername, result.getUsername());
        verify(userPermissionsService).getUserState(eq(0L), same(auth), same(userPermissionsService), isNull());
    }

    @Test
    @DisplayName("Should get UserState when userId=0 uses principal name with preferred_username claim")
    void testGetUserState_whenNoUserId() {
        // Arrange
        String expectedUsername = "opal-test@HMCTS.NET";

        UserStateDto returnedDto = UserStateDto.builder()
            .userId(123L)
            .username(expectedUsername)
            .build();

        given(userPermissionsService.getUserState(any(), any(), any())).willReturn(returnedDto);
        Authentication auth = createAuthentication(expectedUsername);

        // Act
        ResponseEntity<UserStateDto> response = controller.getUserState(auth, null);
        UserStateDto result = response.getBody();

        // Assert
        assertNotNull(result);
        assertEquals(expectedUsername, result.getUsername());
        verify(userPermissionsService).getUserState(same(auth), same(userPermissionsService), isNull());
    }

    private JwtAuthenticationToken createAuthentication(String preferredName) {
        Map<String, Object> claims = Map.of(
            "preferred_username", preferredName,
            "name", "Test User",
            "sub", "ohE52BNHaghsWf34");
        Jwt jwt = new Jwt("token", null, null, Map.of("alg", "none"), claims);
        return new JwtAuthenticationToken(jwt);
    }

    @Test
    void testAddUser() {
        // Arrange
        UserDto returnedDto = new UserDto();
        returnedDto.setUserId(123L);
        returnedDto.setUsername("opal-test@HMCTS.NET");
        given(userPermissionsService.addUser(any())).willReturn(returnedDto);

        // Act
        ResponseEntity<UserDto> response = controller.addUser("bearer token-value");
        UserDto result = response.getBody();

        // Assert
        assertNotNull(result);
        assertEquals("opal-test@HMCTS.NET", result.getUsername());
        verify(userPermissionsService).addUser(eq("bearer token-value"));
    }

    @Test
    void testUpdateUser_withId() {
        // Arrange
        UserDto returnedDto = new UserDto();
        returnedDto.setUserId(123L);
        returnedDto.setUsername("opal-test@HMCTS.NET");
        returnedDto.setVersionNumber(7L);
        given(userPermissionsService.updateUser(any(), any(), any(), any())).willReturn(returnedDto);

        // Act
        ResponseEntity<UserDto> response = controller.updateUser(1L, "bearer token-value", "if-match");
        log.info(":testUpdateUser: response: {}", response);
        final HttpHeaders headers = response.getHeaders();
        final UserDto result = response.getBody();

        // Assert
        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        assertNotNull(headers);
        assertEquals("\"7\"", headers.getETag());
        assertNotNull(result);
        assertEquals("opal-test@HMCTS.NET", result.getUsername());
        verify(userPermissionsService).updateUser(eq(1L), eq("bearer token-value"), same(userPermissionsService),
                                                  eq("if-match"));
    }

    @Test
    void testUpdateUser_withoutId() {
        // Arrange
        UserDto returnedDto = new UserDto();
        returnedDto.setUserId(123L);
        returnedDto.setUsername("opal-test@HMCTS.NET");
        returnedDto.setVersionNumber(7L);
        given(userPermissionsService.updateUser(any(), any(), any())).willReturn(returnedDto);

        // Act
        ResponseEntity<UserDto> response = controller.updateUser("bearer token-value", "if-match");
        log.info(":testUpdateUser: response: {}", response);
        final HttpHeaders headers = response.getHeaders();
        final UserDto result = response.getBody();

        // Assert
        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        assertNotNull(headers);
        assertEquals("\"7\"", headers.getETag());
        assertNotNull(result);
        assertEquals("opal-test@HMCTS.NET", result.getUsername());
        verify(userPermissionsService).updateUser(eq("bearer token-value"), same(userPermissionsService),
                                                  eq("if-match"));
    }

}
