package uk.gov.hmcts.reform.opal.service.synchronise;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.service.opal.RoleService;
import uk.gov.hmcts.reform.opal.service.rolemapping.UserRoleMappingCacheService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "opal.RoleMappingCacheLookupService")
public class RoleMappingCacheLookupService {

    private static final String SYNC_STAGE = "parse role mapping cache";
    private static final String UNEXPECTED_RUNTIME_EXCEPTION_REASON = "unexpected runtime exception";

    private final UserRoleMappingCacheService userRoleMappingCacheService;
    private final RoleService roleService;
    private final ObjectMapper objectMapper;

    /**
     * Looks up the raw cache payload map for the user and converts it into typed identifiers used by
     * synchronisation.
     *
     * @param user user whose token subject forms the Redis cache key for a
     *             payload map keyed by role id as {@link String}, with values containing business unit ids as
     *             {@link String}
     * @return map keyed by role id as {@link Long}, with values containing business unit ids as {@link Short}
     * @throws UserMissingFromCacheException when there is no cache entry for the user token subject
     */
    public Map<Long, Set<Short>> getRoleMappingByTokenSubject(UserEntity user)
        throws UserMissingFromCacheException {
        try {
            String tokenSubject = user.getTokenSubject();
            String roleMappingCacheString = userRoleMappingCacheService.getUserMapping(tokenSubject);
            if (roleMappingCacheString == null || roleMappingCacheString.isBlank()) {
                throw new UserMissingFromCacheException("Nothing in cache for : " + tokenSubject);
            }
            Map<String, Set<String>> cacheMap = readCacheMap(user, roleMappingCacheString);
            return convertCacheMap(user, cacheMap);
        } catch (RuntimeException exception) {
            if (exception instanceof SynchronisePermissionsException synchronisePermissionsException) {
                throw synchronisePermissionsException;
            }
            throw new SynchronisePermissionsException(user, SYNC_STAGE, UNEXPECTED_RUNTIME_EXCEPTION_REASON, exception);
        }
    }

    private Map<String, Set<String>> readCacheMap(UserEntity user, String cacheValue) {
        try {
            Map<String, Set<String>> cacheMap = objectMapper.readValue(cacheValue, new TypeReference<>() {});
            if (cacheMap == null) {
                throw new SynchronisePermissionsException(user, SYNC_STAGE, "payload resolved to null");
            }
            return cacheMap;
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new SynchronisePermissionsException(user, SYNC_STAGE, "could not parse JSON", exception);
        }
    }

    /**
     * Converts the raw cache payload map into typed identifiers used by synchronisation.
     *
     * @param cacheMap map keyed by role id as {@link String}, with values containing business unit ids as
     *                 {@link String}
     * @return map keyed by role id as {@link Long}, with values containing business unit ids as {@link Short}
     */
    @SuppressWarnings("java:S135")
    private Map<Long, Set<Short>> convertCacheMap(UserEntity user, Map<String, Set<String>> cacheMap) {

        Map<Long, Set<Short>> converted = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : cacheMap.entrySet()) {
            Long roleId = parseRoleId(user, entry.getKey());
            try {
                roleService.requireRole(roleId);
            }  catch (EntityNotFoundException exception) {
                log.warn("Cache roleId not found in database. user: {} roleId: {}", user.getTokenSubject(), roleId);
                continue;
            }

            if (entry.getValue() == null) {
                log.warn("Role {} has null business unit ids in cache for user {}. Treating as empty set.",
                         entry.getKey(), user.getUserId());
                converted.put(roleId, new HashSet<>());
                continue;
            }
            Set<Short> businessUnitIds = new HashSet<>();
            for (String businessUnitId : entry.getValue()) {
                businessUnitIds.add(parseBusinessUnitId(user, businessUnitId));
            }
            converted.put(roleId, businessUnitIds);
        }
        return converted;
    }

    private Long parseRoleId(UserEntity user, String roleId) {
        try {
            return Long.valueOf(roleId);
        } catch (NumberFormatException exception) {
            throw new SynchronisePermissionsException(
                user,
                SYNC_STAGE, "could not parse roleId " + roleId,
                exception);
        }
    }

    private Short parseBusinessUnitId(UserEntity user, String businessUnitId) {
        try {
            return Short.valueOf(businessUnitId);
        } catch (NumberFormatException exception) {
            throw new SynchronisePermissionsException(
                user,
                SYNC_STAGE, "could not parse businessUnitId " + businessUnitId,
                exception
            );
        }
    }
}
