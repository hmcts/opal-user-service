package uk.gov.hmcts.reform.opal.service.opal;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.entity.BusinessEventEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessEventLogType;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserRoleEntity;
import uk.gov.hmcts.reform.opal.repository.BusinessEventRepository;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitUserRoleRepository;
import uk.gov.hmcts.reform.opal.repository.UserRepository;
import uk.gov.hmcts.reform.opal.service.UserPermissionsService;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

@ActiveProfiles({"integration"})
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("UserService integration tests")
@SuppressWarnings({"java:S6813", "SpringJavaInjectionPointsAutowiringInspection"})
class UserServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private BusinessUnitUserRoleRepository businessUnitUserRoleRepository;

    @Autowired
    private BusinessEventRepository businessEventRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private UserPermissionsService userPermissionsService;

    @Test
    @DisplayName("Should add role assignments and log assigned event")
    void addOrReplaceRoleInformationOnUser_addsRoleAssignments() throws JsonProcessingException {
        when(userPermissionsService.getAuthenticatedUserId(userPermissionsService)).thenReturn(500000003L);

        final long businessEventCountBeforeCall = businessEventRepository.count();

        userService.addOrReplaceRoleInformationOnUser(
            500000000L, 3L, Set.of((short) 68, (short) 69), userService
        );

        assertThat(getAssignedBusinessUnitUserIds(500000000L, 3L)).containsExactlyInAnyOrder("L066JG", "L078JG");
        assertThat(getAssignedBusinessUnitIds(500000000L, 3L)).containsExactlyInAnyOrder((short) 68, (short) 69);

        assertThat(businessEventRepository.count()).isEqualTo(businessEventCountBeforeCall + 1);

        BusinessEventEntity businessEvent = findLatestBusinessEvent();
        assertThat(businessEvent.getEventType()).isEqualTo(BusinessEventLogType.ROLE_ASSIGNED_TO_USER);
        assertThat(businessEvent.getSubjectUserId()).isEqualTo(500000000L);
        assertThat(businessEvent.getInitiatorUserId()).isEqualTo(500000003L);
        assertRoleAssignedEventDetails(businessEvent.getEventDetails(), 3L, Set.of((short) 68, (short) 69));
    }

    @Test
    @DisplayName("Should replace existing role assignments and log amended event")
    void addOrReplaceRoleInformationOnUser_replacesRoleAssignments() throws JsonProcessingException {
        when(userPermissionsService.getAuthenticatedUserId(userPermissionsService)).thenReturn(500000003L);

        final long businessEventCountBeforeCall = businessEventRepository.count();

        userService.addOrReplaceRoleInformationOnUser(
            500000000L, 2L, Set.of((short) 68, (short) 73), userService
        );

        assertThat(getAssignedBusinessUnitUserIds(500000000L, 2L)).containsExactlyInAnyOrder("L066JG", "L067JG");
        assertThat(getAssignedBusinessUnitIds(500000000L, 2L)).containsExactlyInAnyOrder((short) 68, (short) 73);

        assertThat(businessEventRepository.count()).isEqualTo(businessEventCountBeforeCall + 1);

        BusinessEventEntity businessEvent = findLatestBusinessEvent();
        assertThat(businessEvent.getEventType())
            .isEqualTo(BusinessEventLogType.BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED);
        assertThat(businessEvent.getSubjectUserId()).isEqualTo(500000000L);
        assertThat(businessEvent.getInitiatorUserId()).isEqualTo(500000003L);
        assertRoleAmendedEventDetails(
            businessEvent.getEventDetails(), 2L, Set.of((short) 68, (short) 73), Set.of((short) 70)
        );
    }

    @Test
    @DisplayName("Should reject missing business unit user alignment without changing state")
    void addOrReplaceRoleInformationOnUser_rejectsMissingAlignment() {
        long businessEventCountBeforeCall = businessEventRepository.count();
        List<BusinessUnitUserRoleEntity> assignmentsBeforeCall =
            businessUnitUserRoleRepository.findAllByBusinessUnitUser_User_UserIdAndRole_RoleId(500000000L, 3L);

        assertThrows(
            IllegalStateException.class,
            () -> userService.addOrReplaceRoleInformationOnUser(
                500000000L, 3L, Set.of((short) 68, (short) 72), userService
            )
        );

        assertThat(
            businessUnitUserRoleRepository.findAllByBusinessUnitUser_User_UserIdAndRole_RoleId(500000000L, 3L)
        ).usingRecursiveFieldByFieldElementComparator().containsExactlyElementsOf(assignmentsBeforeCall);
        assertThat(businessEventRepository.count()).isEqualTo(businessEventCountBeforeCall);
    }

    @Test
    @DisplayName("Should reject missing role without changing state")
    void addOrReplaceRoleInformationOnUser_rejectsMissingRole() {
        long businessEventCountBeforeCall = businessEventRepository.count();
        List<BusinessUnitUserRoleEntity> assignmentsBeforeCall =
            businessUnitUserRoleRepository.findAllByBusinessUnitUser_User_UserIdAndRole_RoleId(500000000L, 999L);

        assertThrows(
            jakarta.persistence.EntityNotFoundException.class,
            () -> userService.addOrReplaceRoleInformationOnUser(
                500000000L, 999L, Set.of((short) 68, (short) 69), userService
            )
        );

        assertThat(
            businessUnitUserRoleRepository.findAllByBusinessUnitUser_User_UserIdAndRole_RoleId(500000000L, 999L)
        ).usingRecursiveFieldByFieldElementComparator().containsExactlyElementsOf(assignmentsBeforeCall);
        assertThat(businessEventRepository.count()).isEqualTo(businessEventCountBeforeCall);
    }

    @Test
    @DisplayName("Should remove role assignments and log unassigned event")
    void deleteRoleFromUser_removesRoleAssignments() {
        when(userPermissionsService.getAuthenticatedUserId(userPermissionsService)).thenReturn(500000003L);

        final long businessEventCountBeforeCall = businessEventRepository.count();

        userService.deleteRoleFromUser(500000000L, 1L, userService);

        assertThat(getAssignedBusinessUnitUserIds(500000000L, 1L)).isEmpty();
        assertThat(getAssignedBusinessUnitIds(500000000L, 1L)).isEmpty();
        assertThat(businessEventRepository.count()).isEqualTo(businessEventCountBeforeCall + 1);

        BusinessEventEntity businessEvent = findLatestBusinessEvent();
        assertThat(businessEvent.getEventType()).isEqualTo(BusinessEventLogType.ROLE_UNASSIGNED_FROM_USER);
        assertThat(businessEvent.getSubjectUserId()).isEqualTo(500000000L);
        assertThat(businessEvent.getInitiatorUserId()).isEqualTo(500000003L);
        assertRoleUnassignedEventDetails(businessEvent.getEventDetails(), 1L, Set.of((short) 70), 2L);
    }

    @Test
    @DisplayName("Should do nothing when the user does not have the role")
    void deleteRoleFromUser_returnsWhenNoAssignmentsExist() {
        long businessEventCountBeforeCall = businessEventRepository.count();
        List<BusinessUnitUserRoleEntity> assignmentsBeforeCall =
            businessUnitUserRoleRepository.findAllByBusinessUnitUser_User_UserIdAndRole_RoleId(500000006L, 1L);

        userService.deleteRoleFromUser(500000006L, 1L, userService);

        assertThat(
            businessUnitUserRoleRepository.findAllByBusinessUnitUser_User_UserIdAndRole_RoleId(500000006L, 1L)
        ).usingRecursiveFieldByFieldElementComparator().containsExactlyElementsOf(assignmentsBeforeCall);
        assertThat(businessEventRepository.count()).isEqualTo(businessEventCountBeforeCall);
    }

    @Test
    @DisplayName("Should reject missing role during delete without changing state")
    void deleteRoleFromUser_rejectsMissingRole() {
        final long businessEventCountBeforeCall = businessEventRepository.count();

        assertThrows(
            jakarta.persistence.EntityNotFoundException.class,
            () -> userService.deleteRoleFromUser(500000000L, 999L, userService)
        );

        assertThat(businessEventRepository.count()).isEqualTo(businessEventCountBeforeCall);
    }

    private BusinessEventEntity findLatestBusinessEvent() {
        return businessEventRepository.findAll().stream()
            .max(Comparator.comparing(BusinessEventEntity::getBusinessEventId))
            .orElseThrow();
    }

    private Set<String> getAssignedBusinessUnitUserIds(Long userId, Long roleId) {
        return userRepository.findIdWithPermissions(userId).orElseThrow()
            .getBusinessUnitUsers().stream()
            .filter(businessUnitUser -> businessUnitUser.getBusinessUnitUserRoleList().stream()
                .anyMatch(assignment -> assignment.getRoleId().equals(roleId)))
            .map(businessUnitUser -> businessUnitUser.getBusinessUnitUserId())
            .collect(Collectors.toSet());
    }

    private Set<Short> getAssignedBusinessUnitIds(Long userId, Long roleId) {
        return userRepository.findIdWithPermissions(userId).orElseThrow()
            .getBusinessUnitUsers().stream()
            .filter(businessUnitUser -> businessUnitUser.getBusinessUnitUserRoleList().stream()
                .anyMatch(assignment -> assignment.getRoleId().equals(roleId)))
            .map(businessUnitUser -> businessUnitUser.getBusinessUnitId())
            .collect(Collectors.toSet());
    }

    private void assertRoleAssignedEventDetails(String actualJson, Long roleId, Set<Short> addedBusinessUnitIds)
        throws JsonProcessingException {
        JsonNode actual = objectMapper.readTree(actualJson);
        assertThat(actual.get("role_id").longValue()).isEqualTo(roleId);
        assertThat(actual.get("added_business_unit_ids"))
            .extracting(JsonNode::shortValue)
            .containsExactlyInAnyOrderElementsOf(addedBusinessUnitIds);
    }

    private void assertRoleAmendedEventDetails(
        String actualJson, Long roleId, Set<Short> addedBusinessUnitIds, Set<Short> removedBusinessUnitIds)
        throws JsonProcessingException {

        JsonNode actual = objectMapper.readTree(actualJson);
        assertThat(actual.get("role_id").longValue()).isEqualTo(roleId);
        assertThat(actual.get("added_business_unit_ids"))
            .extracting(JsonNode::shortValue)
            .containsExactlyInAnyOrderElementsOf(addedBusinessUnitIds);
        assertThat(actual.get("removed_business_unit_ids"))
            .extracting(JsonNode::shortValue)
            .containsExactlyInAnyOrderElementsOf(removedBusinessUnitIds);
    }

    private void assertRoleUnassignedEventDetails(
        String actualJson, Long roleId, Set<Short> businessUnitIds, Long roleVersion) {

        JsonNode actual = objectMapper.readTree(actualJson);
        assertThat(actual.get("role_id").longValue()).isEqualTo(roleId);
        assertThat(actual.get("business_unit_ids"))
            .extracting(JsonNode::shortValue)
            .containsExactlyInAnyOrderElementsOf(businessUnitIds);
        assertThat(actual.get("role_version").longValue()).isEqualTo(roleVersion);
    }
}
