package uk.gov.hmcts.reform.opal.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.reform.opal.service.UserPermissionsService;

import static uk.gov.hmcts.reform.opal.util.HttpUtil.buildResponse;

@RestController
@RequestMapping("/v2/users")
@RequiredArgsConstructor
@Slf4j(topic = "opal.UserPermissionsV2Controller")
public class UserPermissionsV2Controller {

    private static final String X_NEW_LOGIN = "X-New-Login";
    private static final MediaType V2_CONTENT_TYPE =
        MediaType.parseMediaType("application/vnd.uk.gov.hmcts.service.resource.v2+json");
    private final UserPermissionsService userPermissionsService;

    @GetMapping("/{userId}/state")
    public ResponseEntity<UserStateV2Dto> getUserStateV2(
        @PathVariable Long userId,
        @RequestHeader(value = X_NEW_LOGIN, required = false) Boolean newLogin,
        @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String contentType)
        throws HttpMediaTypeNotSupportedException {

        if (!isSupportedContentType(contentType)) {
            throw new HttpMediaTypeNotSupportedException(
                contentType == null ? "Missing Content-Type" : contentType);
        }

        log.debug(":GET:getUserStateV2: userId: {}, new login: {}", userId, newLogin);
        return buildResponse(userPermissionsService
                                 .getUserStateV2(userId, newLogin));
    }

    private boolean isSupportedContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }

        try {
            MediaType requestedContentType = MediaType.parseMediaType(contentType);
            return hasSameTypeAndSubtype(requestedContentType, MediaType.APPLICATION_JSON)
                || hasSameTypeAndSubtype(requestedContentType, V2_CONTENT_TYPE);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean hasSameTypeAndSubtype(MediaType requestedContentType, MediaType supportedContentType) {
        return supportedContentType.getType().equalsIgnoreCase(requestedContentType.getType())
            && supportedContentType.getSubtype().equalsIgnoreCase(requestedContentType.getSubtype());
    }
}
