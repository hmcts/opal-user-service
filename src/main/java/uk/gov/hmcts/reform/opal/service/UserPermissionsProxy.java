package uk.gov.hmcts.reform.opal.service;

import org.springframework.security.core.Authentication;
import uk.gov.hmcts.reform.opal.dto.UserDto;
import uk.gov.hmcts.reform.opal.dto.UserStateDto;
import uk.gov.hmcts.reform.opal.entity.UserEntity;

public interface UserPermissionsProxy {
    UserEntity getUser(Long userId);

    UserEntity getUser(String token);

    UserStateDto getUserState(Authentication authentication, UserPermissionsProxy proxy);

    UserStateDto buildUserState(UserEntity user);

    UserDto updateUser(String authHeaderValue, UserPermissionsProxy proxy, String ifMatch);
}
