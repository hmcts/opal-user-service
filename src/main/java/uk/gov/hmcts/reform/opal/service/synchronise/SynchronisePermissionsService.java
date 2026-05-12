package uk.gov.hmcts.reform.opal.service.synchronise;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyBusinessUnitUser;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyBusinessUnitUsersRequest;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyBusinessUnitUsersResponse;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyGetUserRequest;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyGetUserResponse;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.service.opal.BusinessUnitUserService;
import uk.gov.hmcts.reform.opal.service.opal.UserService;

import java.util.List;

// Implements https://tools.hmcts.net/jira/browse/PO-2831

@Service
@RequiredArgsConstructor
@Slf4j(topic = "opal.SynchronisePermissionsService")
public class SynchronisePermissionsService {

    private final LegacyUserService legacyUserService;
    private final SynchroniseBusinessUnitUsersService refreshBusinessUnitUsersService;
    private final SynchroniseRolesService synchroniseRolesService;

    @Lazy
    private final UserService userService;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void synchronise(UserEntity user) {

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

        //3. Update any business_unit_users in the database that do not match the data returned from the legacy API
        List<LegacyBusinessUnitUser> legacyBuuList = legacyBusinessUnitUsersResponse.getBusinessUnitUsers();
        refreshBusinessUnitUsersService.refreshBusinessUnitUsers(user, legacyBuuList);

        //4-6. Process role mapping cache
        synchroniseRolesService.process(user, legacyBuuList);

        //7. Call activateUser method if the user does not have an activation date.
        if (user.getActivationDate() == null) {
            userService.activateUser(user);
            log.debug("User activated");
        }
    }
}
