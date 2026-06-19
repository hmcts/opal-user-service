package uk.gov.hmcts.reform.opal.controllers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.reform.opal.dto.UserDto;
import uk.gov.hmcts.reform.opal.service.UserPermissionsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    void testAddUser() throws InterruptedException {
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
    void testUpdateUser_withId() throws InterruptedException {
        // Arrange
        UserDto returnedDto = new UserDto();
        returnedDto.setUserId(123L);
        returnedDto.setUsername("opal-test@HMCTS.NET");
        returnedDto.setVersionNumber(7L);
        given(userPermissionsService.updateUser(any(), any(), any())).willReturn(returnedDto);

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
        verify(userPermissionsService).updateUser(eq(1L), eq("bearer token-value"),
                                                  eq("if-match"));
    }

    @Test
    void testUpdateUser_withoutId() throws InterruptedException {
        // Arrange
        UserDto returnedDto = new UserDto();
        returnedDto.setUserId(123L);
        returnedDto.setUsername("opal-test@HMCTS.NET");
        returnedDto.setVersionNumber(7L);
        given(userPermissionsService.updateUser(any(), any())).willReturn(returnedDto);

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
        verify(userPermissionsService).updateUser(eq("bearer token-value"),
                                                  eq("if-match"));
    }

}
