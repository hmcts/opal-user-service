package uk.gov.hmcts.reform.opal.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Content-Digest default configuration integration tests")
class ContentDigestIntegrationTest extends AbstractContentDigestIntegrationTest {

    @Test
    void missingHeader_returnsSuccessWithResponseContentDigest() throws Exception {
        mockMvc.perform(get(ROOT_ENDPOINT).header(CONTENT_DIGEST, validEmptyBodyDigest()))
            .andExpect(status().isOk())
            .andExpect(header().exists(CONTENT_DIGEST));
    }
}
