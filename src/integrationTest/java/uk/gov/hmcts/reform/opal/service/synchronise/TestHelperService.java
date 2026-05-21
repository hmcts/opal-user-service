package uk.gov.hmcts.reform.opal.service.synchronise;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.opal.entity.BusinessEventLogType;
import uk.gov.hmcts.reform.opal.entity.ApplicationFunctionEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserRoleEntity;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntitlementEntity;
import uk.gov.hmcts.reform.opal.repository.BusinessEventRepository;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitRepository;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitUserRepository;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitUserRoleRepository;
import uk.gov.hmcts.reform.opal.repository.RoleRepository;
import uk.gov.hmcts.reform.opal.repository.TestRepository;
import uk.gov.hmcts.reform.opal.repository.UserRepository;
import uk.gov.hmcts.reform.opal.repository.UserEntitlementRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Service
@Profile("integration")
@RequiredArgsConstructor
public class TestHelperService {

    private final UserRepository userRepository;
    private final BusinessEventRepository businessEventRepository;
    private final BusinessUnitRepository businessUnitRepository;
    private final BusinessUnitUserRepository businessUnitUserRepository;
    private final BusinessUnitUserRoleRepository businessUnitUserRoleRepository;
    private final UserEntitlementRepository userEntitlementRepository;
    private final RoleRepository roleRepository;
    private final TestRepository testRepository;
    private final StringRedisTemplate redisTemplate;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    public long countRoleAssignments(long userId) {
        return testRepository.countRoleAssignments(userId);
    }

    public LocalDateTime getActivationDate(long userId) {
        return testRepository.findUserActivationDate(userId).orElse(null);
    }

    public void insertBusinessUnitUser(String businessUnitUserId, short businessUnitId, long userId) {
        BusinessUnitUserEntity businessUnitUser = BusinessUnitUserEntity.builder()
            .businessUnitUserId(businessUnitUserId)
            .businessUnit(getRequiredBusinessUnit(businessUnitId))
            .user(getRequiredUser(userId))
            .build();
        businessUnitUserRepository.saveAndFlush(businessUnitUser);
    }

    public void insertUserEntitlement(String businessUnitUserId, long applicationFunctionId) {
        UserEntitlementEntity userEntitlement = UserEntitlementEntity.builder()
            .businessUnitUser(getRequiredBusinessUnitUser(businessUnitUserId))
            .applicationFunction(entityManager.getReference(ApplicationFunctionEntity.class, applicationFunctionId))
            .build();
        userEntitlementRepository.saveAndFlush(userEntitlement);
    }

    public void insertBusinessUnitUserRole(String businessUnitUserId, long roleId) {
        BusinessUnitUserRoleEntity businessUnitUserRole = BusinessUnitUserRoleEntity.builder()
            .businessUnitUser(getRequiredBusinessUnitUser(businessUnitUserId))
            .role(getRequiredRole(roleId))
            .build();
        businessUnitUserRoleRepository.saveAndFlush(businessUnitUserRole);
    }

    public void updateBusinessUnitUser(String businessUnitUserId, short businessUnitId, long userId) {
        BusinessUnitUserEntity businessUnitUser = getRequiredBusinessUnitUser(businessUnitUserId);
        businessUnitUser.setBusinessUnit(getRequiredBusinessUnit(businessUnitId));
        businessUnitUser.setUser(getRequiredUser(userId));
        businessUnitUserRepository.saveAndFlush(businessUnitUser);
    }

    public BusinessUnitUserSnapshot getBusinessUnitUserSnapshot(String businessUnitUserId) {
        TestRepository.BusinessUnitUserRow row = testRepository.findBusinessUnitUserRowByBusinessUnitUserId(
            businessUnitUserId
        ).orElseThrow(() -> new IllegalStateException("Missing business unit user fixture: " + businessUnitUserId));
        return new BusinessUnitUserSnapshot(
            row.getBusinessUnitUserId(),
            row.getBusinessUnitId(),
            row.getUserId()
        );
    }

    public BusinessUnitUserSnapshot getBusinessUnitUserSnapshot(long userId, String businessUnitUserId) {
        BusinessUnitUserSnapshot row = getBusinessUnitUserSnapshot(businessUnitUserId);
        if (row.userId() != userId) {
            throw new IllegalStateException(
                "Missing business unit user row for userId=" + userId + ", businessUnitUserId=" + businessUnitUserId
            );
        }
        return row;
    }

    public void assertBusinessUnitUserRow(String businessUnitUserId, short expectedBusinessUnitId, long expectedUserId) {
        BusinessUnitUserSnapshot businessUnitUserRow = getBusinessUnitUserSnapshot(expectedUserId, businessUnitUserId);
        assertThat(businessUnitUserRow.userId()).isEqualTo(expectedUserId);
        assertThat(businessUnitUserRow.businessUnitId()).isEqualTo(expectedBusinessUnitId);
        assertThat(businessUnitUserRow.businessUnitUserId()).isEqualTo(businessUnitUserId);
    }

    public void assertBusinessUnitUserRow(
        String businessUnitUserId,
        short expectedBusinessUnitId,
        long expectedUserId,
        long roleId,
        long expectedRoleCount
    ) {
        assertBusinessUnitUserRow(businessUnitUserId, expectedBusinessUnitId, expectedUserId);
        assertThat(countRoleAssignmentsForBusinessUnitUser(businessUnitUserId, roleId)).isEqualTo(expectedRoleCount);
    }

    public long businessUnitUserCount() {
        return testRepository.count();
    }

    public boolean businessUnitUserExists(String businessUnitUserId) {
        return testRepository.countByBusinessUnitUserId(businessUnitUserId) > 0;
    }

    public long userEntitlementCount(String businessUnitUserId) {
        return testRepository.countUserEntitlementsByBusinessUnitUserId(businessUnitUserId);
    }

    public long userRoleMappingCount(String businessUnitUserId) {
        return testRepository.countRoleMappingsByBusinessUnitUserId(businessUnitUserId);
    }

    public long countRoleAssignmentsForBusinessUnitUser(String businessUnitUserId, long roleId) {
        return testRepository.countRoleAssignmentsForBusinessUnitUser(businessUnitUserId, roleId);
    }

    public Set<Short> getUserBusinessUnitIds(long userId) {
        return new LinkedHashSet<>(testRepository.findUserBusinessUnitIds(userId));
    }

    public long countRoleAssignmentsForUserBusinessUnit(long userId, short businessUnitId, long roleId) {
        return testRepository.countRoleAssignmentsForUserBusinessUnit(userId, businessUnitId, roleId);
    }

    public long countRoleAssignmentsForUserRole(long userId, long roleId) {
        return testRepository.countRoleAssignmentsForUserRole(userId, roleId);
    }

    public void updateUserActivationDate(long userId, LocalDateTime activationDate) {
        UserEntity user = getRequiredUser(userId);
        user.setActivationDate(activationDate);
        userRepository.saveAndFlush(user);
    }

    public void assertUserBusinessUnitIds(long userId, short... expectedBusinessUnitIds) {
        Set<Short> expectedBusinessUnitIdSet = new LinkedHashSet<>();
        for (short businessUnitId : expectedBusinessUnitIds) {
            expectedBusinessUnitIdSet.add(businessUnitId);
        }
        assertThat(getUserBusinessUnitIds(userId)).containsExactlyInAnyOrderElementsOf(expectedBusinessUnitIdSet);
    }

    public void assertUserBusinessUnitRoleCount(long userId, short businessUnitId, long roleId, long expectedRoleCount) {
        assertThat(countRoleAssignmentsForUserBusinessUnit(userId, businessUnitId, roleId)).isEqualTo(expectedRoleCount);
    }

    public void assertUserHasNoActivationDate(long userId) {
        assertThat(getActivationDate(userId)).isNull();
    }

    public void assertUserActivationDateIsToday(long userId) {
        LocalDateTime activationDate = getActivationDate(userId);
        assertThat(activationDate).isNotNull();
        assertThat(activationDate.toLocalDate()).isEqualTo(LocalDate.now());
    }

    public void assertUserActivationDate(long userId, LocalDateTime expectedActivationDate) {
        assertThat(getActivationDate(userId)).isEqualTo(expectedActivationDate);
    }

    public void insertBusinessEvent(
        long businessEventId,
        BusinessEventLogType eventType,
        long subjectUserId,
        long initiatorUserId,
        String eventDetails
    ) {
        testRepository.insertBusinessEvent(
            businessEventId,
            eventType.name(),
            subjectUserId,
            initiatorUserId,
            eventDetails
        );
    }

    public void resetBusinessEventsTable() {
        businessEventRepository.deleteAllInBatch();
    }

    public void clearRoleMappingCacheEntries(String roleMappingUserPrefix) {
        Set<String> cacheKeys = redisTemplate.keys(roleMappingUserPrefix + "*");
        if (cacheKeys != null && !cacheKeys.isEmpty()) {
            redisTemplate.delete(cacheKeys);
        }
    }

    public void setRoleMappingCache(UserEntity user, Map<Long, Set<Short>> roleMapping, String roleMappingUserPrefix)
        throws JsonProcessingException {
        String cacheKey = roleMappingUserPrefix + user.getTokenSubject();
        redisTemplate.opsForValue().set(
            cacheKey,
            objectMapper.writeValueAsString(TestHelperUtil.toCacheRoleMapping(roleMapping))
        );
    }

    public void assertRoleMappingCache(
        UserEntity user,
        Map<Long, Set<Short>> expectedRoleMapping,
        String roleMappingUserPrefix
    ) throws JsonProcessingException {
        String cacheKey = roleMappingUserPrefix + user.getTokenSubject();
        String actualRoleMappingCacheValue = redisTemplate.opsForValue().get(cacheKey);
        assertThat(actualRoleMappingCacheValue).isNotNull();
        assertThat(objectMapper.readTree(actualRoleMappingCacheValue)).isEqualTo(
            objectMapper.readTree(objectMapper.writeValueAsString(TestHelperUtil.toCacheRoleMapping(expectedRoleMapping)))
        );
    }

    public List<BusinessEventLogType> getLoggedBusinessEventTypes() {
        return testRepository.findLoggedBusinessEventTypes();
    }

    public void assertLoggedBusinessEventTypes(BusinessEventLogType... expectedEventTypes) {
        assertThat(getLoggedBusinessEventTypes()).containsExactly(expectedEventTypes);
    }

    public void assertLoggedBusinessEventTypesInAnyOrder(BusinessEventLogType... expectedEventTypes) {
        assertThat(getLoggedBusinessEventTypes()).containsExactlyInAnyOrder(expectedEventTypes);
    }

    public Set<Short> getAssignedBusinessUnitIds(long userId, long roleId) {
        return new LinkedHashSet<>(testRepository.findAssignedBusinessUnitIdsForUserRole(userId, roleId));
    }

    public Set<String> getReturnedPermissionNames(String businessUnitUserId) {
        return new LinkedHashSet<>(testRepository.findPermissionNamesByBusinessUnitUserId(businessUnitUserId));
    }

    public String formatPermissionsSnapshotAsJson(long userId) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(getPermissionsSnapshot(userId));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialise permissions snapshot for user " + userId, exception);
        }
    }

    private List<Map<String, Object>> getPermissionsSnapshot(long userId) {
        return testRepository.findBusinessUnitUserRowsByUserId(userId).stream()
            .map(row -> {
                Map<String, Object> businessUnitSnapshot = new LinkedHashMap<>();
                businessUnitSnapshot.put("business_unit_id", row.getBusinessUnitId());
                businessUnitSnapshot.put("roles", getAssignedRoleIds(row.getBusinessUnitUserId()));
                return businessUnitSnapshot;
            })
            .toList();
    }

    private Set<Long> getAssignedRoleIds(String businessUnitUserId) {
        return new LinkedHashSet<>(testRepository.findAssignedRoleIdsByBusinessUnitUserId(businessUnitUserId));
    }

    private UserEntity getRequiredUser(long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("Missing user fixture: " + userId));
    }

    private BusinessUnitEntity getRequiredBusinessUnit(short businessUnitId) {
        return businessUnitRepository.findById(businessUnitId)
            .orElseThrow(() -> new IllegalStateException("Missing business unit fixture: " + businessUnitId));
    }

    private BusinessUnitUserEntity getRequiredBusinessUnitUser(String businessUnitUserId) {
        return businessUnitUserRepository.findById(businessUnitUserId)
            .orElseThrow(() -> new IllegalStateException("Missing business unit user fixture: " + businessUnitUserId));
    }

    private RoleEntity getRequiredRole(long roleId) {
        return roleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalStateException("Missing role fixture: " + roleId));
    }

    public record BusinessUnitUserSnapshot(String businessUnitUserId, short businessUnitId, long userId) {
    }
}
