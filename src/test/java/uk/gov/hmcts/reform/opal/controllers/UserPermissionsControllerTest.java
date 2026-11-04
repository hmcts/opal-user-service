package uk.gov.hmcts.reform.opal.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.exception.PermissionNotAllowedException;
import uk.gov.hmcts.reform.opal.authorisation.model.Permissions;
import uk.gov.hmcts.reform.opal.config.LegacyModeConfiguration;
import uk.gov.hmcts.reform.opal.dto.UserDto;
import uk.gov.hmcts.reform.opal.service.UserPermissionsService;
import uk.gov.hmcts.reform.opal.service.rolemapping.MappingFileProcessingResult;
import uk.gov.hmcts.reform.opal.service.rolemapping.UserRoleUploadService;

@Slf4j(topic = "opal.UserPermissionsControllerTest")
@ExtendWith(MockitoExtension.class)
class UserPermissionsControllerTest {

    @Mock
    private UserPermissionsService userPermissionsService;

    @Mock
    private LegacyModeConfiguration legacyModeConfiguration;

    @Mock
    private UserRoleUploadService userRoleUploadService;

    @InjectMocks
    private UserPermissionsController controller;

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
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
    void uploadRoleMapping_throwsWhenLegacyModeIsDisabled() {
        MockMultipartFile file = new MockMultipartFile("file", "roles.csv", "text/csv", "".getBytes());
        given(legacyModeConfiguration.isLegacyMode()).willReturn(false);

        assertThrows(FeatureDisabledException.class, () -> controller.uploadRoleMapping(file));

        verify(userRoleUploadService, never()).validateAndUploadRoleMappingCsv(any());
    }

    @Test
    void uploadRoleMapping_throwsWhenUserLacksRoleMappingPermission() {
        MockMultipartFile file = new MockMultipartFile("file", "roles.csv", "text/csv", "".getBytes());
        given(legacyModeConfiguration.isLegacyMode()).willReturn(true);
        setCurrentUserPermission(false);

        assertThrows(PermissionNotAllowedException.class, () -> controller.uploadRoleMapping(file));

        verify(userRoleUploadService, never()).validateAndUploadRoleMappingCsv(any());
    }

    @Test
    void uploadRoleMapping_uploadsFileForAuthorizedUser() {
        MockMultipartFile file = new MockMultipartFile("file", "roles.csv", "text/csv", "".getBytes());
        MappingFileProcessingResult expected = new MappingFileProcessingResult(List.of(), Set.of(), Set.of());
        given(legacyModeConfiguration.isLegacyMode()).willReturn(true);
        setCurrentUserPermission(true);
        given(userRoleUploadService.validateAndUploadRoleMappingCsv(file)).willReturn(expected);

        ResponseEntity<MappingFileProcessingResult> response = controller.uploadRoleMapping(file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
        verify(userRoleUploadService).validateAndUploadRoleMappingCsv(file);
    }

    private void setCurrentUserPermission(boolean hasPermission) {
        OpalJwtAuthenticationToken authentication = mock(OpalJwtAuthenticationToken.class);
        doReturn(hasPermission).when(authentication)
            .hasPermission(any(Permissions.ROLE_MAPPING.getDescriptor().getClass()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void testUpdateUser_withId() {
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
    void testUpdateUser_withoutId() {
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
