package uk.gov.hmcts.reform.opal.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.opal.BaseIntegrationTest;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

@ActiveProfiles({"integration"})
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("RoleRepository integration tests")
class RoleRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("Test roles view only brings back latest versions")
    void testRolesViewOnlyBringsBackLatestVersions() {
        RoleEntity finesRole1 = findRoleByName(1, "Fines_Role_1");
        RoleEntity finesRole2 = findRoleByName(1, "Fines_Role_2");
        RoleEntity confiscationRole3 = findRoleByName(2, "Confiscation_Role_3");

        assertThat(finesRole1.getVersionNumber()).isEqualTo(2);
        assertThat(finesRole1.getApplicationFunctionList()).containsExactly(
            "CREATE_MANAGE_DRAFT_ACCOUNTS", "ACCOUNT_ENQUIRY");
        assertThat(finesRole2.getVersionNumber()).isEqualTo(3);
        assertThat(finesRole2.getApplicationFunctionList()).containsExactly(
            "COLLECTION_ORDER", "CHECK_VALIDATE_DRAFT_ACCOUNTS", "SEARCH_AND_VIEW_ACCOUNTS");
        assertThat(confiscationRole3.getVersionNumber()).isEqualTo(2);
        assertThat(confiscationRole3.getApplicationFunctionList()).containsExactly(
            "CREATE_MANAGE_DRAFT_ACCOUNTS", "COLLECTION_ORDER");
    }

    @Test
    @DisplayName("Test role repository finds current active roles")
    void testRoleRepositoryFindsCurrentActiveRoles() {
        assertThat(roleRepository.existsByRoleId(1L)).isTrue();
        assertThat(roleRepository.existsByRoleIdAndIsActiveTrue(1L)).isTrue();
        assertThat(roleRepository.countByRoleIdAndIsActiveTrue(1L)).isEqualTo(1);

        RoleEntity role = roleRepository.findByRoleIdAndIsActiveTrue(1L).orElseThrow();
        assertThat(role.getRoleId()).isEqualTo(1L);
        assertThat(role.getVersionNumber()).isEqualTo(2L);
        assertThat(role.isActive()).isTrue();
    }

    @Test
    @DisplayName("Test role repository returns empty for missing roles")
    void testRoleRepositoryReturnsEmptyForMissingRoles() {
        assertThat(roleRepository.existsByRoleId(999L)).isFalse();
        assertThat(roleRepository.existsByRoleIdAndIsActiveTrue(999L)).isFalse();
        assertThat(roleRepository.countByRoleIdAndIsActiveTrue(999L)).isZero();
        assertThat(roleRepository.findByRoleIdAndIsActiveTrue(999L)).isEmpty();
    }

    private RoleEntity findRoleByName(Integer domainId, String roleName) {
        Set<RoleEntity> matchingRoles = roleRepository.findAllByDomain_Id(domainId)
            .stream()
            .filter(role -> role.getName().equals(roleName))
            .collect(Collectors.toSet());
        assertThat(matchingRoles).hasSize(1);
        return matchingRoles.iterator().next();
    }
}
