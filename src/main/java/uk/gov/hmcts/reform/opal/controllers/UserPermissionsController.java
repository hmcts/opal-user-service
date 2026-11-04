package uk.gov.hmcts.reform.opal.controllers;

import static uk.gov.hmcts.reform.opal.util.FeatureFlags.RELEASE_1A;
import static uk.gov.hmcts.reform.opal.util.FeatureFlags.RELEASE_1A_1_2;
import static uk.gov.hmcts.reform.opal.util.FeatureFlags.RELEASE_1A_1_2_ENABLED_PROPERTY;
import static uk.gov.hmcts.reform.opal.util.FeatureFlags.RELEASE_1A_ENABLED_PROPERTY;
import static uk.gov.hmcts.reform.opal.util.HttpUtil.buildCreatedResponse;
import static uk.gov.hmcts.reform.opal.util.HttpUtil.buildResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureToggle;
import uk.gov.hmcts.opal.common.user.authorisation.exception.PermissionNotAllowedException;
import uk.gov.hmcts.opal.common.util.SecurityUtil;
import uk.gov.hmcts.reform.opal.authorisation.model.Permissions;
import uk.gov.hmcts.reform.opal.config.LegacyModeConfiguration;
import uk.gov.hmcts.reform.opal.dto.UserDto;
import uk.gov.hmcts.reform.opal.service.UserPermissionsService;
import uk.gov.hmcts.reform.opal.service.rolemapping.MappingFileProcessingResult;
import uk.gov.hmcts.reform.opal.service.rolemapping.UserRoleUploadService;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j(topic = "opal.UserPermissionsController")
public class UserPermissionsController {

    private final LegacyModeConfiguration legacyModeConfiguration;

    private final UserPermissionsService userPermissionsService;
    private final UserRoleUploadService userRoleUploadService;

    @PostMapping()
    @FeatureToggle(feature = RELEASE_1A, defaultValueProperty = RELEASE_1A_ENABLED_PROPERTY)
    public ResponseEntity<UserDto> addUser(@RequestHeader(value = "Authorization") String authHeaderValue) {
        log.debug(":POST:addUser:");
        return buildCreatedResponse(userPermissionsService.addUser(authHeaderValue));
    }

    @PostMapping(value = "role-mapping/upload",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @FeatureToggle(feature = RELEASE_1A_1_2, defaultValueProperty = RELEASE_1A_1_2_ENABLED_PROPERTY)
    public ResponseEntity<MappingFileProcessingResult> uploadRoleMapping(@RequestPart("file") MultipartFile file) {
        log.debug(":POST:uploadRoleMapping:");

        if (!legacyModeConfiguration.isLegacyMode()) {
            throw new FeatureDisabledException("Only accessible in legacy mode");
        }

        if (!SecurityUtil.getOpalJwtAuthenticationTokenForCurrentUser().hasPermission(Permissions.ROLE_MAPPING.getDescriptor())) {
            throw new PermissionNotAllowedException();
        }

        return buildResponse(userRoleUploadService.validateAndUploadRoleMappingCsv(file));
    }

    @PutMapping("/{userId}")
    @FeatureToggle(feature = RELEASE_1A, defaultValueProperty = RELEASE_1A_ENABLED_PROPERTY)
    public ResponseEntity<UserDto> updateUser(@PathVariable Long userId,
                                              @RequestHeader(value = "Authorization") String authHeaderValue,
                                              @RequestHeader(value = "If-Match") String ifMatch) {
        log.debug(":PUT:updateUser:");
        return buildResponse(userPermissionsService
                                 .updateUser(userId, authHeaderValue, ifMatch));
    }

    @PutMapping()
    @FeatureToggle(feature = RELEASE_1A, defaultValueProperty = RELEASE_1A_ENABLED_PROPERTY)
    public ResponseEntity<UserDto> updateUser(@RequestHeader(value = "Authorization") String authHeaderValue,
                                              @RequestHeader(value = "If-Match") String ifMatch) {
        log.debug(":PUT:updateUser:");
        return buildResponse(userPermissionsService.updateUser(authHeaderValue, ifMatch));
    }

}
