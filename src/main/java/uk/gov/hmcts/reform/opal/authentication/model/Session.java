package uk.gov.hmcts.reform.opal.authentication.model;

public record Session(String sessionId, String accessToken, long accessTokenExpiresIn) {
}
