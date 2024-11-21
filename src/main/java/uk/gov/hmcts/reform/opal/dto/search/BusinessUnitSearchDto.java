package uk.gov.hmcts.reform.opal.dto.search;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.opal.dto.ToJsonString;

@Data
@Builder
public class BusinessUnitSearchDto implements ToJsonString {

    private String businessUnitId;
    private String businessUnitName;
    private String businessUnitCode;
    private String businessUnitType;
    private String accountNumberPrefix;
    private String parentBusinessUnitId;

}
