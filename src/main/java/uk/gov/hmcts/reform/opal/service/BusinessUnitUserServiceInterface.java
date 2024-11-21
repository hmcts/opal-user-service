package uk.gov.hmcts.reform.opal.service;

import uk.gov.hmcts.reform.opal.dto.search.BusinessUnitUserSearchDto;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;

import java.util.List;

public interface BusinessUnitUserServiceInterface {

    BusinessUnitUserEntity getBusinessUnitUser(String businessUnitUserId);

    List<BusinessUnitUserEntity> searchBusinessUnitUsers(BusinessUnitUserSearchDto criteria);
}
