package uk.gov.hmcts.reform.opal.dto.synchronise;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegacyBusinessUnitUsersResponse {

    @JsonProperty("count")
    private Integer count;

    @Builder.Default
    @JsonProperty("business_unit_user_ids")
    private List<LegacyBusinessUnitUser> businessUnitUsers = new ArrayList<>();
}
