package uk.gov.hmcts.reform.opal.service;

import uk.gov.hmcts.reform.opal.dto.search.BusinessUnitUserSearchDto;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;

import java.util.List;
import java.util.Set;

public interface BusinessUnitUserServiceInterface {

    BusinessUnitUserEntity getBusinessUnitUser(String businessUnitUserId);

    List<BusinessUnitUserEntity> searchBusinessUnitUsers(BusinessUnitUserSearchDto criteria);

    Set<RoleEntity> findAllRolesOfUser(UserEntity user);
}
