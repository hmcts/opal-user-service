package uk.gov.hmcts.reform.opal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyBusinessUnitUserId;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.opal.LegacyStubContainerConfig.legacyGatewayUrl;

public final class LegacyWireMockXmlStubHelper {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    private static final int DEFAULT_STUB_PRIORITY = 5;
    private static final String GET_SYSTEM_USER_IDS_BY_EMAIL = "GetSystemUserIdsByEmail";
    private static final String GET_BUU_BY_LIBRA_IDS = "GetBUUserIdsBySystemUserIds";

    private final ObjectMapper objectMapper;
    private final String wireMockAdminBaseUrl;
    private final List<String> mappingIds = new ArrayList<>();

    private LegacyWireMockXmlStubHelper(ObjectMapper objectMapper, String wireMockAdminBaseUrl) {
        this.objectMapper = objectMapper;
        this.wireMockAdminBaseUrl = wireMockAdminBaseUrl;
    }

    public static LegacyWireMockXmlStubHelper initialise(ObjectMapper objectMapper) throws Exception {
        String wireMockAdminBaseUrl = resolveWireMockAdminBaseUrl();
        return new LegacyWireMockXmlStubHelper(objectMapper, wireMockAdminBaseUrl);
    }

    public void registerXmlStub(String actionType, String responseXml) throws Exception {
        registerXmlStub(actionType, responseXml, DEFAULT_STUB_PRIORITY);
    }

    public void registerXmlStub(String actionType, String responseXml, int priority) throws Exception {
        Map<String, Object> mapping = Map.of(
            "priority", priority,
            "request", Map.of(
                "method", "POST",
                "url", "/opal?actionType=" + actionType
            ),
            "response", Map.of(
                "status", 200,
                "headers", Map.of("Content-Type", "application/xml"),
                "body", responseXml
            )
        );
        String responseBody = postToWireMockAdmin("/mappings", objectMapper.writeValueAsString(mapping), 201);
        String mappingId = objectMapper.readTree(responseBody).path("id").asText(null);
        if (mappingId == null || mappingId.isBlank()) {
            throw new IllegalStateException("WireMock did not return a mapping id. responseBody=" + responseBody);
        }
        mappingIds.add(mappingId);
    }

    public void registerBusinessUnitUserLookupStub(List<LegacyBusinessUnitUserId> businessUnitUsers) throws Exception {
        registerBusinessUnitUserLookupStub(businessUnitUsers, DEFAULT_STUB_PRIORITY);
    }

    public void registerSystemUserLookupStub(List<String> libraUserIds) throws Exception {
        registerSystemUserLookupStub(libraUserIds, DEFAULT_STUB_PRIORITY);
    }

    public void registerSystemUserLookupStub(List<String> libraUserIds, int priority) throws Exception {
        Objects.requireNonNull(libraUserIds, "libraUserIds must not be null");
        StringBuilder responseXml = new StringBuilder()
            .append("<LibraUserIds>")
            .append("<count>").append(libraUserIds.size()).append("</count>");
        for (String libraUserId : libraUserIds) {
            responseXml.append("<libra_user_ids>").append(libraUserId).append("</libra_user_ids>");
        }
        responseXml.append("</LibraUserIds>");
        registerXmlStub(GET_SYSTEM_USER_IDS_BY_EMAIL, responseXml.toString(), priority);
    }

    public void registerBusinessUnitUserLookupStub(List<LegacyBusinessUnitUserId> businessUnitUsers, int priority)
        throws Exception {
        Objects.requireNonNull(businessUnitUsers, "businessUnitUsers must not be null");
        StringBuilder responseXml = new StringBuilder()
            .append("<BusinessUnitUserIds>")
            .append("<count>").append(businessUnitUsers.size()).append("</count>");
        for (LegacyBusinessUnitUserId businessUnitUser : businessUnitUsers) {
            responseXml.append("<business_unit_user_ids>")
                .append("<business_unit_user_id>").append(businessUnitUser.getBusinessUnitUserId())
                .append("</business_unit_user_id>")
                .append("<business_unit_id>").append(businessUnitUser.getBusinessUnitId())
                .append("</business_unit_id>")
                .append("</business_unit_user_ids>");
        }
        responseXml.append("</BusinessUnitUserIds>");
        registerXmlStub(GET_BUU_BY_LIBRA_IDS, responseXml.toString(), priority);
    }

    public void clearRegisteredStubs() throws Exception {
        for (String mappingId : mappingIds) {
            deleteFromWireMockAdmin("/mappings/" + mappingId, 200);
        }
        mappingIds.clear();
    }

    private static String resolveWireMockAdminBaseUrl() throws Exception {
        URI gatewayUri = URI.create(legacyGatewayUrl());
        String hostAndPort = gatewayUri.getScheme() + "://" + gatewayUri.getHost() + ":" + gatewayUri.getPort();
        List<String> candidates = List.of(
            hostAndPort + "/__admin",
            legacyGatewayUrl() + "/__admin"
        );
        for (String candidate : candidates) {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(candidate + "/mappings"))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not resolve WireMock admin endpoint for legacy gateway.");
    }

    private String postToWireMockAdmin(String path, String body, int expectedStatus) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(wireMockAdminBaseUrl + path))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != expectedStatus) {
            throw new IllegalStateException(
                "WireMock admin call failed. path=" + path
                    + ", expectedStatus=" + expectedStatus
                    + ", actualStatus=" + response.statusCode()
                    + ", body=" + response.body()
            );
        }
        return response.body();
    }

    private void deleteFromWireMockAdmin(String path, int expectedStatus) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(wireMockAdminBaseUrl + path))
            .timeout(Duration.ofSeconds(10))
            .DELETE()
            .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != expectedStatus) {
            throw new IllegalStateException(
                "WireMock admin delete failed. path=" + path
                    + ", expectedStatus=" + expectedStatus
                    + ", actualStatus=" + response.statusCode()
                    + ", body=" + response.body()
            );
        }
    }
}
