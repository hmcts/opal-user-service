package uk.gov.hmcts.reform.opal.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Sql(scripts = "classpath:db.insertData/insert_user_state_data.sql", executionPhase = BEFORE_TEST_CLASS)
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=create-drop"})
@DisplayName("UserPermissionsController Integration Test with Security")
class UserPermissionsControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String URL_BASE = "/users";

    @Test
    @DisplayName("Should return 200 and full user state for a user with permissions [PO-857]")
    void getUserState_whenUserHasPermissions_returns200AndCorrectPayload() throws Exception {
        long userIdWithPermissions = 500000000L;

        ResultActions actions = mockMvc.perform(get(URL_BASE + "/" + userIdWithPermissions + "/state"));

        actions.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$['user_id']").value(500000000))
            .andExpect(jsonPath("$['username']").value("opal-test@HMCTS.NET"))
            .andExpect(jsonPath("$['business_unit_users']", hasSize(3)))
            .andExpect(jsonPath("$['business_unit_users'][*]['business_unit_id']",
                                containsInAnyOrder(70, 68, 61)))
            .andExpect(jsonPath(
                "$['business_unit_users'][?(@['business_unit_id']==70)]['business_unit_user_id']")
                           .value("L065JG"))
            .andExpect(jsonPath(
                "$['business_unit_users'][?(@['business_unit_id']==70)]['permissions'][*]['permission_name']",
                                containsInAnyOrder("Account Enquiry - Account Notes", "Account Enquiry")))
            .andExpect(jsonPath(
                "$['business_unit_users'][?(@['business_unit_id']==68)]['business_unit_user_id']")
                           .value("L066JG"))
            .andExpect(jsonPath(
                "$['business_unit_users'][?(@['business_unit_id']==68)]['permissions'][0]['permission_name']")
                           .value("Account Enquiry - Account Notes"))
            .andExpect(jsonPath(
                "$['business_unit_users'][?(@['business_unit_id']==61)]['business_unit_user_id']")
                           .value("L080JG"))
            .andExpect(jsonPath(
                "$['business_unit_users'][?(@['business_unit_id']==61)]['permissions'][0]['permission_name']")
                           .value("Collection Order"));
    }

    @Test
    @DisplayName("Should return 200 and state with empty list for a user that exists but has no permissions [PO-857]")
    void getUserState_whenUserExistsButHasNoPermissions_returns200AndEmptyList() throws Exception {    
        long userIdWithoutPermissions = 500000001L;

        ResultActions actions = mockMvc.perform(get(URL_BASE + "/" + userIdWithoutPermissions + "/state"));

        actions.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$['user_id']").value(500000001))
            .andExpect(jsonPath("$['username']").value("opal-test-2@HMCTS.NET"))
            .andExpect(jsonPath("$['business_unit_users']", hasSize(0)));
    }

    @Test
    @DisplayName("Should return 404 Not Found for a user that does not exist [PO-857]")
    void getUserState_whenUserDoesNotExist_returns404() throws Exception {
        long nonExistentUserId = 999999999L;

        mockMvc.perform(get(URL_BASE + "/" + nonExistentUserId + "/state"))
            .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("Should return 406 for invalid User ID format [PO-857]")
    void getUserState_whenUserIdFormatIsInvalid_returns406() throws Exception {
        String invalidUserId = "invalidUserId";
        
        mockMvc.perform(get(URL_BASE + "/" + invalidUserId + "/state"))
            .andExpect(status().isNotAcceptable());
    }
    
    @Test
    @DisplayName("Should handle business unit with one permission [PO-857]")
    void getUserState_whenBusinessUnitHasOnePermission_returnsCorrectPermission() throws Exception {
        long userIdWithPermissions = 500000000L;
        
        ResultActions actions = mockMvc.perform(get(URL_BASE + "/" + userIdWithPermissions + "/state"));
        
        actions.andExpect(status().isOk())
            .andExpect(jsonPath(
                "$['business_unit_users'][?(@['business_unit_id']==68)]['permissions']", hasSize(1)))
            .andExpect(jsonPath(
                "$['business_unit_users'][?(@['business_unit_id']==68)]['permissions'][0]['permission_name']")
                .value("Account Enquiry - Account Notes"));
    }
    
    @Test
    @DisplayName("Should handle business unit with multiple permissions [PO-857]")
    void getUserState_whenBusinessUnitHasMultiplePermissions_returnsAllPermissions() throws Exception {
        long userIdWithPermissions = 500000000L;
        
        ResultActions actions = mockMvc.perform(get(URL_BASE + "/" + userIdWithPermissions + "/state"));
        
        actions.andExpect(status().isOk())
            .andExpect(jsonPath(
                "$['business_unit_users'][?(@['business_unit_id']==70)]['permissions'][*]['permission_name']",
                        containsInAnyOrder("Account Enquiry - Account Notes", "Account Enquiry")));
    }
}
