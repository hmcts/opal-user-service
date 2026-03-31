package uk.gov.hmcts.opal.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public final class TestHttpClient {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private TestHttpClient() {
    }

    public static TestHttpResponse get(String url, Map<String, String> headers) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET();

        headers.forEach(requestBuilder::header);

        return send(requestBuilder.build());
    }

    public static TestHttpResponse post(String url, String body, Map<String, String> headers) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));

        headers.forEach(requestBuilder::header);

        return send(requestBuilder.build());
    }

    private static TestHttpResponse send(HttpRequest request) {
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return new TestHttpResponse(response.statusCode(), response.body());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call test endpoint", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling test endpoint", e);
        }
    }

    public record TestHttpResponse(int statusCode, String body) {
        public String jsonPath(String fieldName) {
            try {
                JsonNode root = OBJECT_MAPPER.readTree(body);
                JsonNode node = root.path(fieldName);
                return node.isMissingNode() || node.isNull() ? null : node.asText();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to parse JSON response body", e);
            }
        }
    }
}
