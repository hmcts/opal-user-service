package uk.gov.hmcts.reform.opal.service;

import uk.gov.hmcts.reform.opal.entity.UserEntity;

import java.util.Set;

public interface UserServiceProxy {

    UserEntity getUser(Long userId);

    void addOrReplaceRoleInformationOnUser(UserEntity user, long roleId, Set<Short> businessUnitIds);
}
