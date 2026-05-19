package uk.gov.hmcts.reform.opal.service.synchronise;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.opal.entity.UserEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleMappingCacheLookupService {

    private static final String ROLE_MAPPING_USER_PREFIX = "ROLE_MAPPING_USER_";
    private static final String SYNC_STAGE = "parse role mapping cache";
    private static final String UNEXPECTED_RUNTIME_EXCEPTION_REASON = "unexpected runtime exception";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Looks up the raw cache payload map for the user and converts it into typed identifiers used by
     * synchronisation.
     *
     * @param user user whose token subject forms the Redis key ({@code ROLE_MAPPING_USER_<tokenSubject>}) for a
     *             payload map keyed by role id as {@link String}, with values containing business unit ids as
     *             {@link String}
     * @return map keyed by role id as {@link Long}, with values containing business unit ids as {@link Short}
     * @throws UserMissingFromCacheException when there is no cache entry for the user token subject
     */
    public Map<Long, Set<Short>> getRoleMappingByTokenSubject(UserEntity user)
        throws UserMissingFromCacheException {
        try {
            String tokenSubject = user.getTokenSubject();
            String cacheKey = ROLE_MAPPING_USER_PREFIX + tokenSubject;
            String roleMappingCacheString = redisTemplate.opsForValue().get(cacheKey);
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
    private Map<Long, Set<Short>> convertCacheMap(UserEntity user, Map<String, Set<String>> cacheMap) {

        Map<Long, Set<Short>> converted = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : cacheMap.entrySet()) {
            Long roleId = parseRoleId(user, entry.getKey());
            if (entry.getValue() == null) {
                throw new SynchronisePermissionsException(
                    user,
                    SYNC_STAGE, "null business unit ids for role " + entry.getKey()
                );
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
