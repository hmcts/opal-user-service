package uk.gov.hmcts.reform.opal.service.synchronise;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyBusinessUnitUserId;
import uk.gov.hmcts.reform.opal.entity.UserEntity;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class TestHelperUtil {

    private TestHelperUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static LegacyBusinessUnitUserId legacyBusinessUnitUser(String businessUnitUserId, String businessUnitId) {
        return LegacyBusinessUnitUserId.builder()
            .businessUnitUserId(businessUnitUserId)
            .businessUnitId(businessUnitId)
            .build();
    }

    public static LegacyBusinessUnitUserId legacyBusinessUnitUser(String businessUnitUserId, short businessUnitId) {
        return legacyBusinessUnitUser(businessUnitUserId, Short.toString(businessUnitId));
    }

    public static void setAuthenticatedUser(String tokenSubject) {
        Jwt jwt = new Jwt(
            "integration-test-token",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "none"),
            Map.of("sub", tokenSubject)
        );
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    public static UserEntity buildUser(long userId, String tokenSubject) {
        return UserEntity.builder()
            .userId(userId)
            .tokenSubject(tokenSubject)
            .build();
    }

    public static String synchronisePermissionsErrorMessage(long userId, String stage, String reason) {
        return "Could not synchronise permissions for user " + userId
            + " at stage: " + stage
            + ". Reason: " + reason;
    }

    public static Map<String, Set<String>> toCacheRoleMapping(Map<Long, Set<Short>> roleMapping) {
        Map<String, Set<String>> cacheRoleMapping = new LinkedHashMap<>();
        for (Map.Entry<Long, Set<Short>> roleMappingEntry : roleMapping.entrySet()) {
            Set<String> businessUnitIds = new LinkedHashSet<>();
            for (Short businessUnitId : roleMappingEntry.getValue()) {
                businessUnitIds.add(Short.toString(businessUnitId));
            }
            cacheRoleMapping.put(Long.toString(roleMappingEntry.getKey()), businessUnitIds);
        }
        return cacheRoleMapping;
    }
}
