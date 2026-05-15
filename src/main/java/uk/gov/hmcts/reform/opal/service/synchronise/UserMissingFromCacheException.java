package uk.gov.hmcts.reform.opal.service.synchronise;

public class UserMissingFromCacheException extends Exception {

    public UserMissingFromCacheException(String message) {
        super(message);
    }
}
