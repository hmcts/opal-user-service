package uk.gov.hmcts.reform.opal.service.synchronise;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyBusinessUnitUserId;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.service.opal.BusinessUnitUserService;
import uk.gov.hmcts.reform.opal.service.opal.UserService;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Implements Steps 4-6 of https://tools.hmcts.net/jira/browse/PO-2831

@Service
@AllArgsConstructor
@Slf4j(topic = "opal.SynchroniseRolesService")
public class SynchroniseRolesService {

    private static final String SYNC_STAGE = "synchronise roles";
    private static final String UNEXPECTED_RUNTIME_EXCEPTION_REASON = "unexpected runtime exception";

    private final UserService userService;

    private final BusinessUnitUserService businessUnitUserService;

    private final RoleMappingCacheLookupService roleMappingCacheLookupService;

    @Transactional
    public Set<Long> synchroniseRoles(UserEntity user, List<LegacyBusinessUnitUserId> legacyBuuList) {
        return synchroniseRoles(user, getValidatedRoleMap(user, legacyBuuList));
    }

    @Transactional
    public Set<Long> synchroniseRoles(UserEntity user, Map<Long, Set<Short>> validatedRoleMap) {
        try {
            // This map  represents the CSV/legacy blend, so only
            // validated BU ids are eligible for role assignment.
            validatedRoleMap.keySet().forEach(roleId ->
                userService.addOrReplaceRoleInformationOnUser(user, roleId, validatedRoleMap.get(roleId))
            );

            Set<Long> validatedRoleIds = new LinkedHashSet<>(validatedRoleMap.keySet());

            // 6. Remove any roles associated with user in db, but not present in validated role map
            Set<RoleEntity> usersCurrentRoles = businessUnitUserService.findAllRolesOfUser(user);
            for (RoleEntity role : usersCurrentRoles) {
                if (!validatedRoleIds.contains(role.getRoleId())) {
                    userService.deleteRoleFromUser(user, role.getRoleId());
                }
            }
            return validatedRoleIds;
        } catch (RuntimeException exception) {
            if (exception instanceof SynchronisePermissionsException synchronisePermissionsException) {
                throw synchronisePermissionsException;
            }
            throw new SynchronisePermissionsException(user, SYNC_STAGE,
                                                      UNEXPECTED_RUNTIME_EXCEPTION_REASON, exception);
        }
    }

    @Transactional(readOnly = true)
    public Map<Long, Set<Short>> getValidatedRoleMap(UserEntity user, List<LegacyBusinessUnitUserId> legacyBuuList) {
        try {
            try {
                Map<Long, Set<Short>> roleMap = roleMappingCacheLookupService.getRoleMappingByTokenSubject(
                    user
                );

                Set<Short> legacyBuIds = new LinkedHashSet<>();
                for (LegacyBusinessUnitUserId legacyUser : legacyBuuList) {
                    legacyBuIds.add(parseBusinessUnitId(user, legacyUser.getBusinessUnitId()));
                }

                // Keep only BU ids that appear in both sources:
                // CSV cache says "user should have role on BU"
                // legacy says "user currently has BU"
                return pruneBusinessUnitsNotReturnedByLegacy(roleMap, legacyBuIds, user);
            } catch (UserMissingFromCacheException e) {
                log.warn("Nothing in cache for : " + user.getTokenSubject());
                return Map.of();
            }
        } catch (RuntimeException exception) {
            if (exception instanceof SynchronisePermissionsException synchronisePermissionsException) {
                throw synchronisePermissionsException;
            }
            throw new SynchronisePermissionsException(user, SYNC_STAGE,
                                                      UNEXPECTED_RUNTIME_EXCEPTION_REASON, exception);
        }
    }

    private static @NonNull Map<Long, Set<Short>> pruneBusinessUnitsNotReturnedByLegacy(
        Map<Long, Set<Short>> typedRoleMap, Set<Short> legacyBuIds, UserEntity user) {
        Map<Long, Set<Short>> prunedMap = new LinkedHashMap<>();
        for (Long roleId : typedRoleMap.keySet()) {
            // Core comparison: a BU survives only if it is present in the
            // CSV-derived role map and in the legacy response for the current user.
            Set<Short> verifiedBuIds = new LinkedHashSet<>();
            for (Short buId : typedRoleMap.get(roleId)) {
                if (legacyBuIds.contains(buId)) {
                    verifiedBuIds.add(buId);
                } else {
                    log.info("Ignoring business unit {} from mapping as not found in legacy. User:{}", buId, user.getUserId());
                }
            }
            if (!verifiedBuIds.isEmpty()) {
                prunedMap.put(roleId, verifiedBuIds);
            }
        }
        return prunedMap;
    }

    private Short parseBusinessUnitId(UserEntity user, String businessUnitId) {
        try {
            return Short.valueOf(businessUnitId);
        } catch (NumberFormatException exception) {
            throw new SynchronisePermissionsException(user, SYNC_STAGE,
                                                      "parse failed for legacy business unit id", exception);
        }
    }
}
