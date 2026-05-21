package uk.gov.hmcts.reform.opal.service.synchronise;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.service.rolemapping.UserRoleMappingCacheService;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

@ActiveProfiles({"integration"})
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("RoleMappingCacheLookupService integration tests")
class RoleMappingCacheLookupServiceIntegrationTest extends AbstractIntegrationTest {

    private static final long USER_ID = 500000000L;
    private static final String TOKEN_SUBJECT = "subject-lookup-123";
    private static final String SYNC_STAGE = "parse role mapping cache";
    private static final String COULD_NOT_PARSE_JSON_REASON = "could not parse JSON";

    @Autowired
    private RoleMappingCacheLookupService roleMappingCacheLookupService;

    @Autowired
    private UserRoleMappingCacheService userRoleMappingCacheService;

    @Test
    @DisplayName("Should read role mapping from Redis and convert ids to numeric types")
    void getRoleMappingByTokenSubject_readsFromRedisAndConvertsToNumericMap() throws Exception {
        userRoleMappingCacheService.putUserMapping(
            TOKEN_SUBJECT,
            Map.of(
                "101", Set.of("7", "8"),
                "202", Set.of("9")
            )
        );

        try {
            Map<Long, Set<Short>> result = roleMappingCacheLookupService.getRoleMappingByTokenSubject(
                TestHelperUtil.buildUser(USER_ID, TOKEN_SUBJECT)
            );

            assertThat(result).isEqualTo(Map.of(
                101L, Set.of((short) 7, (short) 8),
                202L, Set.of((short) 9)
            ));
        } finally {
            userRoleMappingCacheService.deleteUserMapping(TOKEN_SUBJECT);
        }
    }

    @Test
    @DisplayName("Should throw SynchronisePermissionsException when Redis payload is not a role mapping")
    void getRoleMappingByTokenSubject_throwsWhenRedisPayloadIsInvalid() {
        userRoleMappingCacheService.putUserMapping(TOKEN_SUBJECT, "not-json");

        try {
            assertThatThrownBy(() -> roleMappingCacheLookupService.getRoleMappingByTokenSubject(
                TestHelperUtil.buildUser(USER_ID, TOKEN_SUBJECT)
            ))
                .isInstanceOf(SynchronisePermissionsException.class)
                .hasMessage(TestHelperUtil.synchronisePermissionsErrorMessage(
                    USER_ID,
                    SYNC_STAGE,
                    COULD_NOT_PARSE_JSON_REASON
                ));
        } finally {
            userRoleMappingCacheService.deleteUserMapping(TOKEN_SUBJECT);
        }
    }

    @Test
    @DisplayName("Should throw UserMissingFromCacheException when Redis payload is missing")
    void getRoleMappingByTokenSubject_throwsWhenRedisPayloadIsMissing() {
        userRoleMappingCacheService.deleteUserMapping(TOKEN_SUBJECT);

        assertThatThrownBy(() -> roleMappingCacheLookupService.getRoleMappingByTokenSubject(
            TestHelperUtil.buildUser(USER_ID, TOKEN_SUBJECT)
        ))
            .isInstanceOf(UserMissingFromCacheException.class)
            .hasMessage("Nothing in cache for : " + TOKEN_SUBJECT);
    }
}
