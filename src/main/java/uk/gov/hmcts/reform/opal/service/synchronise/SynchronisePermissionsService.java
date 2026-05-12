package uk.gov.hmcts.reform.opal.service.synchronise;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.opal.dto.synchronise.*;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.service.opal.BusinessUnitUserService;
import uk.gov.hmcts.reform.opal.service.opal.UserService;

import java.util.List;
import java.util.Set;

// Implements https://tools.hmcts.net/jira/browse/PO-2831

@Service
@RequiredArgsConstructor
@Slf4j(topic = "opal.SynchronisePermissionsService")
public class SynchronisePermissionsService {

    private final LegacyUserService legacyUserService;
    private final SynchroniseBusinessUnitUsersService refreshBusinessUnitUsersService;
    private final SynchroniseRolesService synchroniseRolesService;
    private final BusinessUnitUserService businessUnitUserService;
    @Lazy
    private final UserService userService;

    @Transactional(propagation = Propagation.REQUIRES_NEW,  noRollbackFor = SynchronisePermissionsException.class)
    public void synchronise(UserEntity user) throws SynchronisePermissionsException {
        try {
            //1. Fetch Libra user id's  from the legacy system
            LegacyGetUserResponse legacyGetUserResponse = legacyUserService.getUserIds(
                new LegacyGetUserRequest(user.getUsername())
            );
            List<String> libraUserIds = legacyGetUserResponse.getLibraUserIds();
            log.debug("legacyGetUserResponse: {}", legacyGetUserResponse);

            //2. Fetch Business unit user id's and business unit id's from the legacy system (PO-3442)
            //   using the Libra User id's returned previously
            LegacyBusinessUnitUsersResponse legacyBusinessUnitUsersResponse = legacyUserService.getBusinessUnitUsers(
                new LegacyBusinessUnitUsersRequest(libraUserIds)
            );
            log.debug("legacyBusinessUnitUsersResponse: {}", legacyBusinessUnitUsersResponse);

            //3. Update any business_unit_users in the database that does not match the data returned from the legacy API
            List<LegacyBusinessUnitUser> legacyBuuList = legacyBusinessUnitUsersResponse.getBusinessUnitUsers();
            refreshBusinessUnitUsersService.refreshBusinessUnitUsers(user, legacyBuuList);

            //4-6. Process role mapping cache
            synchroniseRolesService.process(user, legacyBuuList);

            //7. Call activateUser method if the user does not have an activation date.
            if (user.getActivationDate() == null) {
                userService.activateUser(user);
                log.debug("User activated");
            }
        } catch (SynchronisePermissionsException e) {
            //If any errors occurs in this process all roles should be removed from the user in the opal database
            // (ensuring the transaction was committed) this can be done by removing all business_unit_user_roles
            // for this user. And an error should be returned to the user.
            log.error("Legacy refresh failed for user id {}", user.getUserId(), e);
            Set<RoleEntity> roles = businessUnitUserService.findAllRolesOfUser(user);
            for (RoleEntity role : roles) {
                userService.deleteRoleFromUser(user, role.getRoleId());
            }
            throw e;
        }
    }
}
