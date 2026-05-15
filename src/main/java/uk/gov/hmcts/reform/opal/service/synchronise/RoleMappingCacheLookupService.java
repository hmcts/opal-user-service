package uk.gov.hmcts.reform.opal.service.synchronise;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleMappingCacheLookupService {

    private static final String ROLE_MAPPING_USER_PREFIX = "ROLE_MAPPING_USER_";
    private static final String PARSE_ERROR_PREFIX = "Could not parse role mapping cache : ";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Map<Long, Set<Short>> getRoleMappingByTokenSubject(String tokenSubject)
        throws UserMissingFromCacheException {

        String cacheKey = ROLE_MAPPING_USER_PREFIX + tokenSubject;
        String roleMappingCacheString = redisTemplate.opsForValue().get(cacheKey);
        if (roleMappingCacheString == null || roleMappingCacheString.isBlank()) {
            throw new UserMissingFromCacheException("Nothing in cache for : " + tokenSubject);
        }
        Map<String, Set<String>> cacheMap = readCacheMap(roleMappingCacheString);
        return convertCacheMap(cacheMap);
    }

    private Map<String, Set<String>> readCacheMap(String cacheValue) {
        try {
            Map<String, Set<String>> cacheMap = objectMapper.readValue(cacheValue, new TypeReference<>() {});
            if (cacheMap == null) {
                throw new SynchronisePermissionsException(PARSE_ERROR_PREFIX + "payload resolved to null");
            }
            return cacheMap;
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new SynchronisePermissionsException(PARSE_ERROR_PREFIX + "could not parse JSON", exception);
        }
    }

    private Map<Long, Set<Short>> convertCacheMap(Map<String, Set<String>> cacheMap) {

        Map<Long, Set<Short>> converted = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : cacheMap.entrySet()) {
            Long roleId = parseRoleId(entry.getKey());
            if (entry.getValue() == null) {
                throw new SynchronisePermissionsException(
                    PARSE_ERROR_PREFIX + "null business unit ids for role " + entry.getKey()
                );
            }
            Set<Short> businessUnitIds = new HashSet<>();
            for (String businessUnitId : entry.getValue()) {
                businessUnitIds.add(parseBusinessUnitId(businessUnitId));
            }
            converted.put(roleId, businessUnitIds);
        }
        return converted;
    }

    private Long parseRoleId(String roleId) {
        try {
            return Long.valueOf(roleId);
        } catch (NumberFormatException exception) {
            throw new SynchronisePermissionsException(PARSE_ERROR_PREFIX + "could not parse roleId " + roleId,
                                                      exception);
        }
    }

    private Short parseBusinessUnitId(String businessUnitId) {
        try {
            return Short.valueOf(businessUnitId);
        } catch (NumberFormatException exception) {
            throw new SynchronisePermissionsException(PARSE_ERROR_PREFIX + "could not parse businessUnitId "
                                                          + businessUnitId, exception);
        }
    }
}
