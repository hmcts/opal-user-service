package uk.gov.hmcts.reform.opal.controllers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest
    @ValueSource(strings = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8",
        V2_CONTENT_TYPE,
        V2_CONTENT_TYPE + ";charset=UTF-8"
    })
    @DisplayName("controller.getUserStateV2 should return DTO from the service")
    void testGetUserStateV2(String contentType) throws HttpMediaTypeNotSupportedException {
        // Arrange
        Long userId = 123L;
        Boolean newLogin = true;
        UserStateV2Dto dto = new UserStateV2Dto();
        when(userPermissionsService.getUserStateV2(userId, newLogin))
            .thenReturn(dto);

        // Act
        ResponseEntity<UserStateV2Dto> response = controller.getUserStateV2(userId, newLogin, contentType);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        " ",
        V1_CONTENT_TYPE,
        "application/xml",
        "text/plain",
        "not-a-media-type"
    })
    @DisplayName("controller.getUserStateV2 should reject unsupported content types")
    void testGetUserStateV2_rejectsUnsupportedContentTypes(String contentType) {
        assertThatThrownBy(() -> controller.getUserStateV2(123L, true, contentType))
            .isInstanceOf(HttpMediaTypeNotSupportedException.class);
    }
}
