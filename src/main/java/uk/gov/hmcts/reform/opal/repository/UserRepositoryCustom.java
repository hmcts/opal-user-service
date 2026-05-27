package uk.gov.hmcts.reform.opal.repository;

import uk.gov.hmcts.reform.opal.entity.UserEntity;

public interface UserRepositoryCustom {

    void refresh(UserEntity user);
}
