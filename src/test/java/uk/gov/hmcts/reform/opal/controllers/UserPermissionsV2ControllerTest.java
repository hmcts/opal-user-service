package uk.gov.hmcts.reform.opal.controllers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.reform.opal.service.UserPermissionsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Slf4j(topic = "opal.UserPermissionsV2ControllerTest")
@ExtendWith(MockitoExtension.class)
class UserPermissionsV2ControllerTest {

    @Mock
    private UserPermissionsService userPermissionsService;

    @InjectMocks
    private UserPermissionsV2Controller controller;

    @Test
    @DisplayName("controller.getUserStateV2 should return DTO from the service")
    void testGetUserStateV2() {
        // Arrange
        Long userId = 0L;
        Boolean newLogin = true;
        UserStateV2Dto dto = new UserStateV2Dto();
        when(userPermissionsService.getUserStateV2(newLogin))
            .thenReturn(dto);

        // Act
        ResponseEntity<UserStateV2Dto> response = controller.getUserStateV2(userId, newLogin);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("controller.getUserStateV2 should treat null X-New-Login as false")
    void testGetUserStateV2TreatsNullNewLoginAsFalse() {
        // Arrange
        Long userId = 0L;
        Boolean newLogin = null;
        UserStateV2Dto dto = new UserStateV2Dto();
        when(userPermissionsService.getUserStateV2(false))
            .thenReturn(dto);

        // Act
        ResponseEntity<UserStateV2Dto> response = controller.getUserStateV2(userId, newLogin);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
        verify(userPermissionsService).getUserStateV2(false);
    }

    @Test
    @DisplayName("controller.getUserStateV2 should reject non-zero user id with bad request")
    void testGetUserStateV2RejectsNonZeroUserId() {
        // Arrange
        Long userId = 123L;
        Boolean newLogin = true;

        // Act / Assert
        assertThatThrownBy(() -> controller.getUserStateV2(userId, newLogin))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(userPermissionsService);
    }
}
