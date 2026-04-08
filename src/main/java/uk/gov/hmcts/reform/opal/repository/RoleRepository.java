package uk.gov.hmcts.reform.opal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;

import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Short>,
    JpaSpecificationExecutor<RoleEntity> {

    @Query("select r from RoleEntity r where r.domain.id = :domainId")
    Set<RoleEntity> findAllByDomainId(Long domainId);
}
