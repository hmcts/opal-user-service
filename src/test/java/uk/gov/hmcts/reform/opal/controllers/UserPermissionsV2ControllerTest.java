package uk.gov.hmcts.reform.opal.controllers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.reform.opal.service.UserPermissionsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@Slf4j(topic = "opal.UserPermissionsV2ControllerTest")
@ExtendWith(MockitoExtension.class)
class UserPermissionsV2ControllerTest {

    private static final String V1_CONTENT_TYPE = "application/vnd.uk.gov.hmcts.service.resource.v1+json";
    private static final String V2_CONTENT_TYPE = "application/vnd.uk.gov.hmcts.service.resource.v2+json";

    @Mock
    private UserPermissionsService userPermissionsService;

    @InjectMocks
    private UserPermissionsV2Controller controller;

    @Test
    @DisplayName("controller.getUserStateV2 should return DTO from the service")
    void testGetUserStateV2() throws HttpMediaTypeNotSupportedException {
        // Arrange
        Long userId = 123L;
        Boolean newLogin = true;
        UserStateV2Dto dto = new UserStateV2Dto();
        when(userPermissionsService.getUserStateV2(userId, newLogin))
            .thenReturn(dto);

        // Act
        ResponseEntity<UserStateV2Dto> response = controller.getUserStateV2(
            userId, newLogin, MediaType.APPLICATION_JSON_VALUE);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("controller.getUserStateV2 should allow the V2 vendor content type")
    void testGetUserStateV2_allowsV2ContentType() throws HttpMediaTypeNotSupportedException {
        // Arrange
        Long userId = 123L;
        Boolean newLogin = true;
        UserStateV2Dto dto = new UserStateV2Dto();
        when(userPermissionsService.getUserStateV2(userId, newLogin))
            .thenReturn(dto);

        // Act
        ResponseEntity<UserStateV2Dto> response = controller.getUserStateV2(userId, newLogin, V2_CONTENT_TYPE);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("controller.getUserStateV2 should reject the V1 vendor content type")
    void testGetUserStateV2_rejectsV1ContentType() {
        assertThatThrownBy(() -> controller.getUserStateV2(123L, true, V1_CONTENT_TYPE))
            .isInstanceOf(HttpMediaTypeNotSupportedException.class);
    }
}
