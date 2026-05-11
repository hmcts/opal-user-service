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
public class LegacyBusinessUnitUser {

    @JsonProperty("business_unit_user_id")
    private String businessUnitUserId;

    @JsonProperty("business_unit_id")
    private String businessUnitId;
}
