package uk.gov.hmcts.reform.opal.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.opal.common.dto.ToJsonString.toPrettyJson;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Map;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.opal.common.user.authentication.service.AccessTokenService;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.service.JsonSchemaValidationService;

@ActiveProfiles({"integration"})
@Slf4j(topic = "opal.UserPermissionsControllerIntegrationTest")
@Sql(scripts = "classpath:db.insertData/insert_user_state_data.sql", executionPhase = BEFORE_TEST_CLASS)
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=create-drop"})
@DisplayName("UserPermissionsController 'Add' and 'Update' Integration Tests with Security")
class UserPermissionsControllerOtherIntegrationTest extends AbstractIntegrationTest {

    private static final String URL_BASE = "/users";
    private static final String SQL_USER_QUERY = "SELECT * FROM users WHERE user_id = ?";
    private static final String GET_USER_STATE_RESPONSE_JSON = "getUserStateResponse.json";

    @MockitoSpyBean
    private JsonSchemaValidationService jsonSchemaValidationService;

    @MockitoSpyBean
    private AccessTokenService accessTokenService;

    @Test
    void addUser() throws Exception {

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
    void addUser_whenNonUniqueTokenSubject_givesBadRequest() throws Exception {

        final String exitingTokenSubject = "GfsHbIMt49WjQ"; //used by opal-test-2@HMCTS.NET

        mockMvc.perform(
            post(URL_BASE).header("Authorization", "Bearer " + createSignedToken(exitingTokenSubject)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$['title']").value("Conflict"))
            .andExpect(jsonPath("$['detail']").value("Data integrity violation with the requested resource"))
            .andExpect(jsonPath("$['constraintViolated']").value("users_token_subject_key"));
    }

    @Test
    void updateUser_ok_1() throws Exception {

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
    void updateUser_ok_2() throws Exception {

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
            assertInstanceOf(NullPointerException.class, ex.getCause(),
                "Expected NullPointerException when JWT claim is null"
            );
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
