package uk.gov.hmcts.reform.opal.service.synchronise;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyBusinessUnitUserId;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.service.opal.BusinessUnitUserService;
import uk.gov.hmcts.reform.opal.service.opal.UserService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptySet;

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
    public Set<Long> synchroniseRoles(UserEntity user, List<LegacyBusinessUnitUserId> legacyBuuList) {

        Set<Long> validatedRoleIds;
        try {
            // 4. Fetch data from the role mapping cache (
            Map<Long, Set<Short>> roleMap = roleMappingCacheLookupService.getRoleMappingByTokenSubject(
                user.getTokenSubject()
            );

            // 5. Compare the two results and capture the intercept
            Set<Short> legacyBuuIds =  new HashSet<>();
            for (LegacyBusinessUnitUserId legacyUser : legacyBuuList) {
                legacyBuuIds.add(parseBusinessUnitId(legacyUser.getBusinessUnitId()));
            }

            Map<Long, Set<Short>> validatedRoleMap = pruneBusinessUnitsNotReturnedByLegacy(roleMap, legacyBuuIds);
            validatedRoleMap.keySet().forEach(roleId ->
                userService.addOrReplaceRoleInformationOnUser(user, roleId, validatedRoleMap.get(roleId))
            );
            validatedRoleIds = validatedRoleMap.keySet();

        } catch (UserMissingFromCacheException e) {
            log.info("Nothing in cache for : " + user.getTokenSubject());
            validatedRoleIds = emptySet();
        }

        // 6. Remove any roles associated with user in db, but not present in validated role map
        Set<RoleEntity> usersCurrentRoles = businessUnitUserService.findAllRolesOfUser(user);
        for (RoleEntity role : usersCurrentRoles) {
            if (!validatedRoleIds.contains(role.getRoleId())) {
                userService.deleteRoleFromUser(user, role.getRoleId());
            }
        }

        return validatedRoleIds;
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

    private Short parseBusinessUnitId(String businessUnitId) {
        try {
            return Short.valueOf(businessUnitId);
        } catch (NumberFormatException exception) {
            throw new SynchronisePermissionsException("Could not parse legacy business unit id", exception);
        }
    }
}
