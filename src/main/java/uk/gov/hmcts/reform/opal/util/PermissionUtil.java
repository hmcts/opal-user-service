package uk.gov.hmcts.reform.opal.util;

import org.springframework.security.access.AccessDeniedException;
import uk.gov.hmcts.reform.opal.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.reform.opal.authorisation.model.Permissions;
import uk.gov.hmcts.reform.opal.authorisation.model.UserState;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitRef;
import uk.gov.hmcts.reform.opal.service.opal.UserStateService;

import java.util.List;
import java.util.Optional;

public class PermissionUtil {

    private PermissionUtil() {

    }

    public static BusinessUnitUser getRequiredBusinessUnitUser(UserState userState, Short businessUnitId) {
        return userState.getBusinessUnitUserForBusinessUnit(businessUnitId).orElseThrow(() -> new
            AccessDeniedException("User does not have assigned permissions in business unit: " + businessUnitId));
    }

    public static boolean checkBusinessUnitUserHasPermission(BusinessUnitUser businessUnitUser,
                                                             Permissions permission) {
        if (businessUnitUser.doesNotHavePermission(permission)) {
            throw new AccessDeniedException("User does not have the required permission: " + permission.description);
        }
        return true;
    }

    public static boolean checkAnyBusinessUnitUserHasPermission(UserState userState, Permissions permission) {
        if (userState.noBusinessUnitUserHasPermission(permission)) {
            throw new AccessDeniedException("User does not have the required permission: " + permission.description);
        }
        return true;
    }

    public static  <B extends BusinessUnitRef> List<B> filterBusinessUnitsByPermission(
        UserStateService userStateService, List<B> refData,
        Optional<Permissions> optPermission, String authHeaderValue) {

        return optPermission.map(
            permission -> {
                UserState.UserBusinessUnits userBusinessUnits = userStateService
                    .getUserStateUsingAuthToken(authHeaderValue)
                    .allBusinessUnitUsersWithPermission(permission);
                return refData
                    .stream()
                    .filter(bu -> userBusinessUnits
                        .containsBusinessUnit(bu.getBusinessUnitId()))
                    .toList();
            }).orElse(refData);
    }
}
