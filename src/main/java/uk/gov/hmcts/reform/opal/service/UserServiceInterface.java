package uk.gov.hmcts.reform.opal.service;

import uk.gov.hmcts.reform.opal.dto.search.UserSearchDto;
import uk.gov.hmcts.reform.opal.entity.UserEntity;

import java.util.List;

public interface UserServiceInterface {

    UserEntity getUser(String userId);

    List<UserEntity> searchUsers(UserSearchDto criteria);
}
