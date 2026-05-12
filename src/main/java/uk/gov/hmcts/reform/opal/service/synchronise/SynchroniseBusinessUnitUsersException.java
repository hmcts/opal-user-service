package uk.gov.hmcts.reform.opal.service.synchronise;

public class SynchroniseBusinessUnitUsersException extends RuntimeException {
    public SynchroniseBusinessUnitUsersException(String message) {
        super(message);
    }

    public SynchroniseBusinessUnitUsersException(String message, Throwable cause) {
        super(message, cause);
    }

    public SynchroniseBusinessUnitUsersException(Throwable cause) {
        super(cause);
    }
}
