package uk.gov.hmcts.reform.opal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.opal.entity.UserEntitlementEntity;

import java.util.List;
import java.util.Set;

@Repository
public interface UserEntitlementRepository extends JpaRepository<UserEntitlementEntity, Long>,
    JpaSpecificationExecutor<UserEntitlementEntity> {

    List<UserEntitlementEntity> findAllByBusinessUnitUser_BusinessUnitUserId(String businessUnitUserId);

    /**
     * Finds all entitlements for a given userId.Eagerly fetches all parent objects
     * (the business unit user, the user itself, the business unit, and the function name)
     * in a single query to avoid the N+1 problem.
     */
    @Query("""
        SELECT DISTINCT ue FROM UserEntitlementEntity ue
        JOIN FETCH ue.applicationFunction
        JOIN FETCH ue.businessUnitUser buu
        JOIN FETCH buu.businessUnit
        JOIN FETCH buu.user u
        WHERE u.userId = :userId""")
    Set<UserEntitlementEntity> findAllByUserIdWithFullJoins(Long userId);

}
