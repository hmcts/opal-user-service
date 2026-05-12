package uk.gov.hmcts.reform.opal.service.synchronise;

public class RoleMappingCacheLookupException extends RuntimeException {
    public RoleMappingCacheLookupException(String message) {
        super(message);
    }

    public RoleMappingCacheLookupException(String message, Throwable cause) {
        super(message, cause);
    }

    public RoleMappingCacheLookupException(Throwable cause) {
        super(cause);
    }
}
