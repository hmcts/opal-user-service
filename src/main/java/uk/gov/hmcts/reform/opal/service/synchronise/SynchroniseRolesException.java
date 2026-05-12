package uk.gov.hmcts.reform.opal.service.synchronise;

public class SynchroniseRolesException  extends RuntimeException {
    public SynchroniseRolesException(String message) {
        super(message);
    }

    public SynchroniseRolesException(String message, Throwable cause) {
        super(message, cause);
    }

    public SynchroniseRolesException(Throwable cause) {
        super(cause);
    }
}
