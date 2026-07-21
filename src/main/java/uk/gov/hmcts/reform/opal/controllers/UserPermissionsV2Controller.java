package uk.gov.hmcts.reform.opal.controllers;

import static uk.gov.hmcts.reform.opal.util.FeatureFlags.RELEASE_1A;
import static uk.gov.hmcts.reform.opal.util.FeatureFlags.RELEASE_1A_ENABLED_PROPERTY;
import static uk.gov.hmcts.reform.opal.util.HttpUtil.buildResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureToggle;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.reform.opal.service.UserPermissionsService;

import java.util.UUID;

@RestController
@RequestMapping("/v2/users")
@RequiredArgsConstructor
@Slf4j(topic = "opal.UserPermissionsV2Controller")
public class UserPermissionsV2Controller {

    private static final String X_NEW_LOGIN = "X-New-Login";
    private static final Long CURRENT_USER_ID = 0L;
    private final UserPermissionsService userPermissionsService;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/{userId}/state")
    @FeatureToggle(feature = RELEASE_1A, defaultValueProperty = RELEASE_1A_ENABLED_PROPERTY)
    public ResponseEntity<UserStateV2Dto> getUserStateV2(
        @PathVariable Long userId,
        @RequestHeader(value = X_NEW_LOGIN, required = false) Boolean newLogin) {

        log.info("\n\n UserPermissionV2Controller getUserStateV2: \n Writing to locked User table.\n\n");
        jdbcTemplate.update("""
                INSERT INTO users (
                    token_preferred_username,
                    description,
                    created_date,
                    token_subject,
                    token_name,
                    version_number
                )
                VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?, ?)
                """,
            "fmea-db-write@hmcts.net",
            "FMEA users table insert test",
            "fmea-users-table-insert-" + UUID.randomUUID(),
            "FMEA Users Table Insert",
            0L);

        log.debug(":GET:getUserStateV2: userId: {}, new login: {}", userId, newLogin);
        if (!CURRENT_USER_ID.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only userId 0 is supported.");
        }
        boolean isNewLogin = Boolean.TRUE.equals(newLogin);
        return buildResponse(userPermissionsService.getUserStateV2(isNewLogin));
    }
}
