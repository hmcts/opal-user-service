package uk.gov.hmcts.reform.opal.service.synchronise;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyBusinessUnitUserId;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.service.opal.UserService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

// Implements https://tools.hmcts.net/jira/browse/PO-2831

@Service
@RequiredArgsConstructor
@Slf4j(topic = "opal.SynchronisePermissionsService")
public class SynchronisePermissionsService {

    private static final String SYNC_STAGE = "synchronise user permissions";
    private static final String UNEXPECTED_RUNTIME_EXCEPTION_REASON = "unexpected runtime exception";

    private final LegacyWrapperService legacyWrapperService;
    private final SynchroniseBusinessUnitUsersService synchroniseBusinessUnitUsersService;
    private final SynchroniseRolesService synchroniseRolesService;

    private final UserService userService;

    @Transactional(propagation = REQUIRES_NEW, rollbackFor = Exception.class)
    public void synchronise(UserEntity detachedUser) {
        UserEntity user = detachedUser;
        try {
            user = userService.getUser(detachedUser.getUserId());

            //1-2. Get the legacy business unit ids
            List<LegacyBusinessUnitUserId> legacyBuuList = legacyWrapperService.getBusinessUnitUserIds(user);

            // Build the CSV-backed role/BU view first so the same validated BU set can drive
            // both role assignment and post-sync BU cleanup.
            Map<Long, Set<Short>> validatedRoleMap = synchroniseRolesService.getValidatedRoleMap(user, legacyBuuList);

            //3. Update any business_unit_users in the database that do not match the data returned from the legacy API
            synchroniseBusinessUnitUsersService.synchroniseBusinessUnitsUsers(user, legacyBuuList);

            //4-6. Update the users roles
            Set<Long> validatedRoleIds = synchroniseRolesService.synchroniseRoles(user, validatedRoleMap);

            // Legacy may return extra BU rows that are absent from the CSV mapping. Remove those
            // after role sync so neither the DB nor user state retains empty BU entries.
            synchroniseBusinessUnitUsersService.removeBusinessUnitUsersWithoutValidatedRoleMappings(
                user.getUserId(),
                getValidatedBusinessUnitIds(validatedRoleMap)
            );

            //7. Call activateUser method if the user does not have an activation date
            if (!validatedRoleIds.isEmpty() && user.getActivationDate() == null) {
                userService.activateUser(user);
                log.debug("User activated");
            }
        } catch (RuntimeException exception) {
            if (exception instanceof SynchronisePermissionsException synchronisePermissionsException) {
                throw synchronisePermissionsException;
            }
            throw new SynchronisePermissionsException(user, SYNC_STAGE,
                                                      UNEXPECTED_RUNTIME_EXCEPTION_REASON,
                                                      exception);
        }
    }

    private Set<Short> getValidatedBusinessUnitIds(Map<Long, Set<Short>> validatedRoleMap) {
        return validatedRoleMap.values().stream()
            .flatMap(Set::stream)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
