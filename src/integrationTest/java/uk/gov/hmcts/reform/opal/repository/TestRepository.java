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

    long countByBusinessUnitUserId(String businessUnitUserId);

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
