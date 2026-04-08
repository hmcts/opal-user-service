package uk.gov.hmcts.reform.opal.service;

import org.springframework.security.core.Authentication;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.reform.opal.dto.UserDto;
import uk.gov.hmcts.reform.opal.entity.UserEntity;

public interface UserPermissionsProxy {

    UserEntity getUser(Long userId);

    UserEntity getUser(String token);

    UserEntity getUserV2(Long userId);

    UserEntity getUserV2(String token);

    Long getUserId(Authentication authentication, UserPermissionsProxy proxy);

    @Deprecated // Use getUserStateV2 equivalent
    UserStateDto getUserState(Authentication authentication, UserPermissionsProxy proxy, Boolean newLogin);

    @Deprecated // Use getUserStateV2 equivalent
    UserStateDto getUserState(Long userId, Authentication authentication, UserPermissionsProxy proxy, Boolean newLogin);

    UserStateV2Dto getUserStateV2(UserPermissionsProxy proxy, Boolean newLogin);

    UserStateV2Dto getUserStateV2(Long userId, UserPermissionsProxy proxy,
                                  Boolean newLogin);

    UserStateDto buildUserState(UserEntity user);

    UserDto updateUser(String authHeaderValue, UserPermissionsProxy proxy, String ifMatch);
}
