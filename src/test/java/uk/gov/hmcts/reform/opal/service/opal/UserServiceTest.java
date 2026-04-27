package uk.gov.hmcts.reform.opal.service.opal;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;
import uk.gov.hmcts.reform.opal.dto.businessevent.AccountActivationInitiatedEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.RoleAssignedToUserEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.UnitsAssociatedToRoleAmendedEvent;
import uk.gov.hmcts.reform.opal.dto.search.UserSearchDto;
import uk.gov.hmcts.reform.opal.entity.BusinessEventEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessEventLogType;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserRoleEntity;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.repository.UserRepository;
import uk.gov.hmcts.reform.opal.service.BusinessEventService;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BusinessUnitUserService businessUnitUserService;

    @Mock
    private RoleService roleService;

    @Mock
    private BusinessEventService businessEventService;

    @InjectMocks
    private UserService userService;

    @Test
    void testGetUser() {
        // Arrange

        UserEntity userEntity = UserEntity.builder().build();
        when(userRepository.getReferenceById(any())).thenReturn(userEntity);

        // Act
        UserEntity result = userService.getUser("1");

        // Assert
        assertNotNull(result);

    }

    @Test
    void testGetUserById() {
        UserEntity userEntity = UserEntity.builder().userId(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(userEntity));

        UserEntity result = userService.getUser(1L);

        assertEquals(userEntity, result);
    }

    @Test
    void testGetUserById_whenMissing_throwsEntityNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
            EntityNotFoundException.class,
            () -> userService.getUser(99L)
        );

        assertEquals("User not found with id: 99", exception.getMessage());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testSearchUsers() {
        // Arrange
        JpaSpecificationExecutor.SpecificationFluentQuery ffq =
            Mockito.mock(JpaSpecificationExecutor.SpecificationFluentQuery.class);

        UserEntity userEntity = UserEntity.builder().build();
        Page<UserEntity> mockPage = new PageImpl<>(List.of(userEntity), Pageable.unpaged(), 999L);
        when(userRepository.findBy(any(Specification.class), any())).thenAnswer(iom -> {
            iom.getArgument(1, Function.class).apply(ffq);
            return mockPage;
        });

        // Act
        List<UserEntity> result = userService.searchUsers(UserSearchDto.builder().build());

        // Assert
        assertEquals(List.of(userEntity), result);

    }

    @Test
    void testGetUserStateByUsername() {
        // Arrange
        UserEntity userEntity = UserEntity.builder().userId(123L).username("John Smith").build();
        when(userRepository.findByUsername(any())).thenReturn(userEntity);
        when(businessUnitUserService.getAuthorisationBusinessUnitPermissionsByUserId(any()))
            .thenReturn(Collections.emptySet());

        // Act
        UserState result = userService.getUserStateByUsername("");

        // Assert
        assertNotNull(result);
        assertEquals(123L, result.getUserId());
        assertEquals("John Smith", result.getUserName());

    }

    @Test
    void testGetLimitedUserStateByUsername() {
        // Arrange
        UserEntity userEntity = UserEntity.builder().userId(123L).username("John Smith").build();
        when(userRepository.findByUsername(any())).thenReturn(userEntity);

        // Act
        Optional<UserState> result = userService.getLimitedUserStateByUsername("");

        // Assert
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(123L, result.get().getUserId());
        assertEquals("John Smith", result.get().getUserName());

    }

    @Test
    void addOrReplaceRoleInformationOnUser_updatesAssignments() {
        UserEntity user = user(123L);
        Set<Short> requestedBusinessUnitIds = Set.of((short) 11, (short) 14, (short) 15);
        List<BusinessUnitUserEntity> alignedBusinessUnitUsers = alignedBusinessUnitUsers();
        List<BusinessUnitUserRoleEntity> existingAssignments = existingAssignments();
        RoleEntity role = role(201L);

        stubAssignmentFlow(requestedBusinessUnitIds, alignedBusinessUnitUsers, existingAssignments, role);

        userService.addOrReplaceRoleInformationOnUser(user, 201L, requestedBusinessUnitIds);

        verify(roleService).removeObsoleteAssignments(
            eq(existingAssignments), eq(Set.of("BU001", "BU004", "BU005"))
        );
        verify(roleService).addMissingAssignments(
            eq(existingAssignments),
            argThat((Map<String, BusinessUnitUserEntity> businessUnitUsersById) ->
                businessUnitUsersById.keySet().equals(Set.of("BU001", "BU004", "BU005"))
                    && businessUnitUsersById.get("BU004").equals(alignedBusinessUnitUsers.get(1))
                    && businessUnitUsersById.get("BU005").equals(alignedBusinessUnitUsers.get(2))),
            eq(Set.of("BU001", "BU004", "BU005")),
            eq(role)
        );
    }

    @Test
    void addOrReplaceRoleInformationOnUser_logsAmendedEvent() {
        UserEntity user = user(123L);
        Set<Short> requestedBusinessUnitIds = Set.of((short) 11, (short) 14, (short) 15);

        stubAssignmentFlow(requestedBusinessUnitIds, alignedBusinessUnitUsers(), existingAssignments(), role(201L));

        userService.addOrReplaceRoleInformationOnUser(user, 201L, requestedBusinessUnitIds);

        verify(businessEventService).logBusinessEvent(
            eq(BusinessEventLogType.BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED), eq(123L),
            argThat((UnitsAssociatedToRoleAmendedEvent event) ->
                event.roleId().equals(201L)
                    && event.addedBusinessUnitIds().equals(Set.of((short) 14, (short) 15))
                    && event.removedBusinessUnitIds().equals(Set.of((short) 12, (short) 13))),
            eq(businessEventService)
        );
    }

    @Test
    void addOrReplaceRoleInformationOnUser_logsAssignedEvent() {
        final UserEntity user = user(123L);
        final Set<Short> requestedBusinessUnitIds = Set.of((short) 11, (short) 14, (short) 15);
        final List<BusinessUnitUserEntity> alignedBusinessUnitUsers = alignedBusinessUnitUsers();

        when(roleService.getAlignedBusinessUnitUsers(123L, requestedBusinessUnitIds))
            .thenReturn(alignedBusinessUnitUsers);
        when(roleService.requireRole(201L)).thenReturn(role(201L));
        when(roleService.getExistingAssignments(123L, 201L)).thenReturn(List.of());
        when(businessEventService.logBusinessEvent(
            eq(BusinessEventLogType.ROLE_ASSIGNED_TO_USER), eq(123L), any(RoleAssignedToUserEvent.class),
            eq(businessEventService))).thenReturn(BusinessEventEntity.builder().businessEventId(2L).build());

        userService.addOrReplaceRoleInformationOnUser(user, 201L, requestedBusinessUnitIds);

        verify(businessEventService).logBusinessEvent(
            eq(BusinessEventLogType.ROLE_ASSIGNED_TO_USER), eq(123L),
            argThat((RoleAssignedToUserEvent event) ->
                event.roleId().equals(201L)
                    && event.addedBusinessUnitIds().equals(Set.of((short) 11, (short) 14, (short) 15))),
            eq(businessEventService)
        );
    }

    @Test
    void activateUser_withoutDate_delegatesToOverload() {
        OffsetDateTime fixedDateTime = OffsetDateTime.parse("2026-04-27T10:15:30+01:00");
        Clock fixedClock = Clock.fixed(fixedDateTime.toInstant(), fixedDateTime.getOffset());

        userService = new UserService(
            userRepository,
            businessUnitUserService,
            roleService,
            businessEventService,
            fixedClock
        );

        UserEntity user = user(123L);

        UserService spyService = Mockito.spy(userService);

        spyService.activateUser(user);

        verify(spyService).activateUser(eq(user), eq(fixedDateTime));
    }

    @Test
    void activateUser_withDate_setsDate_andLogsEvent() {
        // Arrange
        UserEntity user = user(123L);
        OffsetDateTime activationDate = OffsetDateTime.parse("2026-04-27T10:15:30+01:00");

        // Act
        userService.activateUser(user, activationDate);

        // Assert
        assertEquals(activationDate.toLocalDateTime(), user.getActivationDate());

        verify(businessEventService).logBusinessEvent(
            eq(BusinessEventLogType.ACCOUNT_ACTIVATION_INITIATED),
            eq(123L),
            argThat((AccountActivationInitiatedEvent event) ->
                        event.accountActivationDate().equals(activationDate)
            ),
            eq(businessEventService)
        );
    }

    private BusinessUnitUserEntity businessUnitUser(String businessUnitUserId, Long userId, Short businessUnitId) {
        return BusinessUnitUserEntity.builder()
            .businessUnitUserId(businessUnitUserId)
            .user(UserEntity.builder().userId(userId).build())
            .businessUnit(BusinessUnitEntity.builder().businessUnitId(businessUnitId).build())
            .build();
    }

    private BusinessUnitUserRoleEntity assignment(
        String businessUnitUserId, Long userId, Short businessUnitId, Long roleId) {
        return BusinessUnitUserRoleEntity.builder()
            .businessUnitUser(businessUnitUser(businessUnitUserId, userId, businessUnitId))
            .role(RoleEntity.builder().roleId(roleId).build())
            .build();
    }

    private UserEntity user(Long userId) {
        return UserEntity.builder().userId(userId).build();
    }

    private RoleEntity role(Long roleId) {
        return RoleEntity.builder().roleId(roleId).build();
    }

    private List<BusinessUnitUserEntity> alignedBusinessUnitUsers() {
        return List.of(
            businessUnitUser("BU001", 123L, (short) 11),
            businessUnitUser("BU004", 123L, (short) 14),
            businessUnitUser("BU005", 123L, (short) 15)
        );
    }

    private List<BusinessUnitUserRoleEntity> existingAssignments() {
        return List.of(
            assignment("BU001", 123L, (short) 11, 201L),
            assignment("BU002", 123L, (short) 12, 201L),
            assignment("BU003", 123L, (short) 13, 201L)
        );
    }

    private void stubAssignmentFlow(
        Set<Short> requestedBusinessUnitIds,
        List<BusinessUnitUserEntity> alignedBusinessUnitUsers,
        List<BusinessUnitUserRoleEntity> existingAssignments,
        RoleEntity role) {
        when(roleService.getAlignedBusinessUnitUsers(123L, requestedBusinessUnitIds))
            .thenReturn(alignedBusinessUnitUsers);
        when(roleService.requireRole(201L)).thenReturn(role);
        when(roleService.getExistingAssignments(123L, 201L)).thenReturn(existingAssignments);
        when(businessEventService.logBusinessEvent(
            eq(BusinessEventLogType.BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED), eq(123L),
            any(UnitsAssociatedToRoleAmendedEvent.class),
            eq(businessEventService))).thenReturn(BusinessEventEntity.builder().businessEventId(1L).build());
    }
}
