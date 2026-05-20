package uk.gov.hmcts.reform.opal.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.opal.entity.BusinessEventLogType;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("integration")
public interface TestRepository extends JpaRepository<BusinessUnitUserEntity, String> {

    interface BusinessUnitUserRow {
        String getBusinessUnitUserId();

        Short getBusinessUnitId();

        Long getUserId();
    }

    @Query("""
        select buu.businessUnitUserId as businessUnitUserId,
               buu.businessUnit.businessUnitId as businessUnitId,
               buu.user.userId as userId
        from BusinessUnitUserEntity buu
        where buu.user.userId = :userId and buu.businessUnitUserId = :businessUnitUserId
        """)
    Optional<BusinessUnitUserRow> findBusinessUnitUserRow(@Param("userId") long userId,
                                                           @Param("businessUnitUserId") String businessUnitUserId);

    @Query("""
        select buu.businessUnitUserId as businessUnitUserId,
               buu.businessUnit.businessUnitId as businessUnitId,
               buu.user.userId as userId
        from BusinessUnitUserEntity buu
        where buu.businessUnitUserId = :businessUnitUserId
        """)
    Optional<BusinessUnitUserRow> findBusinessUnitUserRowByBusinessUnitUserId(
        @Param("businessUnitUserId") String businessUnitUserId
    );

    @Query("""
        select buu.businessUnitUserId as businessUnitUserId,
               buu.businessUnit.businessUnitId as businessUnitId,
               buu.user.userId as userId
        from BusinessUnitUserEntity buu
        where buu.user.userId = :userId
        order by buu.businessUnitUserId
        """)
    List<BusinessUnitUserRow> findBusinessUnitUserRowsByUserId(@Param("userId") long userId);

    long countByBusinessUnitUserId(String businessUnitUserId);

    @Query("""
        select count(ue)
        from UserEntitlementEntity ue
        where ue.businessUnitUser.businessUnitUserId = :businessUnitUserId
        """)
    long countUserEntitlementsByBusinessUnitUserId(@Param("businessUnitUserId") String businessUnitUserId);

    @Query("""
        select count(buur)
        from BusinessUnitUserRoleEntity buur
        where buur.businessUnitUser.businessUnitUserId = :businessUnitUserId
        """)
    long countRoleMappingsByBusinessUnitUserId(@Param("businessUnitUserId") String businessUnitUserId);

    @Query("""
        select buur.businessUnitUser.businessUnit.businessUnitId
        from BusinessUnitUserRoleEntity buur
        where buur.businessUnitUser.user.userId = :userId
          and buur.role.roleId = :roleId
        order by buur.businessUnitUser.businessUnit.businessUnitId
        """)
    List<Short> findAssignedBusinessUnitIdsForUserRole(@Param("userId") long userId, @Param("roleId") long roleId);

    @Query("""
        select buur.role.roleId
        from BusinessUnitUserRoleEntity buur
        where buur.businessUnitUser.businessUnitUserId = :businessUnitUserId
        order by buur.role.roleId
        """)
    List<Long> findAssignedRoleIdsByBusinessUnitUserId(@Param("businessUnitUserId") String businessUnitUserId);

    @Query(value = """
        SELECT DISTINCT permission_name
        FROM business_unit_users buu
        JOIN business_unit_user_roles buur ON buur.business_unit_user_id = buu.business_unit_user_id
        JOIN v_current_roles role ON role.role_id = buur.role_id
        CROSS JOIN LATERAL unnest(role.application_function_list) AS permission_name
        WHERE buu.business_unit_user_id = :businessUnitUserId
        ORDER BY permission_name
        """, nativeQuery = true)
    List<String> findPermissionNamesByBusinessUnitUserId(@Param("businessUnitUserId") String businessUnitUserId);

    @Query("""
        select count(buur)
        from BusinessUnitUserRoleEntity buur
        where buur.businessUnitUser.user.userId = :userId
        """)
    long countRoleAssignments(@Param("userId") long userId);

    @Query("""
        select count(buur)
        from BusinessUnitUserRoleEntity buur
        where buur.businessUnitUser.businessUnitUserId = :businessUnitUserId
          and buur.role.roleId = :roleId
        """)
    long countRoleAssignmentsForBusinessUnitUser(@Param("businessUnitUserId") String businessUnitUserId,
                                                  @Param("roleId") long roleId);

    @Query("""
        select buu.businessUnit.businessUnitId
        from BusinessUnitUserEntity buu
        where buu.user.userId = :userId
        """)
    List<Short> findUserBusinessUnitIds(@Param("userId") long userId);

    @Query("""
        select count(buur)
        from BusinessUnitUserRoleEntity buur
        where buur.businessUnitUser.user.userId = :userId
          and buur.businessUnitUser.businessUnit.businessUnitId = :businessUnitId
          and buur.role.roleId = :roleId
        """)
    long countRoleAssignmentsForUserBusinessUnit(@Param("userId") long userId,
                                                  @Param("businessUnitId") short businessUnitId,
                                                  @Param("roleId") long roleId);

    @Query("""
        select count(buur)
        from BusinessUnitUserRoleEntity buur
        where buur.businessUnitUser.user.userId = :userId
          and buur.role.roleId = :roleId
        """)
    long countRoleAssignmentsForUserRole(@Param("userId") long userId, @Param("roleId") long roleId);

    @Query("""
        select u.activationDate
        from UserEntity u
        where u.userId = :userId
        """)
    Optional<LocalDateTime> findUserActivationDate(@Param("userId") long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
        INSERT INTO business_events (business_event_id, event_type, subject_user_id, initiator_user_id, event_details,
                                     event_date)
        VALUES (:businessEventId, CAST(:eventType AS t_event_type_enum), :subjectUserId, :initiatorUserId,
                CAST(:eventDetails AS json), NOW())
        """, nativeQuery = true)
    void insertBusinessEvent(@Param("businessEventId") long businessEventId,
                             @Param("eventType") String eventType,
                             @Param("subjectUserId") long subjectUserId,
                             @Param("initiatorUserId") long initiatorUserId,
                             @Param("eventDetails") String eventDetails);

    @Query("""
        select be.eventType
        from BusinessEventEntity be
        order by be.businessEventId
        """)
    List<BusinessEventLogType> findLoggedBusinessEventTypes();
}
