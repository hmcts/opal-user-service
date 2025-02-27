package uk.gov.hmcts.reform.opal.authentication.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.reform.opal.authorisation.model.UserState;

@Builder
@Value
public class SecurityToken {

    @JsonProperty("access_token")
    String accessToken;
    @JsonProperty("user_state")
    UserState userState;
}
