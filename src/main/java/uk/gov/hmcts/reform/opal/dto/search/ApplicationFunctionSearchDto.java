package uk.gov.hmcts.reform.opal.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.opal.dto.ToJsonString;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationFunctionSearchDto implements ToJsonString {

    private String applicationFunctionId;
    private String functionName;

}
