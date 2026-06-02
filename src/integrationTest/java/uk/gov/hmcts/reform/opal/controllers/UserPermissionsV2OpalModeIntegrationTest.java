package uk.gov.hmcts.reform.opal.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.repository.UserRepository;
import uk.gov.hmcts.reform.opal.service.synchronise.SynchronisePermissionsService;
import uk.gov.hmcts.reform.opal.service.synchronise.TestHelperUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"integration", "opal"})
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_METHOD)
@DisplayName("UserPermissionsV2 opal mode integration tests")
class UserPermissionsV2OpalModeIntegrationTest extends AbstractIntegrationTest {

    private static final long USER_ID = 500000003L;

    @Autowired
    private UserRepository userRepository;

    @MockitoSpyBean
    private SynchronisePermissionsService synchronisePermissionsService;

    @Test
    @DisplayName("Should not trigger legacy synchronisation when app mode is opal")
    void getUserStateV2_whenAppModeIsOpal_doesNotSynchroniseWithLegacy() throws Exception {
        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        TestHelperUtil.setAuthenticatedUser(user);

        mockMvc.perform(get("/v2/users/0/state"))
            .andExpect(status().isOk());

        verify(synchronisePermissionsService, never()).synchronise(any(UserEntity.class));
    }

    @Test
    @DisplayName("Should reject non-zero user id requests")
    void getUserStateV2_whenUserIdIsNonZero_returnsBadRequest() throws Exception {

        mockMvc.perform(get("/v2/users/" + USER_ID + "/state"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.detail").value("Only userId 0 is supported."));

        verify(synchronisePermissionsService, never()).synchronise(any(UserEntity.class));
    }
}
