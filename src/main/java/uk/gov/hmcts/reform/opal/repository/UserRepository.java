package uk.gov.hmcts.reform.opal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.opal.entity.UserEntity;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long>,
    JpaSpecificationExecutor<UserEntity> {

    UserEntity findByUsername(String username);

    Optional<UserEntity> findByTokenSubject(String tokenSubject);

    @EntityGraph(attributePaths = {
        "businessUnitUsers",
        "businessUnitUsers.businessUnit",
        "businessUnitUsers.businessUnit.domain",
        "businessUnitUsers.businessUnitUserRoleList",
        "businessUnitUsers.businessUnitUserRoleList.role"
    })
    @Query("select u from UserEntity u where u.userId = ?1")
    Optional<UserEntity> findIdWithPermissions(Long id);

    @EntityGraph(attributePaths = {
        "businessUnitUsers",
        "businessUnitUsers.businessUnit",
        "businessUnitUsers.businessUnit.domain",
        "businessUnitUsers.businessUnitUserRoleList",
        "businessUnitUsers.businessUnitUserRoleList.role"
    })
    @Query("select u from UserEntity u where u.tokenSubject = ?1")
    Optional<UserEntity> findByTokenSubjectWithPermissions(String subject);
}
