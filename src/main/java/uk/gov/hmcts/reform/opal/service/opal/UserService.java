package uk.gov.hmcts.reform.opal.service.opal;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;
import uk.gov.hmcts.opal.common.util.SecurityUtil;
import uk.gov.hmcts.reform.opal.dto.businessevent.AccountActivationInitiatedEvent;
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

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "opal.UserService")
@Qualifier("userService")
@Transactional(readOnly = true)
public class UserService implements UserServiceInterface, UserServiceProxy {

    private final UserRepository userRepository;

    private final RoleService roleService;

    //@Lazy to avoid circular dependency
    @Lazy
    private final BusinessEventService businessEventService;

    private final UserSpecs specs = new UserSpecs();

    private final Clock clock;

    @Override
    public UserEntity getUser(String userId) {
        return userRepository.getReferenceById(Long.valueOf(userId));
    }

    @Override
    public UserEntity getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new
            EntityNotFoundException("User not found with id: " + userId));
    }

    @Override
    public List<UserEntity> searchUsers(UserSearchDto criteria) {
        Page<UserEntity> page = userRepository.findBy(
            specs.findBySearchCriteria(criteria),
            ffq -> ffq.page(Pageable.unpaged()));

        return page.getContent();
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

        List<BusinessUnitUserEntity> alignedBusinessUnitUsers = roleService.getAlignedBusinessUnitUsers(
            user.getUserId(),
            businessUnitIds);

        Set<String> requestedBusinessUnitUserIds = getBusinessUnitUserIds(alignedBusinessUnitUsers);
        Map<String, BusinessUnitUserEntity> businessUnitUsersById = mapBusinessUnitUsersById(alignedBusinessUnitUsers);

        List<BusinessUnitUserRoleEntity> existingAssignments = roleService.getExistingAssignments(
            user.getUserId(),
            roleId);

        logBusinessEvent(existingAssignments, user, role, businessUnitIds);

        roleService.removeObsoleteAssignments(existingAssignments, requestedBusinessUnitUserIds);
        roleService.addMissingAssignments(
            existingAssignments,
            businessUnitUsersById,
            requestedBusinessUnitUserIds,
            role);
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
        return businessUnitUsers.stream().map(BusinessUnitUserEntity::getBusinessUnitUserId)
            .collect(Collectors.toCollection(
                LinkedHashSet::new));
    }

    private Map<String, BusinessUnitUserEntity> mapBusinessUnitUsersById(
        List<BusinessUnitUserEntity> businessUnitUsers) {

        return businessUnitUsers.stream().collect(Collectors.toMap(
            BusinessUnitUserEntity::getBusinessUnitUserId,
            businessUnitUser -> businessUnitUser,
            (left, right) -> left,
            LinkedHashMap::new));
    }

    private void logBusinessEvent(List<BusinessUnitUserRoleEntity> existingAssignments, UserEntity user,
                                  RoleEntity role, Set<Short> businessUnitIds) {

        Set<Short> existingBusinessUnitIds = existingAssignments.stream()
            .map(assignment -> assignment.getBusinessUnitUser().getBusinessUnitId()).collect(
                Collectors.toCollection(LinkedHashSet::new));

        Set<Short> addedBusinessUnitIds = new LinkedHashSet<>(businessUnitIds);
        addedBusinessUnitIds.removeAll(existingBusinessUnitIds);

        Set<Short> removedBusinessUnitIds = new LinkedHashSet<>(existingBusinessUnitIds);
        removedBusinessUnitIds.removeAll(businessUnitIds);

        if (existingAssignments.isEmpty()) {
            log.debug(":logBusinessEvent: assigned business units: {}", addedBusinessUnitIds);
            businessEventService.logBusinessEvent(
                BusinessEventLogType.ROLE_ASSIGNED_TO_USER, user.getUserId(),
                new RoleAssignedToUserEvent(role.getRoleId(), role.getVersionNumber(), addedBusinessUnitIds),
                businessEventService);
        } else {
            log.debug(
                ":logBusinessEvent: amended business units: added {}, removed {}",
                addedBusinessUnitIds,
                removedBusinessUnitIds
            );
            businessEventService.logBusinessEvent(
                BusinessEventLogType.BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED, user.getUserId(),
                new UnitsAssociatedToRoleAmendedEvent(
                    role.getRoleId(), role.getVersionNumber(), addedBusinessUnitIds, removedBusinessUnitIds),
                businessEventService);
        }
    }

    // testing endpoint entry method
    @Transactional
    public void activateUser(long userId, OffsetDateTime activationDate) {
        activateUser(getUser(userId), activationDate);
    }

    @Transactional
    public void activateUser(UserEntity user) {
        activateUser(user, OffsetDateTime.now(clock));
    }

    @Transactional
    public void activateUser(UserEntity user, OffsetDateTime activationDate) {
        user.setActivationDate(activationDate.toLocalDateTime());
        businessEventService.logBusinessEvent(
            BusinessEventLogType.ACCOUNT_ACTIVATION_INITIATED,
            user.getUserId(),
            new AccountActivationInitiatedEvent(activationDate),
            businessEventService);
    }

    @Transactional
    public void refreshUser(UserEntity user) {
        userRepository.refresh(user);
    }

    public UserEntity getAuthenticatedUser() {
        OpalJwtAuthenticationToken authenticationToken =
            SecurityUtil.getOpalJwtAuthenticationTokenForCurrentUser();
        return userRepository.findByUserId(authenticationToken.getUserId())
            .orElseThrow(
                () -> new EntityNotFoundException("User not found with id: " + authenticationToken.getUserId()));
    }
}
