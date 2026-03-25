package uk.gov.hmcts.reform.opal.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.opal.common.dto.ToJsonString;
import uk.gov.hmcts.opal.common.dto.Versioned;

import java.math.BigInteger;
import java.util.Optional;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStateDto implements Versioned, ToJsonString {

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

    @JsonIgnore
    private Long versionNumber;

    @Override
    @JsonProperty("version")
    public BigInteger getVersion() {
        return Optional.ofNullable(versionNumber).map(BigInteger::valueOf).orElse(BigInteger.ZERO);

    }
}
