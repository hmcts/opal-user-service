package uk.gov.hmcts.reform.opal.service.opal;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;
import uk.gov.hmcts.reform.opal.dto.businessevent.RoleAssignedToUserEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.RoleUnassignedFromUserEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.UnitsAssociatedToRoleAmendedEvent;
import uk.gov.hmcts.reform.opal.dto.search.UserSearchDto;
import uk.gov.hmcts.reform.opal.entity.BusinessEventLogType;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserRoleEntity;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.repository.UserRepository;
import uk.gov.hmcts.reform.opal.repository.jpa.UserSpecs;
import uk.gov.hmcts.reform.opal.service.BusinessEventService;
import uk.gov.hmcts.reform.opal.service.UserServiceInterface;
import uk.gov.hmcts.reform.opal.service.UserServiceProxy;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "opal.UserService")
@Qualifier("userService")
@Transactional(readOnly = true)
public class UserService implements UserServiceInterface, UserServiceProxy {

    private final UserRepository userRepository;

    private final BusinessUnitUserService businessUnitUserService;

    private final RoleService roleService;

    private final BusinessEventService businessEventService;

    private final UserSpecs specs = new UserSpecs();

    @Override
    public UserEntity getUser(String userId) {
        return userRepository.getReferenceById(Long.valueOf(userId));
    }

    @Override
    public UserEntity getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
    }

    @Override
    public List<UserEntity> searchUsers(UserSearchDto criteria) {
        Page<UserEntity> page = userRepository
            .findBy(specs.findBySearchCriteria(criteria),
                    ffq -> ffq.page(Pageable.unpaged()));

        return page.getContent();
    }

    /**
     * Retrieves a UserState object by starting with multiple queries against 3 different repositories.
     * During some limited developer testing, this method was less performant than the similar method
     * in the UserEntitlementService, but will still return a UserState even if no Entitlements exist for that user,
     * but the User <i>does</i> exist in the table.
     */
    @Cacheable(cacheNames = "users", key = "#username")
    public UserState getUserStateByUsername(String username) {
        UserEntity user = userRepository.findByUsername(username);
        return UserState.builder()
            .userId(user.getUserId())
            .userName(user.getUsername())
            .businessUnitUser(
                businessUnitUserService.getAuthorisationBusinessUnitPermissionsByUserId(user.getUserId()))
            .build();
    }

    /**
     * Return a 'cut down' UserState object that that only tries to populate Business Unit Users but not Permissions.
     * The assumption is that previous code has attempted to retrieve a UserState object via a query against
     * the UserEntitlementService, but failed. This could be because of a lack of Entitlements associated with
     * a BusinessUnitUnit, or a lack of BusinessUnitUsers associated with this user. So assuming there
     * is a valid User for the given username, then this method will return a non-null object.
     */
    public Optional<UserState> getLimitedUserStateByUsername(String username) {
        Optional<UserEntity> userEntity = Optional.ofNullable(userRepository.findByUsername(username));

        return userEntity.map(u -> UserState.builder()
            .userId(u.getUserId())
            .userName(u.getUsername())
            .businessUnitUser(
                businessUnitUserService.getLimitedBusinessUnitPermissionsByUserId(u.getUserId()))
            .build());
    }

    @Transactional
    public void addOrReplaceRoleInformationOnUser(
        long userId, long roleId, Set<Short> businessUnitIds, UserServiceProxy proxy) {
        proxy.addOrReplaceRoleInformationOnUser(proxy.getUser(userId), roleId, businessUnitIds);
    }

    @Override
    @Transactional
    public void addOrReplaceRoleInformationOnUser(UserEntity user, long roleId, Set<Short> businessUnitIds) {

        RoleEntity role = roleService.requireRole(roleId);

        List<BusinessUnitUserEntity> alignedBusinessUnitUsers =
            roleService.getAlignedBusinessUnitUsers(user.getUserId(), businessUnitIds);

        Set<String> requestedBusinessUnitUserIds = getBusinessUnitUserIds(alignedBusinessUnitUsers);
        Map<String, BusinessUnitUserEntity> businessUnitUsersById =
            mapBusinessUnitUsersById(alignedBusinessUnitUsers);

        List<BusinessUnitUserRoleEntity> existingAssignments =
            roleService.getExistingAssignments(user.getUserId(), roleId);

        logBusinessEvent(existingAssignments, user, roleId, businessUnitIds);

        roleService.removeObsoleteAssignments(existingAssignments, requestedBusinessUnitUserIds);
        roleService.addMissingAssignments(
            existingAssignments, businessUnitUsersById, requestedBusinessUnitUserIds, role);
    }

    @Transactional
    public void deleteRoleFromUser(long userId, long roleId, UserServiceProxy proxy) {
        proxy.deleteRoleFromUser(proxy.getUser(userId), roleId);
    }

    @Override
    @Transactional
    public void deleteRoleFromUser(UserEntity user, long roleId) {
        RoleEntity role = roleService.requireRole(roleId);

        List<BusinessUnitUserRoleEntity> existingAssignments =
            roleService.getExistingAssignments(user.getUserId(), roleId);

        if (existingAssignments.isEmpty()) {
            return;
        }

        Set<Short> removedBusinessUnitIds = existingAssignments.stream()
            .map(assignment -> assignment.getBusinessUnitUser().getBusinessUnitId())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        roleService.removeAssignments(existingAssignments);

        businessEventService.logBusinessEvent(
            BusinessEventLogType.ROLE_UNASSIGNED_FROM_USER,
            user.getUserId(),
            new RoleUnassignedFromUserEvent(roleId, removedBusinessUnitIds, role.getVersionNumber()),
            businessEventService
        );
    }

    private Set<String> getBusinessUnitUserIds(List<BusinessUnitUserEntity> businessUnitUsers) {
        return businessUnitUsers.stream()
            .map(BusinessUnitUserEntity::getBusinessUnitUserId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<String, BusinessUnitUserEntity> mapBusinessUnitUsersById(
        List<BusinessUnitUserEntity> businessUnitUsers) {

        return businessUnitUsers.stream()
            .collect(Collectors.toMap(
                BusinessUnitUserEntity::getBusinessUnitUserId,
                businessUnitUser -> businessUnitUser,
                (left, right) -> left,
                LinkedHashMap::new));
    }

    private void logBusinessEvent(List<BusinessUnitUserRoleEntity> existingAssignments, UserEntity user,
        long roleId, Set<Short> businessUnitIds) {

        Set<Short> existingBusinessUnitIds = existingAssignments.stream()
            .map(assignment -> assignment.getBusinessUnitUser().getBusinessUnitId())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Short> addedBusinessUnitIds = new LinkedHashSet<>(businessUnitIds);
        addedBusinessUnitIds.removeAll(existingBusinessUnitIds);

        Set<Short> removedBusinessUnitIds = new LinkedHashSet<>(existingBusinessUnitIds);
        removedBusinessUnitIds.removeAll(businessUnitIds);

        if (existingAssignments.isEmpty()) {
            log.debug(":logBusinessEvent: assigned business units: {}", addedBusinessUnitIds);
            businessEventService.logBusinessEvent(
                BusinessEventLogType.ROLE_ASSIGNED_TO_USER, user.getUserId(),
                new RoleAssignedToUserEvent(roleId, addedBusinessUnitIds),
                businessEventService);
        } else {
            log.debug(
                ":logBusinessEvent: amended business units: added {}, removed {}",
                addedBusinessUnitIds, removedBusinessUnitIds);
            businessEventService.logBusinessEvent(
                BusinessEventLogType.BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED, user.getUserId(),
                new UnitsAssociatedToRoleAmendedEvent(roleId, addedBusinessUnitIds, removedBusinessUnitIds),
                businessEventService);
        }
    }
}
