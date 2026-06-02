package uk.gov.hmcts.reform.opal.service.synchronise;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyBusinessUnitUserId;
import uk.gov.hmcts.reform.opal.entity.UserEntity;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TestHelperUtil {

    private TestHelperUtil() {
        throw new IllegalStateException("Utility class");
    }

    private static UserEntity getDefaultUser() {
        return UserEntity.builder()
            .userId(500000000L)
            .username("username")
            .build();
    }

    public static UserStateV2 buildUserStateV2(UserEntity user) {
        return UserStateV2.builder()
            .userId(user.getUserId())
            .username(user.getUsername())
            .build();
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

    public static void setAuthenticatedUser(UserEntity user) {
        Jwt jwt = new Jwt(
            "integration-test-token",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "none"),
            Map.of(
                "sub", user.getTokenSubject(),
                "preferred_username", user.getUsername(),
                "name", user.getTokenName()
            )
        );
        SecurityContextHolder.getContext().setAuthentication(buildOpalJwtAuthenticationToken(jwt, user));
    }

    public static OpalJwtAuthenticationToken createJwtPrincipal(String sub, String preferred, String name) {
        Jwt jwt = new Jwt(
            "mock-token-value",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "none"),
            Map.of("sub", sub,
                "preferred_username", preferred,
                "name", name
            )
        );
        return buildOpalJwtAuthenticationToken(jwt, getDefaultUser());
    }

    public static OpalJwtAuthenticationToken buildOpalJwtAuthenticationToken(Jwt jwt, UserEntity user) {
        UserStateV2 userStateV2 = buildUserStateV2(user);
        return new OpalJwtAuthenticationToken(userStateV2,
            Domain.USER,
            jwt,
            List.of(),
            null);
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
