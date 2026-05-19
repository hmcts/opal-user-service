package uk.gov.hmcts.reform.opal.service.synchronise;

import uk.gov.hmcts.reform.opal.entity.UserEntity;

public class SynchronisePermissionsException extends RuntimeException {
    private static final String ERROR_TEMPLATE = "Could not synchronise permissions for user %s at stage: %s."
        + " Reason: %s";
    private static final String UNKNOWN_USER = "unknown";

    public SynchronisePermissionsException(UserEntity userEntity, String stage, String reason) {
        super(formatMessage(userEntity, stage, reason));
    }

    public SynchronisePermissionsException(UserEntity userEntity, String stage, String reason, Throwable cause) {
        super(formatMessage(userEntity, stage, reason), cause);
    }

    private static String formatMessage(UserEntity userEntity, String stage, String reason) {
        String userId = userEntity == null || userEntity.getUserId() == null
            ? UNKNOWN_USER
            : userEntity.getUserId().toString();
        return ERROR_TEMPLATE.formatted(userId, stage, reason);
    }
}
