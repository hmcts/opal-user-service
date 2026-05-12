package uk.gov.hmcts.reform.opal.service.synchronise;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyBusinessUnitUser;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.service.opal.BusinessUnitUserService;
import uk.gov.hmcts.reform.opal.service.opal.UserService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Implements Steps 4-6 of https://tools.hmcts.net/jira/browse/PO-2831

@Service
@AllArgsConstructor
@Slf4j(topic = "opal.SynchroniseRolesService")
public class SynchroniseRolesService {

    @Lazy
    private final UserService userService;

    private final BusinessUnitUserService businessUnitUserService;

    private final RoleMappingCacheLookupService roleMappingCacheLookupService;

    @Transactional
    public void process(UserEntity user, List<LegacyBusinessUnitUser> legacyBuuList) throws SynchronisePermissionsException {

        Map<Long, Set<Short>> roleMap = roleMappingCacheLookupService.getRoleMappingByTokenSubject(
            user.getTokenSubject()
        );

        Set<Short> legacyBuuIds =  new HashSet<>();
        for (LegacyBusinessUnitUser legacyUser : legacyBuuList) {
            legacyBuuIds.add(parseBusinessUnitId(legacyUser.getBusinessUnitId()));
        }

        Map<Long, Set<Short>> prunedMap = pruneBusinessUnitsNotReturnedByLegacy(roleMap, legacyBuuIds);

        // Add or replace the roles in the pruned role map
        prunedMap.keySet().forEach(roleId -> userService.addOrReplaceRoleInformationOnUser(
            user,
            roleId,
            prunedMap.get(roleId)
        ));

        // Remove any roles associated with user in db, but not present in pruned role map
        Set<RoleEntity> usersCurrentRoles = businessUnitUserService.findAllRolesOfUser(user);
        Set<Long> cachedRoles = prunedMap.keySet();
        for (RoleEntity role : usersCurrentRoles) {
            if (!cachedRoles.contains(role.getRoleId())) {
                userService.deleteRoleFromUser(user, role.getRoleId());
            }
        }
    }

    private static @NonNull Map<Long, Set<Short>> pruneBusinessUnitsNotReturnedByLegacy(
        Map<Long, Set<Short>> typedRoleMap, Set<Short> legacyBuuIds) {
        Map<Long, Set<Short>> prunedMap = new HashMap<>();
        for (Long roleId : typedRoleMap.keySet()) {
            Set<Short> verifiedBuus = new HashSet<>();
            for (Short buu : typedRoleMap.get(roleId)) {
                if (legacyBuuIds.contains(buu)) {
                    verifiedBuus.add(buu);
                }
            }
            if (!verifiedBuus.isEmpty()) {
                prunedMap.put(roleId, verifiedBuus);
            }
        }
        return prunedMap;
    }

    private Short parseBusinessUnitId(String businessUnitId) throws SynchronisePermissionsException {
        try {
            return Short.valueOf(businessUnitId);
        } catch (NumberFormatException exception) {
            throw new SynchronisePermissionsException("Could not parse role mapping cache");
        }
    }
}
