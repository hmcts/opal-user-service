package uk.gov.hmcts.reform.opal.controllers.advice;

import static uk.gov.hmcts.reform.opal.util.VersionUtils.createETag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.hmcts.opal.common.controllers.advice.OpalProblemDetailFactory;
import uk.gov.hmcts.opal.common.dto.Versioned;
import uk.gov.hmcts.reform.opal.exception.JsonSchemaValidationException;
import uk.gov.hmcts.reform.opal.exception.ResourceConflictException;

import java.util.Optional;

@Slf4j(topic = "opal.GlobalExceptionHandler")
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JsonSchemaValidationException.class)
    public ResponseEntity<ProblemDetail> handleJsonSchemaValidationException(JsonSchemaValidationException ex) {
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            "The request does not conform to the required JSON schema",
            "json-schema-validation",
            false,
            ex
        );
        return responseWithProblemDetail(HttpStatus.BAD_REQUEST, problemDetail);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ProblemDetail> handleResourceConflictException(ResourceConflictException ex) {
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.CONFLICT,
            "Conflict",
            "A conflict occurred with the requested resource",
            "resource-conflict",
            false,
            ex
        );

        problemDetail.setProperty("resourceType", ex.getResourceType());
        problemDetail.setProperty("resourceId", ex.getResourceId());
        problemDetail.setProperty("conflictReason", ex.getConflictReason());

        return responseWithProblemDetail(HttpStatus.CONFLICT, problemDetail, ex.getVersioned());
    }

    private ProblemDetail createProblemDetail(HttpStatus status, String title, String detail,
                                              String typeUri, boolean retry, Throwable exception) {
        return OpalProblemDetailFactory.createProblemDetail(status, title, detail, typeUri, retry, exception, log);
    }

    private ResponseEntity<ProblemDetail> responseWithProblemDetail(HttpStatus status, ProblemDetail problemDetail) {
        return responseWithProblemDetail(status, problemDetail, null);
    }

    private ResponseEntity<ProblemDetail> responseWithProblemDetail(HttpStatus status, ProblemDetail problemDetail,
                                                                    Versioned versioned) {
        BodyBuilder builder = ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON);
        Optional.ofNullable(versioned).ifPresent(v -> builder.eTag(createETag(v)));
        return builder.body(problemDetail);
    }
}
