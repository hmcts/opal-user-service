package uk.gov.hmcts.reform.opal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserRoleEntity;

import java.util.List;

@Repository
public interface BusinessUnitUserRoleRepository extends JpaRepository<BusinessUnitUserRoleEntity, Long>,
    JpaSpecificationExecutor<BusinessUnitUserRoleEntity> {

    List<BusinessUnitUserRoleEntity> findAllByBusinessUnitUser_User_UserIdAndRole_RoleId(Long userId, Long roleId);
}
