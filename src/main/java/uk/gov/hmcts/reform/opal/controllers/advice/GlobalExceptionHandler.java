package uk.gov.hmcts.reform.opal.controllers.advice;


import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.QueryTimeoutException;
import jakarta.servlet.ServletException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.LazyInitializationException;
import org.hibernate.PropertyValueException;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import uk.gov.hmcts.reform.opal.exception.OpalApiException;
import uk.gov.hmcts.reform.opal.exception.ResourceConflictException;

import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Slf4j(topic = "opal.GlobalExceptionHandler")
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    public static final String DB_UNAVAILABLE_MESSAGE = "Opal Fines Database is currently unavailable";
    public static final String UNKNOWN = "'Unknown'";
    public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";


    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMediaTypeNotAcceptableException(
        HttpMediaTypeNotAcceptableException ex) {

        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.NOT_ACCEPTABLE,
            "Not Acceptable",
            "The requested media type cannot be produced by the server",
            "not-acceptable",
            ex
        );

        return responseWithProblemDetail(HttpStatus.NOT_ACCEPTABLE, problemDetail);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatchException(
        MethodArgumentTypeMismatchException ex) {

        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.NOT_ACCEPTABLE,
            "Not Acceptable",
            "Invalid parameter value format",
            "type-mismatch",
            ex
        );

        problemDetail.setProperty("reason", ex.getMessage());

        return responseWithProblemDetail(HttpStatus.NOT_ACCEPTABLE, problemDetail);
    }

    @ExceptionHandler(PropertyValueException.class)
    public ResponseEntity<ProblemDetail> handlePropertyValueException(PropertyValueException pve) {
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Property Value Error",
            "Invalid or missing value for a required property",
            "property-value-error",
            pve
        );

        // Add additional properties to the problem detail
        problemDetail.setProperty("entity", pve.getEntityName());
        problemDetail.setProperty("property", pve.getPropertyName());

        return responseWithProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, problemDetail);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleHttpMediaTypeNotSupportedException(
        HttpMediaTypeNotSupportedException ex) {

        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Unsupported Media Type",
            "The Content-Type is not supported. Please use application/json",
            "unsupported-media-type",
            ex
        );

        return responseWithProblemDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, problemDetail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadableException(
        HttpMessageNotReadableException ex) {

        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            "The request body could not be read. It may be missing or invalid JSON.",
            "message-not-readable",
            ex
        );

        return responseWithProblemDetail(HttpStatus.BAD_REQUEST, problemDetail);
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ProblemDetail> handleInvalidDataAccessApiUsageException(
        InvalidDataAccessApiUsageException idaaue) {

        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            INTERNAL_SERVER_ERROR,
            "A problem occurred while accessing data",
            "invalid-data-access",
            idaaue
        );

        return responseWithProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, problemDetail);
    }

    @ExceptionHandler(InvalidDataAccessResourceUsageException.class)
    public ResponseEntity<ProblemDetail> handleInvalidDataAccessResourceUsageException(
        InvalidDataAccessResourceUsageException idarue) {

        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            INTERNAL_SERVER_ERROR,
            "A problem occurred with the requested data resource",
            "invalid-resource-usage",
            idarue
        );

        return responseWithProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, problemDetail);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFoundException(
        EntityNotFoundException entityNotFoundException) {

        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.NOT_FOUND,
            "Entity Not Found",
            "The requested entity could not be found",
            "entity-not-found",
            entityNotFoundException
        );

        problemDetail.setProperty("reason", entityNotFoundException.getMessage());


        return responseWithProblemDetail(HttpStatus.NOT_FOUND, problemDetail);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> handleNoSuchElementException(
        NoSuchElementException noSuchElementException) {

        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.NOT_FOUND,
            "No Value Present",
            "The requested element does not exist",
            "no-such-element",
            noSuchElementException
        );

        return responseWithProblemDetail(HttpStatus.NOT_FOUND, problemDetail);
    }

    @ExceptionHandler(OpalApiException.class)
    public ResponseEntity<ProblemDetail> handleOpalApiException(
        OpalApiException opalApiException) {

        HttpStatus status = opalApiException.getError().getHttpStatus();

        ProblemDetail problemDetail = createProblemDetail(
            status,
            status.getReasonPhrase(),
            "An error occurred while processing your request",
            "opal-api-error",
            opalApiException
        );

        return responseWithProblemDetail(status, problemDetail);
    }

    @ExceptionHandler({ServletException.class, TransactionSystemException.class, PersistenceException.class})
    public ResponseEntity<ProblemDetail> handleServletExceptions(Exception ex) {

        if (ex instanceof QueryTimeoutException) {
            ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.REQUEST_TIMEOUT,
                "Request Timeout",
                "The request did not receive a response from the database within the timeout period",
                "query-timeout",
                ex
            );

            return responseWithProblemDetail(HttpStatus.REQUEST_TIMEOUT, problemDetail);
        }

        if (ex instanceof NoResourceFoundException nrfe) {
            ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.NOT_FOUND,
                "Not Found",
                "The requested resource could not be found",
                "resource-not-found",
                nrfe
            );

            return responseWithProblemDetail(HttpStatus.NOT_FOUND, problemDetail);
        }

        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            INTERNAL_SERVER_ERROR,
            "An unexpected error occurred while processing your request",
            "servlet-error",
            ex
        );

        return responseWithProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, problemDetail);
    }

    @ExceptionHandler(PSQLException.class)
    public ResponseEntity<ProblemDetail> handlePsqlException(PSQLException psqlException) {
        if (psqlException.getCause() instanceof ConnectException
            || psqlException.getCause() instanceof UnknownHostException) {

            ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                DB_UNAVAILABLE_MESSAGE,
                "database-unavailable",
                psqlException
            );

            return responseWithProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, problemDetail);
        }

        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            INTERNAL_SERVER_ERROR,
            "A database error occurred while processing your request",
            "database-error",
            psqlException
        );

        return responseWithProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, problemDetail);
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<ProblemDetail> handleDataAccessResourceFailureException(
        DataAccessResourceFailureException dataAccessResourceFailureException) {

        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Service Unavailable",
            DB_UNAVAILABLE_MESSAGE,
            "database-unavailable",
            dataAccessResourceFailureException
        );

        return responseWithProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, problemDetail);
    }

    @ExceptionHandler(LazyInitializationException.class)
    public ResponseEntity<ProblemDetail> handleLazyInitializationException(
        LazyInitializationException lazyInitializationException) {

        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            INTERNAL_SERVER_ERROR,
            "A data access error occurred.",
            "lazy-initialization",
            lazyInitializationException
        );

        return responseWithProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, problemDetail);
    }

    @ExceptionHandler(JpaSystemException.class)
    public ResponseEntity<ProblemDetail> handleJpaSystemException(JpaSystemException jpaSystemException) {
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            INTERNAL_SERVER_ERROR,
            "A persistence error occurred while processing your request",
            "jpa-system-error",
            jpaSystemException
        );

        return responseWithProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, problemDetail);
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException e) {
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            "Invalid arguments were provided in the request",
            "illegal-argument",
            e
        );

        return responseWithProblemDetail(HttpStatus.BAD_REQUEST, problemDetail);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleObjectOptimisticLockingFailureException(
        ObjectOptimisticLockingFailureException e) {

        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.CONFLICT,
            "Conflict",
            "Conflict updating record. Please try again.",
            "optimistic-locking",
            e
        );

        problemDetail.setProperty("resourceType", e.getPersistentClassName());
        problemDetail.setProperty("resourceId",
                                  Optional.ofNullable(e.getIdentifier()).map(Object::toString).orElse(""));

        return responseWithProblemDetail(HttpStatus.CONFLICT, problemDetail);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ProblemDetail> handleResourceConflictException(ResourceConflictException e) {
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.CONFLICT,
            "Conflict",
            "A conflict occurred with the requested resource",
            "resource-conflict",
            e
        );

        problemDetail.setProperty("resourceType", e.getResourceType());
        problemDetail.setProperty("resourceId", e.getResourceId());
        problemDetail.setProperty("conflictReason", e.getConflictReason());

        return responseWithProblemDetail(HttpStatus.CONFLICT, problemDetail);
    }

    private ProblemDetail createProblemDetail(HttpStatus status, String title, String detail,
                                              String typeUri, Throwable exception) {
        String errorId = UUID.randomUUID().toString();

        log.error("Error ID {}: {}", errorId, exception.getMessage());
        log.error("Error ID {}:", errorId, exception);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("https://hmcts.gov.uk/problems/" + typeUri));
        problemDetail.setInstance(URI.create("https://hmcts.gov.uk/problems/instance/" + errorId));

        return problemDetail;
    }

    private ResponseEntity<ProblemDetail> responseWithProblemDetail(HttpStatus status, ProblemDetail problemDetail) {
        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problemDetail);
    }

}
