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
    private static final String PARSE_ERROR = "Could not parse role mapping cache";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Map<Long, Set<Short>> getRoleMappingByTokenSubject(String tokenSubject) throws SynchronisePermissionsException {
        String cacheKey = ROLE_MAPPING_USER_PREFIX + tokenSubject;
        String roleMappingCacheString = redisTemplate.opsForValue().get(cacheKey);
        Map<String, Set<String>> cacheMap = readCacheMap(roleMappingCacheString);
        return convertCacheMap(cacheMap);
    }

    private Map<String, Set<String>> readCacheMap(String cacheValue) throws SynchronisePermissionsException {
        try {
            return objectMapper.readValue(cacheValue, new TypeReference<>() {});
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new SynchronisePermissionsException(PARSE_ERROR);
        }
    }

    private Map<Long, Set<Short>> convertCacheMap(Map<String, Set<String>> cacheMap) throws SynchronisePermissionsException {
        Map<Long, Set<Short>> converted = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : cacheMap.entrySet()) {
            Long roleId = parseRoleId(entry.getKey());
            Set<Short> businessUnitIds = new HashSet<>();
            for (String businessUnitId : entry.getValue()) {
                businessUnitIds.add(parseBusinessUnitId(businessUnitId));
            }
            converted.put(roleId, businessUnitIds);
        }
        return converted;
    }

    private Long parseRoleId(String roleId) throws SynchronisePermissionsException {
        try {
            return Long.valueOf(roleId);
        } catch (NumberFormatException exception) {
            throw new SynchronisePermissionsException(PARSE_ERROR);
        }
    }

    private Short parseBusinessUnitId(String businessUnitId) throws SynchronisePermissionsException {
        try {
            return Short.valueOf(businessUnitId);
        } catch (NumberFormatException exception) {
            throw new SynchronisePermissionsException(PARSE_ERROR);
        }
    }
}
