package uk.gov.hmcts.opal.utils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public final class TestHttpClient {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private TestHttpClient() {
    }

    public static TestHttpResponse get(String url, Map<String, String> headers) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET();

        addHeaders(requestBuilder, headers);

        return send(requestBuilder.build());
    }

    public static TestHttpResponseDetails getWithResponseDetails(String url, Map<String, String> headers) {
        return getWithResponseDetailsInternal(url, headers);
    }

    private static TestHttpResponseDetails getWithResponseDetailsInternal(String url, Map<String, String> headers) {
        // Use this opt-in path when a test needs response headers and exact body bytes for header validation.
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET();

        addHeaders(requestBuilder, headers);

        return sendWithResponseDetails(requestBuilder.build());
    }

    public static TestHttpResponse post(String url, String body, Map<String, String> headers) {
        String requestBody = body == null ? "" : body;
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        addHeaders(requestBuilder, headers);

        return send(requestBuilder.build());
    }

    public static TestHttpResponse put(String url, String body, Map<String, String> headers) {
        String requestBody = body == null ? "" : body;
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .PUT(HttpRequest.BodyPublishers.ofString(requestBody));

        addHeaders(requestBuilder, headers);

        return send(requestBuilder.build());
    }

    public static TestHttpResponse patch(String url, String body, Map<String, String> headers) {
        String requestBody = body == null ? "" : body;
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody));

        addHeaders(requestBuilder, headers);

        return send(requestBuilder.build());
    }

    private static void addHeaders(HttpRequest.Builder requestBuilder, Map<String, String> headers) {
        headers.forEach(requestBuilder::header);
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

    private static TestHttpResponseDetails sendWithResponseDetails(HttpRequest request) {
        try {
            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return new TestHttpResponseDetails(response.statusCode(), response.body(), response.headers());
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
            } catch (JacksonException e) {
                throw new IllegalStateException("Failed to parse JSON response body", e);
            }
        }
    }

    public record TestHttpResponseDetails(int statusCode, byte[] bodyBytes, HttpHeaders headers) {
        public String body() {
            return new String(bodyBytes, StandardCharsets.UTF_8);
        }

        public Optional<String> firstHeader(String headerName) {
            return headers.firstValue(headerName);
        }

        public TestHttpResponse toTestHttpResponse() {
            return new TestHttpResponse(statusCode, body());
        }
    }
}
