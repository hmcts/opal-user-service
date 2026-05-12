package uk.gov.hmcts.reform.opal.service.synchronise;

public class SynchronisePermissionsException extends RuntimeException {
    public SynchronisePermissionsException(String message) {
        super(message);
    }

    public SynchronisePermissionsException(String message, Throwable cause) {
        super(message, cause);
    }

    public SynchronisePermissionsException(Throwable cause) {
        super(cause);
    }
}
