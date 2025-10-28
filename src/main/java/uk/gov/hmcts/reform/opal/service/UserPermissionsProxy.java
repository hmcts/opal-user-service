package uk.gov.hmcts.reform.opal.service;

import uk.gov.hmcts.reform.opal.entity.UserEntity;

public interface UserPermissionsProxy {
    UserEntity getUser(Long userId);
}
