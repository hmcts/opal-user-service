package uk.gov.hmcts.reform.opal.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.hmcts.reform.opal.authentication.service.AccessTokenService;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.service.JsonSchemaValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.opal.dto.ToJsonString.toPrettyJson;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Map;

@ActiveProfiles({"integration"})
@Slf4j(topic = "opal.UserPermissionsControllerIntegrationTest")
@Sql(scripts = "classpath:db.insertData/insert_user_state_data.sql", executionPhase = BEFORE_TEST_CLASS)
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=create-drop"})
@DisplayName("UserPermissionsController Integration Test with Security")
class UserPermissionsControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String URL_BASE = "/users";
    private static final String SQL_USER_QUERY = "SELECT * FROM users WHERE user_id = ?";
    private static final String GET_USER_STATE_RESPONSE_JSON = "getUserStateResponse.json";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private JsonSchemaValidationService jsonSchemaValidationService;

    @MockitoSpyBean
    private AccessTokenService accessTokenService;

    @Test
    @DisplayName("Should return 200 and full user state for a user with permissions [PO-857]")
    void getUserState_whenUserHasPermissions_returns200AndCorrectPayload() throws Exception {
        long userIdWithPermissions = 500000000L;

        ResultActions actions = mockMvc.perform(get(URL_BASE + "/" + userIdWithPermissions + "/state"));

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_whenUserHasPermissions_returns200AndCorrectPayload: Response body:\n{}",
                 toPrettyJson(body));

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

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_whenUserExistsButHasNoPermissions_returns200AndEmptyList: Response body:\n{}",
                 toPrettyJson(body));

        actions.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(header().string("ETag", "\"0\""))
            .andExpect(jsonPath("$['user_id']").value(500000001))
            .andExpect(jsonPath("$['username']").value("opal-test-2@HMCTS.NET"))
            .andExpect(jsonPath("$['business_unit_users']", hasSize(0)));
    }

    @Test
    @DisplayName("Should return 200")
    void getUserState_existingUser() throws Exception {
        long userIdWithoutPermissions = 500000003L;

        ResultActions actions = mockMvc.perform(get(URL_BASE + "/" + userIdWithoutPermissions + "/state"));

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_existingUser: Response body:\n{}", toPrettyJson(body));

        actions.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(header().string("ETag", "\"2\""))
            .andExpect(jsonPath("$['user_id']").value(500000003))
            .andExpect(jsonPath("$['username']").value("test-user@HMCTS.NET"))
            .andExpect(jsonPath("$['name']").value("Pablo"))
            .andExpect(jsonPath("$['status']").value("active"))
            .andExpect(jsonPath("$['version']").value(2))
            .andExpect(jsonPath("$['business_unit_users']", hasSize(0)));
    }

    @Test
    @DisplayName("Should return 200 from Authentication Principal and no User Id")
    void getUserState_existingUserViaPrinciple_noUserId() throws Exception {

        JwtAuthenticationToken jwtAuthToken = createJwtPrincipal("jjqwGAERGW43","test-user@HMCTS.NET", "Pablo");
        ResultActions actions = mockMvc.perform(get(URL_BASE + "/state").principal(jwtAuthToken));

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_existingUserViaPrinciple: Response body:\n{}", toPrettyJson(body));

        actions.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(header().string("ETag", "\"2\""))
            .andExpect(jsonPath("$['user_id']").value(500000003))
            .andExpect(jsonPath("$['username']").value("test-user@HMCTS.NET"))
            .andExpect(jsonPath("$['name']").value("Pablo"))
            .andExpect(jsonPath("$['status']").value("active"))
            .andExpect(jsonPath("$['version']").value(2))
            .andExpect(jsonPath("$['business_unit_users']", hasSize(0)));
    }

    @Test
    @DisplayName("Should return 200 from Authentication Principal")
    void getUserState_existingUserViaPrinciple() throws Exception {

        JwtAuthenticationToken jwtAuthToken = createJwtPrincipal("jjqwGAERGW43","test-user@HMCTS.NET", "Pablo");
        ResultActions actions = mockMvc.perform(get(URL_BASE + "/0/state").principal(jwtAuthToken));

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_existingUserViaPrinciple: Response body:\n{}", toPrettyJson(body));

        actions.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(header().string("ETag", "\"2\""))
            .andExpect(jsonPath("$['user_id']").value(500000003))
            .andExpect(jsonPath("$['username']").value("test-user@HMCTS.NET"))
            .andExpect(jsonPath("$['name']").value("Pablo"))
            .andExpect(jsonPath("$['status']").value("active"))
            .andExpect(jsonPath("$['version']").value(2))
            .andExpect(jsonPath("$['business_unit_users']", hasSize(0)));
    }

    @Test
    @DisplayName("Should return 404 Not Found from Authentication Principal")
    void getUserState_existingUserViaPrinciple_doesNotExist() throws Exception {

        JwtAuthenticationToken jwtAuthToken = createJwtPrincipal("invalid_sub","test-user@HMCTS.NET", "Pablo");
        ResultActions actions = mockMvc.perform(get(URL_BASE + "/0/state").principal(jwtAuthToken));

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_existingUserViaPrinciple_doesNotExist: Response body:\n{}",
                 toPrettyJson(body));

        actions.andExpect(status().isNotFound())
            .andExpect(jsonPath("$['reason']").value("User not found with subject: invalid_sub"));
    }

    @Test
    @DisplayName("Should return 409 Conflict from different preferred username")
    void getUserState_existingUserViaPrinciple_conflitPreferred() throws Exception {

        JwtAuthenticationToken jwtAuthToken = createJwtPrincipal("jjqwGAERGW43","different@HMCTS.NET", "Pablo");
        ResultActions actions = mockMvc.perform(get(URL_BASE + "/0/state").principal(jwtAuthToken));

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_existingUserViaPrinciple_doesNotExist: Response body:\n{}",
                 toPrettyJson(body));

        actions.andExpect(status().isConflict())
            .andExpect(header().string("ETag", "\"2\""))
            .andExpect(jsonPath("$['conflictReason']").value(
                "Preferred Username mismatch: token: different@HMCTS.NET, db: test-user@HMCTS.NET"));
    }

    @Test
    @DisplayName("Should return 409 Conflict from different name")
    void getUserState_existingUserViaPrinciple_conflitName() throws Exception {

        JwtAuthenticationToken jwtAuthToken = createJwtPrincipal("jjqwGAERGW43","test-user@HMCTS.NET", "Peter");
        ResultActions actions = mockMvc.perform(get(URL_BASE + "/0/state").principal(jwtAuthToken));

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_existingUserViaPrinciple_doesNotExist: Response body:\n{}",
                 toPrettyJson(body));

        actions.andExpect(status().isConflict())
            .andExpect(header().string("ETag", "\"2\""))
            .andExpect(jsonPath("$['conflictReason']").value("Name mismatch: token: Peter, db: Pablo"));
    }

    @Test
    @DisplayName("Should return 404 Not Found for a user that does not exist [PO-857]")
    void getUserState_whenUserDoesNotExist_returns404() throws Exception {
        long nonExistentUserId = 999999999L;

        ResultActions actions = mockMvc.perform(get(URL_BASE + "/" + nonExistentUserId + "/state"));

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_whenUserDoesNotExist_returns404: Response body:\n{}", toPrettyJson(body));

        actions.andExpect(status().isNotFound())
            .andExpect(jsonPath("$['reason']").value("User not found with id: 999999999"));
    }

    @Test
    @DisplayName("Should return 406 for invalid User ID format [PO-857]")
    void getUserState_whenUserIdFormatIsInvalid_returns406() throws Exception {
        String invalidUserId = "invalidUserId";

        ResultActions actions = mockMvc.perform(get(URL_BASE + "/" + invalidUserId + "/state"));

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_whenUserIdFormatIsInvalid_returns406: Response body:\n{}", toPrettyJson(body));

        actions.andExpect(status().isNotAcceptable());
    }

    @Test
    @DisplayName("Should handle business unit with one permission [PO-857]")
    void getUserState_whenBusinessUnitHasOnePermission_returnsCorrectPermission() throws Exception {
        long userIdWithPermissions = 500000000L;

        ResultActions actions = mockMvc.perform(get(URL_BASE + "/" + userIdWithPermissions + "/state"));

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_whenBusinessUnitHasOnePermission_returnsCorrectPermission: Response body:\n{}",
                 toPrettyJson(body));

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

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_whenBusinessUnitHasMultiplePermissions_returnsAllPermissions: Response body:\n{}",
                 toPrettyJson(body));

        actions.andExpect(status().isOk())
            .andExpect(jsonPath(
                "$['business_unit_users'][?(@['business_unit_id']==70)]['permissions'][*]['permission_name']",
                        containsInAnyOrder("Account Enquiry - Account Notes", "Account Enquiry")));
    }

    @Test
    void testAddUser() throws Exception {

        ResultActions actions = mockMvc.perform(
            post(URL_BASE).header("Authorization", "Bearer " + createSignedToken("kWiw5ddDf32")));

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":testAddUser: Response body:\n{}", toPrettyJson(body));

        actions.andExpect(status().isCreated())
            .andExpect(header().string("ETag", "\"0\""))
            .andExpect(jsonPath("$['user_id']").value(1))
            .andExpect(jsonPath("$['username']").value("j.s@example.com"))
            .andExpect(jsonPath("$['subject']").value("kWiw5ddDf32"))
            .andExpect(jsonPath("$['name']").value("john.smith"))
            .andExpect(jsonPath("$['status']").value("CREATED"))
            .andExpect(jsonPath("$['version']").value(0));

        jsonSchemaValidationService.validateOrError(body, GET_USER_STATE_RESPONSE_JSON);

        Map<String, Object> rowData = jdbcTemplate.queryForMap(SQL_USER_QUERY, 1L);
        log.info(":testAddUser: Inserted into Users table:\n{}", rowData);
        assertEquals(1L, rowData.get("user_id"));
        assertEquals("j.s@example.com", rowData.get("token_preferred_username"));
        assertEquals("kWiw5ddDf32", rowData.get("token_subject"));
        assertEquals("john.smith", rowData.get("token_name"));
        assertEquals("CREATED", rowData.get("status"));
        assertEquals(0L, rowData.get("version_number"));
    }

    @Test
    void testUpdateUser() throws Exception {

        // Check Data in DB before update
        Map<String, Object> rowData = jdbcTemplate.queryForMap(SQL_USER_QUERY, 500000002L);
        log.info(":testUpdateUser: Inserted into Users table:\n{}", rowData);
        assertEquals(500000002L, rowData.get("user_id"));
        assertEquals("update-user@HMCTS.NET", rowData.get("token_preferred_username"));
        assertEquals("BmMfmuTT9pEdG", rowData.get("token_subject"));
        assertNull(rowData.get("token_name"));
        assertEquals("CREATED", rowData.get("status"));
        assertEquals(0L, rowData.get("version_number"));

        // Act
        ResultActions actions = mockMvc.perform(
            put(URL_BASE + "/500000002")
                .header("Authorization", "Bearer " + createSignedToken("2cdF2g3Ds"))
                .header("If-Match", "0"));

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":testUpdateUser: Response body:\n{}", toPrettyJson(body));

        // Assert
        actions.andExpect(status().isOk())
            .andExpect(header().string("ETag", "\"1\""))
            .andExpect(jsonPath("$['user_id']").value(500000002L))
            .andExpect(jsonPath("$['username']").value("j.s@example.com"))
            .andExpect(jsonPath("$['name']").value("john.smith"))
            .andExpect(jsonPath("$['subject']").value("2cdF2g3Ds"))
            .andExpect(jsonPath("$['status']").value("CREATED"))
            .andExpect(jsonPath("$['version']").value(1));

        jsonSchemaValidationService.validateOrError(body, GET_USER_STATE_RESPONSE_JSON);

        // Check data in DB after update
        rowData = jdbcTemplate.queryForMap(SQL_USER_QUERY, 500000002L);
        log.info(":testUpdateUser: Inserted into Users table:\n{}", rowData);
        assertEquals(500000002L, rowData.get("user_id"));
        assertEquals("j.s@example.com", rowData.get("token_preferred_username"));
        assertEquals("2cdF2g3Ds", rowData.get("token_subject"));
        assertEquals("john.smith", rowData.get("token_name"));
        assertEquals("CREATED", rowData.get("status"));
        assertEquals(1L, rowData.get("version_number"));
    }

    @Test
    void testUpdateUser_2() throws Exception {

        // Check Data in DB before update
        Map<String, Object> rowData = jdbcTemplate.queryForMap(SQL_USER_QUERY, 500000005L);
        log.info(":testUpdateUser: Inserted into Users table:\n{}", rowData);
        assertEquals(500000005L, rowData.get("user_id"));
        assertEquals("update-user@HMCTS.NET", rowData.get("token_preferred_username"));
        assertEquals("QeJjwoWnY-kBmMfm", rowData.get("token_subject"));
        assertEquals("Pablo", rowData.get("token_name"));
        assertEquals("active", rowData.get("status"));
        assertEquals(7L, rowData.get("version_number"));

        // Act
        ResultActions actions = mockMvc.perform(
            put(URL_BASE)
                .header("Authorization", "Bearer " + createSignedToken("QeJjwoWnY-kBmMfm"))
                .header("If-Match", "7"));

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":testUpdateUser: Response body:\n{}", toPrettyJson(body));

        // Assert
        actions.andExpect(status().isOk())
            .andExpect(header().string("ETag", "\"8\""))
            .andExpect(jsonPath("$['user_id']").value(500000005L))
            .andExpect(jsonPath("$['username']").value("j.s@example.com"))
            .andExpect(jsonPath("$['name']").value("john.smith"))
            .andExpect(jsonPath("$['status']").value("active"))
            .andExpect(jsonPath("$['version']").value(8));

        jsonSchemaValidationService.validateOrError(body, GET_USER_STATE_RESPONSE_JSON);

        // Check data in DB after update
        rowData = jdbcTemplate.queryForMap(SQL_USER_QUERY, 500000005L);
        log.info(":testUpdateUser: Inserted into Users table:\n{}", rowData);
        assertEquals(500000005L, rowData.get("user_id"));
        assertEquals("j.s@example.com", rowData.get("token_preferred_username"));
        assertEquals("QeJjwoWnY-kBmMfm", rowData.get("token_subject"));
        assertEquals("john.smith", rowData.get("token_name"));
        assertEquals("active", rowData.get("status"));
        assertEquals(8L, rowData.get("version_number"));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when invalid JWT is provided [PO-1894]")
    void updateUser_whenInvalidJwtProvided_returns401() throws Exception {
        mockMvc.perform(put(URL_BASE + "/500000002")
                            .header("Authorization", "Bearer invalid.token.value")
                            .header("If-Match", "0")
                            .accept(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 409 Conflict when If-Match version does not match DB version [PO-1894]")
    void updateUser_whenVersionMismatch_returns409() throws Exception {
        mockMvc.perform(put(URL_BASE + "/500000002")
                            .header("Authorization", "Bearer " + createSignedToken("QeJjwoWnY-kBmMfm"))
                            .header("If-Match", "1") // mismatch
                            .accept(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error when JWT contains null claims [PO-1894]")
    void updateUser_whenJwtHasNullClaims_returns500() throws Exception {
        jdbcTemplate.update("UPDATE users SET version_number = 0 WHERE user_id = 500000004");

        // Build a JWT that will cause a NullPointerException in the service
        KeyPair keyPair = generateKeyPair();
        String token = JWT.create()
            .withHeader(Map.of("typ", "JWT", "alg", "RS256"))
            .withSubject("QeJjwoWnY-kBmMfm")
            .withIssuer("Opal")
            .withExpiresAt(Instant.now().plusSeconds(3600))
            .withIssuedAt(Instant.now().minusSeconds(3600))
            .withClaim("preferred_username", (String) null)
            .sign(Algorithm.RSA256(
                (RSAPublicKey) keyPair.getPublic(),
                (RSAPrivateKey) keyPair.getPrivate()));

        try {
            mockMvc.perform(put(URL_BASE + "/500000004")  // or another user ID with version 0
                                .header("Authorization", "Bearer " + token)
                                .header("If-Match", "0")
                                .accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isInternalServerError());
        } catch (jakarta.servlet.ServletException ex) {
            assertTrue(ex.getCause() instanceof NullPointerException,
                       "Expected NullPointerException when JWT claim is null");
        }
    }

    @Test
    @DisplayName("Should return 404 Not Found when updating a non-existent user [PO-1894]")
    void updateUser_whenUserDoesNotExist_returns404() throws Exception {
        mockMvc.perform(put(URL_BASE + "/9999999999") // user not in DB
                            .header("Authorization", "Bearer " + createSignedToken("QeJjwoWnY-kBmMfm"))
                            .header("If-Match", "0")
                            .accept(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 408 Request Timeout when update request exceeds time limit [PO-1894]")
    void updateUser_whenRequestTimesOut_returns408() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.REQUEST_TIMEOUT, "Request timed out"))
            .when(accessTokenService).extractClaims(anyString());

        mockMvc.perform(put(URL_BASE + "/500000002")
                            .header("Authorization", "Bearer fake.token")
                            .header("If-Match", "0")
                            .accept(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(status().isRequestTimeout())
            .andExpect(content().string(""));
    }

    @Test
    @DisplayName("Should return 503 Service Unavailable when dependent service is down [PO-1894]")
    void updateUser_whenServiceUnavailable_returns503() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable"))
            .when(accessTokenService).extractClaims(anyString());

        mockMvc.perform(put(URL_BASE + "/500000002")
                            .header("Authorization", "Bearer fake.token")
                            .header("If-Match", "0")
                            .accept(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(status().isServiceUnavailable())
            .andExpect(content().string(""));
    }

    private String createSignedToken(String subject) throws NoSuchAlgorithmException {
        Builder builder = createJwtBuilder(subject);
        KeyPair keyPair = generateKeyPair();
        return builder
            .sign(Algorithm.RSA256((RSAPublicKey)keyPair.getPublic(), (RSAPrivateKey)keyPair.getPrivate()));
    }

    private Builder createJwtBuilder(String subject) {
        return JWT.create()
            .withHeader(Map.of("typ", "JWT", "alg", "RS256"))
            .withSubject(subject)
            .withIssuer("Opal")
            .withExpiresAt(Instant.now().plusSeconds(3600))
            .withIssuedAt(Instant.now().minusSeconds(3600))
            .withClaim("name", "john.smith")
            .withClaim("preferred_username", "j.s@example.com");
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    private JwtAuthenticationToken createJwtPrincipal(String sub, String preferred, String name) {
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

        return new JwtAuthenticationToken(jwt);
    }

}
