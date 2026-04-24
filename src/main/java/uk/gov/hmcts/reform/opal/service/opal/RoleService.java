package uk.gov.hmcts.reform.opal.service.opal;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserRoleEntity;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitUserRepository;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitUserRoleRepository;
import uk.gov.hmcts.reform.opal.repository.RoleRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Qualifier("roleService")
public class RoleService {

    private final RoleRepository roleRepository;

    private final BusinessUnitUserRepository businessUnitUserRepository;

    private final BusinessUnitUserRoleRepository businessUnitUserRoleRepository;

    @Transactional(readOnly = true)
    public RoleEntity requireRole(long roleId) {
        return roleRepository.findById(roleId)
            .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + roleId));
    }

    @Transactional(readOnly = true)
    public List<BusinessUnitUserEntity> getAlignedBusinessUnitUsers(Long userId, Set<Short> businessUnitIds) {
        if (businessUnitIds.isEmpty()) {
            return List.of();
        }

        List<BusinessUnitUserEntity> businessUnitUsers =
            businessUnitUserRepository.findAllByUser_UserIdAndBusinessUnit_BusinessUnitIdIn(userId, businessUnitIds);

        validateNoMissingBusinessUnitUserAlignments(userId, businessUnitIds, businessUnitUsers);

        return businessUnitUsers;
    }


    @Transactional(readOnly = true)
    public List<BusinessUnitUserRoleEntity> getExistingAssignments(Long userId, long roleId) {
        return businessUnitUserRoleRepository.findAllByBusinessUnitUser_User_UserIdAndRole_RoleId(userId, roleId);
    }

    @Transactional
    public void removeObsoleteAssignments(List<BusinessUnitUserRoleEntity> existingAssignments,
                                           Set<String> requestedBusinessUnitUserIds) {
        businessUnitUserRoleRepository.deleteAll(
            existingAssignments.stream()
                .filter(existingAssignment -> getDifference(
                    getExistingBusinessUnitUserIds(existingAssignments), requestedBusinessUnitUserIds)
                    .contains(existingAssignment.getBusinessUnitUserId()))
                .toList()
        );
    }

    @Transactional
    public void addMissingAssignments(
        List<BusinessUnitUserRoleEntity> existingAssignments,
        Map<String, BusinessUnitUserEntity> businessUnitUsersById,
        Set<String> requestedBusinessUnitUserIds,
        RoleEntity role) {

        businessUnitUserRoleRepository.saveAll(
            getDifference(requestedBusinessUnitUserIds, getExistingBusinessUnitUserIds(existingAssignments)).stream()
                .map(businessUnitUserId -> BusinessUnitUserRoleEntity.builder()
                    .businessUnitUser(getRequiredBusinessUnitUser(businessUnitUsersById, businessUnitUserId))
                    .role(role)
                    .build())
                .toList()
        );
    }

    private void validateNoMissingBusinessUnitUserAlignments(
        Long userId, Set<Short> requestedBusinessUnitIds, List<BusinessUnitUserEntity> businessUnitUsers) {

        Set<Short> foundBusinessUnitIds = businessUnitUsers.stream()
            .map(BusinessUnitUserEntity::getBusinessUnitId)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Short> missingBusinessUnitIds = new LinkedHashSet<>(requestedBusinessUnitIds);
        missingBusinessUnitIds.removeAll(foundBusinessUnitIds);

        if (missingBusinessUnitIds.isEmpty()) {
            return;
        }

        throw new IllegalStateException(
            "Missing business unit user alignment for user id %d and business unit ids %s"
                .formatted(userId, missingBusinessUnitIds)
        );
    }

    private BusinessUnitUserEntity getRequiredBusinessUnitUser(
        Map<String, BusinessUnitUserEntity> businessUnitUsersById, String businessUnitUserId) {

        return Optional.ofNullable(businessUnitUsersById.get(businessUnitUserId))
            .orElseThrow(() -> new IllegalStateException(
                "Business unit user alignment disappeared for id: " + businessUnitUserId
            ));
    }

    private Set<String> getExistingBusinessUnitUserIds(List<BusinessUnitUserRoleEntity> existingAssignments) {
        return existingAssignments.stream()
            .map(BusinessUnitUserRoleEntity::getBusinessUnitUserId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> getDifference(Set<String> left, Set<String> right) {
        Set<String> difference = new LinkedHashSet<>(left);
        difference.removeAll(right);
        return difference;
    }
}
