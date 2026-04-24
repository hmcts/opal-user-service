package uk.gov.hmcts.reform.opal.service.opal;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserRoleEntity;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitUserRepository;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitUserRoleRepository;
import uk.gov.hmcts.reform.opal.repository.RoleRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private BusinessUnitUserRepository businessUnitUserRepository;

    @Mock
    private BusinessUnitUserRoleRepository businessUnitUserRoleRepository;

    @InjectMocks
    private RoleService roleService;

    @Test
    void requireRole_whenRoleExists_doesNotThrow() {
        when(roleRepository.findById(101L))
            .thenReturn(java.util.Optional.of(RoleEntity.builder().roleId(101L).build()));

        assertDoesNotThrow(() -> roleService.requireRole(101L));
    }

    @Test
    void requireRole_whenRoleDoesNotExist_throwsEntityNotFoundException() {
        when(roleRepository.findById(101L)).thenReturn(java.util.Optional.empty());

        EntityNotFoundException exception = assertThrows(
            EntityNotFoundException.class,
            () -> roleService.requireRole(101L)
        );

        assertEquals("Role not found with id: 101", exception.getMessage());
    }

    @Test
    void getAlignedBusinessUnitUsers_whenAllBusinessUnitsAreAligned_returnsBusinessUnitUsers() {
        BusinessUnitUserEntity buu1 = businessUnitUser("BU001", 1L, (short) 11);
        BusinessUnitUserEntity buu2 = businessUnitUser("BU002", 1L, (short) 12);
        Set<Short> requestedBusinessUnitIds = Set.of((short) 11, (short) 12);

        when(
            businessUnitUserRepository.findAllByUser_UserIdAndBusinessUnit_BusinessUnitIdIn(
                1L,
                requestedBusinessUnitIds
            )
        )
            .thenReturn(List.of(buu1, buu2));

        List<BusinessUnitUserEntity> result =
            roleService.getAlignedBusinessUnitUsers(1L, requestedBusinessUnitIds);

        assertEquals(List.of(buu1, buu2), result);
    }

    @Test
    void getAlignedBusinessUnitUsers_whenAlignmentIsMissing_throwsIllegalStateException() {
        BusinessUnitUserEntity buu1 = businessUnitUser("BU001", 1L, (short) 11);
        Set<Short> requestedBusinessUnitIds = Set.of((short) 11, (short) 12);

        when(
            businessUnitUserRepository.findAllByUser_UserIdAndBusinessUnit_BusinessUnitIdIn(
                1L,
                requestedBusinessUnitIds
            )
        )
            .thenReturn(List.of(buu1));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> roleService.getAlignedBusinessUnitUsers(1L, requestedBusinessUnitIds)
        );

        assertEquals(
            "Missing business unit user alignment for user id 1 and business unit ids [12]",
            exception.getMessage()
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    void removeObsoleteAssignments_deletesOnlyAssignmentsNotInRequestedSet() {
        BusinessUnitUserRoleEntity buur1 = assignment("BU001", 1L, 201L);
        BusinessUnitUserRoleEntity buur2 = assignment("BU002", 1L, 201L);
        BusinessUnitUserRoleEntity buur3 = assignment("BU003", 1L, 201L);

        roleService.removeObsoleteAssignments(
            List.of(buur1, buur2, buur3),
            Set.of("BU001", "BU004", "BU005")
        );

        ArgumentCaptor<List<BusinessUnitUserRoleEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(businessUnitUserRoleRepository).deleteAll(captor.capture());

        List<BusinessUnitUserRoleEntity> deletedAssignments = captor.getValue();
        assertEquals(2, deletedAssignments.size());
        assertEquals(Set.of("BU002", "BU003"), deletedAssignments.stream()
            .map(BusinessUnitUserRoleEntity::getBusinessUnitUserId)
            .collect(java.util.stream.Collectors.toSet()));
    }

    @SuppressWarnings("unchecked")
    @Test
    void addMissingAssignments_savesOnlyAssignmentsNotAlreadyPresent() {
        BusinessUnitUserEntity buu1 = businessUnitUser("BU001", 1L, (short) 11);
        BusinessUnitUserEntity buu4 = businessUnitUser("BU004", 1L, (short) 14);
        BusinessUnitUserEntity buu5 = businessUnitUser("BU005", 1L, (short) 15);
        RoleEntity role = RoleEntity.builder().roleId(201L).build();

        BusinessUnitUserRoleEntity existingAssignment = assignment("BU001", 1L, 201L);

        roleService.addMissingAssignments(
            List.of(existingAssignment),
            Map.of("BU001", buu1, "BU004", buu4, "BU005", buu5),
            Set.of("BU001", "BU004", "BU005"),
            role
        );

        ArgumentCaptor<List<BusinessUnitUserRoleEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(businessUnitUserRoleRepository).saveAll(captor.capture());

        List<BusinessUnitUserRoleEntity> savedAssignments = captor.getValue();
        assertEquals(2, savedAssignments.size());
        assertEquals(Set.of("BU004", "BU005"), savedAssignments.stream()
            .map(BusinessUnitUserRoleEntity::getBusinessUnitUserId)
            .collect(java.util.stream.Collectors.toSet()));
        assertEquals(Set.of(201L), savedAssignments.stream()
            .map(BusinessUnitUserRoleEntity::getRoleId)
            .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void addMissingAssignments_whenRequiredBusinessUnitUserIsMissing_throwsIllegalStateException() {
        BusinessUnitUserRoleEntity existingAssignment = assignment("BU001", 1L, 201L);
        RoleEntity role = RoleEntity.builder().roleId(201L).build();

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> roleService.addMissingAssignments(
                List.of(existingAssignment),
                Map.of("BU001", businessUnitUser("BU001", 1L, (short) 11)),
                Set.of("BU001", "BU004"),
                role
            )
        );

        assertEquals("Business unit user alignment disappeared for id: BU004", exception.getMessage());
        verify(businessUnitUserRoleRepository, never()).saveAll(any());
    }

    private BusinessUnitUserEntity businessUnitUser(String businessUnitUserId, Long userId, Short businessUnitId) {
        return BusinessUnitUserEntity.builder()
            .businessUnitUserId(businessUnitUserId)
            .user(UserEntity.builder().userId(userId).build())
            .businessUnit(BusinessUnitEntity.builder().businessUnitId(businessUnitId).build())
            .build();
    }

    private BusinessUnitUserRoleEntity assignment(String businessUnitUserId, Long userId, Long roleId) {
        return BusinessUnitUserRoleEntity.builder()
            .businessUnitUser(businessUnitUser(businessUnitUserId, userId, (short) 11))
            .role(RoleEntity.builder().roleId(roleId).build())
            .build();
    }
}
