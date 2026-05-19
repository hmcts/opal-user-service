package uk.gov.hmcts.reform.opal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;

import java.util.List;
import java.util.Set;

@Repository
public interface BusinessUnitUserRepository extends JpaRepository<BusinessUnitUserEntity, String>,
    JpaSpecificationExecutor<BusinessUnitUserEntity> {

    List<BusinessUnitUserEntity> findAllByUser_UserId(Long userId);

    List<BusinessUnitUserEntity> findAllByUser_UserIdAndBusinessUnit_BusinessUnitIdIn(
        Long userId, Set<Short> businessUnitIds);

    @Query("""
        select distinct bur.role
        from BusinessUnitUserEntity buu
        join buu.businessUnitUserRoleList bur
        where buu.user.userId = :userId
        """)
    Set<RoleEntity> findDistinctRolesByUserId(@Param("userId") Long userId);
}
