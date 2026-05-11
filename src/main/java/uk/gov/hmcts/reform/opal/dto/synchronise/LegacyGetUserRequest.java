package uk.gov.hmcts.reform.opal.dto.synchronise;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegacyGetUserRequest {

    @JsonProperty("email_address")
    private String emailAddress;
}
