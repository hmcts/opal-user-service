package uk.gov.hmcts.reform.opal.authentication.model;

public record JwtValidationResult(boolean valid, String reason) {

}
