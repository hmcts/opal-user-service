package uk.gov.hmcts.reform.opal.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.reform.opal.dto.UserStateDto;
import uk.gov.hmcts.reform.opal.service.UserPermissionsService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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

        given(userPermissionsService.getUserState(expectedUsername))
            .willReturn(returnedDto);

        Map<String, Object> claims = Map.of(
            "preferred_username", expectedUsername,
            "name","Test User");
        Jwt jwt = new Jwt("token", null, null, Map.of("alg", "none"), claims);
        Authentication auth = new JwtAuthenticationToken(jwt);

        // Act
        ResponseEntity<UserStateDto> response = controller.getUserState(0L, auth);
        UserStateDto result = response.getBody();

        // Assert
        assertNotNull(result);
        assertEquals(expectedUsername, result.getUsername());
        verify(userPermissionsService).getUserState(expectedUsername);
    }
}
