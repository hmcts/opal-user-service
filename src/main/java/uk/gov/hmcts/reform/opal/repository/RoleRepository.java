package uk.gov.hmcts.reform.opal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;

import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long>, JpaSpecificationExecutor<RoleEntity> {

    Set<RoleEntity> findAllByDomain_Id(Integer domainId);

    boolean existsByRoleId(Long roleId);

    boolean existsByRoleIdAndIsActiveTrue(Long roleId);

    long countByRoleIdAndIsActiveTrue(Long roleId);

    Optional<RoleEntity> findByRoleIdAndIsActiveTrue(Long roleId);
}
