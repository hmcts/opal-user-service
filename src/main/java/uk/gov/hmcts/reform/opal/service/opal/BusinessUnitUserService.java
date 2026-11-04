package uk.gov.hmcts.reform.opal.service.opal;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUserV2;
import uk.gov.hmcts.reform.opal.dto.search.BusinessUnitUserSearchDto;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitUserRepository;
import uk.gov.hmcts.reform.opal.repository.jpa.BusinessUnitUserSpecs;
import uk.gov.hmcts.reform.opal.service.BusinessUnitUserServiceInterface;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Qualifier("businessUnitUserService")
@Transactional(readOnly = true)
public class BusinessUnitUserService implements BusinessUnitUserServiceInterface {

    private final BusinessUnitUserRepository businessUnitUserRepository;

    private final BusinessUnitUserSpecs specs = new BusinessUnitUserSpecs();

    @Override
    public BusinessUnitUserEntity getBusinessUnitUser(String businessUnitUserId) {
        return businessUnitUserRepository.getReferenceById(businessUnitUserId);
    }

    @Override
    public List<BusinessUnitUserEntity> searchBusinessUnitUsers(BusinessUnitUserSearchDto criteria) {
        Page<BusinessUnitUserEntity> page = businessUnitUserRepository
            .findBy(specs.findBySearchCriteria(criteria),
                ffq -> ffq.page(Pageable.unpaged()));

        return page.getContent();
    }

    @Override
    public Set<RoleEntity> findAllRolesOfUser(UserEntity user) {
        return businessUnitUserRepository.findDistinctRolesByUserId(user.getUserId());
    }

    /**
     * Return business unit memberships for the user without loading roles or permissions.
     */
    public Set<BusinessUnitUserV2> getLimitedBusinessUnitPermissionsByUserId(Long userId) {
        List<BusinessUnitUserEntity> buuList = businessUnitUserRepository.findAllByUser_UserId(userId);

        return buuList.stream().map(buu -> BusinessUnitUserV2.builder()
            .businessUnitUserId(buu.getBusinessUnitUserId())
            .businessUnitId(buu.getBusinessUnit().getBusinessUnitId())
            .permissions(Collections.emptySet())
            .build()).collect(Collectors.toSet());

    }

}
