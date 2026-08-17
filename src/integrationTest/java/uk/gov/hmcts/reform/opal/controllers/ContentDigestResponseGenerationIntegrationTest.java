package uk.gov.hmcts.reform.opal.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Content-Digest response generation integration tests")
@TestPropertySource(properties = {
    "opal.common.content-digest.request.enforce=false",
    "opal.common.content-digest.request.auto-generate=true",
    "opal.common.content-digest.response.enforce=false"
})
class ContentDigestResponseGenerationIntegrationTest extends AbstractContentDigestIntegrationTest {

    @Test
    void missingHeader_returnsSuccessWithResponseContentDigest() throws Exception {
        MvcResult result = mockMvc.perform(get(ROOT_ENDPOINT))
            .andExpect(status().isOk())
            .andReturn();

        assertValidResponseDigest(result);
    }

    @Test
    void invalidHeader_returnsContentDigestProblemResponse() throws Exception {
        MvcResult result = mockMvc.perform(post(POST_ENDPOINT)
            .contentType(APPLICATION_JSON)
            .content(POST_BODY)
            .header(CONTENT_DIGEST, invalidDigest()))
            .andExpect(status().isBadRequest())
            .andReturn();

        assertContentDigestProblem(result, "Digest validation failed",
            "Body hash did not match for algorithm: sha-512");
        assertValidResponseDigest(result);
    }

    @Test
    void malformedHeader_returnsContentDigestProblemResponse() throws Exception {
        MvcResult result = mockMvc.perform(post(POST_ENDPOINT)
            .contentType(APPLICATION_JSON)
            .content(POST_BODY)
            .header(CONTENT_DIGEST, malformedDigest()))
            .andExpect(status().isBadRequest())
            .andReturn();

        assertContentDigestProblem(result, "Invalid Content-Digest header",
            "No valid digest entries found in header.");
        assertValidResponseDigest(result);
    }
}
