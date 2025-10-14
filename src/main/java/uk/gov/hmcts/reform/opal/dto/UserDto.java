package uk.gov.hmcts.reform.opal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.opal.util.Versioned;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto implements Versioned, ToJsonString {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("username")
    private String username;

    @JsonProperty("name")
    private String name;

    @JsonProperty("status")
    private String status;

    @JsonProperty("version")
    private Long version;

}
