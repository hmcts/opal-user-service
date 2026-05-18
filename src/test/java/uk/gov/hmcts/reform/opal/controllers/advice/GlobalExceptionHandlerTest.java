package uk.gov.hmcts.reform.opal.controllers.advice;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.opal.exception.JsonSchemaValidationException;
import uk.gov.hmcts.reform.opal.exception.ResourceConflictException;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void handleJsonSchemaValidationException_returnsBadRequestProblemDetail() {
        ResponseEntity<ProblemDetail> response = globalExceptionHandler
            .handleJsonSchemaValidationException(new JsonSchemaValidationException("bad schema"));

        assertProblem(response, HttpStatus.BAD_REQUEST, "Bad Request", "json-schema-validation", false);
    }

    @Test
    void handleResourceConflictException_returnsConflictProblemDetail() {
        ResourceConflictException exception = new ResourceConflictException(
            "DraftAccount",
            "123",
            "BusinessUnits mismatch",
            null);

        ResponseEntity<ProblemDetail> response = globalExceptionHandler.handleResourceConflictException(exception);

        assertProblem(response, HttpStatus.CONFLICT, "Conflict", "resource-conflict", false);
        ProblemDetail problemDetail = response.getBody();
        assertEquals("DraftAccount", problemDetail.getProperties().get("resourceType"));
        assertEquals("123", problemDetail.getProperties().get("resourceId"));
        assertEquals("BusinessUnits mismatch", problemDetail.getProperties().get("conflictReason"));
        assertNull(response.getHeaders().getETag());
    }

    @Test
    void handleResourceConflict_withVersioned_addsEtag() {
        ResourceConflictException exception = new ResourceConflictException(
            "DraftAccount",
            "123",
            "BU mismatch",
            () -> new BigInteger("666"));

        ResponseEntity<ProblemDetail> response = globalExceptionHandler.handleResourceConflictException(exception);

        assertProblem(response, HttpStatus.CONFLICT, "Conflict", "resource-conflict", false);
        assertEquals("\"666\"", response.getHeaders().getETag());
    }

    private static void assertProblem(ResponseEntity<ProblemDetail> response, HttpStatus status, String title,
                                      String type, boolean retriable) {
        assertEquals(status, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
        ProblemDetail problemDetail = response.getBody();
        assertNotNull(problemDetail);
        assertEquals(status.value(), problemDetail.getStatus());
        assertEquals(title, problemDetail.getTitle());
        assertEquals("https://hmcts.gov.uk/problems/" + type, problemDetail.getType().toString());
        assertNotNull(problemDetail.getProperties().get("operation_id"));
        assertEquals(retriable, problemDetail.getProperties().get("retriable"));
    }
}
