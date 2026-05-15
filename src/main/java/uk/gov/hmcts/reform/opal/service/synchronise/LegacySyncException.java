package uk.gov.hmcts.reform.opal.service.synchronise;

public class LegacySyncException extends RuntimeException {
    public LegacySyncException(String message) {
        super(message);
    }

    public LegacySyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
