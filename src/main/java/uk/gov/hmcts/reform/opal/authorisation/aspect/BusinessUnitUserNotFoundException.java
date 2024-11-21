package uk.gov.hmcts.reform.opal.authorisation.aspect;

public class BusinessUnitUserNotFoundException extends RuntimeException {

    public BusinessUnitUserNotFoundException(String message) {
        super(message);
    }
}
