package uk.gov.hmcts.reform.opal.service;

import uk.gov.hmcts.reform.opal.dto.search.UserEntitlementSearchDto;
import uk.gov.hmcts.reform.opal.entity.UserEntitlementEntity;

import java.util.List;

public interface UserEntitlementServiceInterface {

    UserEntitlementEntity getUserEntitlement(long userEntitlementId);

    List<UserEntitlementEntity> searchUserEntitlements(UserEntitlementSearchDto criteria);
}
