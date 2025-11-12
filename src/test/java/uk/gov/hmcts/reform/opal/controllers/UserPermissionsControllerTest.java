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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.reform.opal.dto.UserDto;
import uk.gov.hmcts.reform.opal.dto.UserStateDto;
import uk.gov.hmcts.reform.opal.service.UserPermissionsService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@Slf4j
@ExtendWith(MockitoExtension.class)
class UserPermissionsControllerTest {

    @Mock
    private UserPermissionsService userPermissionsService;

    @InjectMocks
    private UserPermissionsController controller;

    @BeforeEach
    void setup() {
        // no additional setup
    }

    @Test
    @DisplayName("getUserState with userId=0 uses principal name with preferred_username claim")
    void getUserState_whenUserIdZero_invokesServiceWithPreferredUsernameClaim() {
        // Arrange
        String expectedUsername = "opal-test@HMCTS.NET";
        UserStateDto returnedDto = new UserStateDto();
        returnedDto.setUserId(123L);
        returnedDto.setUsername(expectedUsername);

        given(userPermissionsService.getUserState(anyLong(), any(), any())).willReturn(returnedDto);

        Map<String, Object> claims = Map.of(
            "preferred_username", expectedUsername,
            "name","Test User",
            "sub", "ohE52BNHaghsWf34");
        Jwt jwt = new Jwt("token", null, null, Map.of("alg", "none"), claims);
        Authentication auth = new JwtAuthenticationToken(jwt);

        // Act
        ResponseEntity<UserStateDto> response = controller.getUserState(0L, auth);
        UserStateDto result = response.getBody();

        // Assert
        assertNotNull(result);
        assertEquals(expectedUsername, result.getUsername());
        verify(userPermissionsService).getUserState(anyLong(), any(), any());
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
    }

    @Test
    void testUpdateUser() {
        // Arrange
        UserDto returnedDto = new UserDto();
        returnedDto.setUserId(123L);
        returnedDto.setUsername("opal-test@HMCTS.NET");
        returnedDto.setVersion(7L);
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
    }

    @Test
    void testUpdateUser_2() {
        // Arrange
        UserDto returnedDto = new UserDto();
        returnedDto.setUserId(123L);
        returnedDto.setUsername("opal-test@HMCTS.NET");
        returnedDto.setVersion(7L);
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
    }
}
