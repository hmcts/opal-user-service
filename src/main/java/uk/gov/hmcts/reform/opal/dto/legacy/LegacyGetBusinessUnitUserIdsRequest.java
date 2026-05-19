package uk.gov.hmcts.reform.opal.dto.legacy;

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
public class LegacyGetBusinessUnitUserIdsRequest {

    @Builder.Default
    @JsonProperty("libra_user_ids")
    private List<String> libraUserIds = new ArrayList<>();
}
