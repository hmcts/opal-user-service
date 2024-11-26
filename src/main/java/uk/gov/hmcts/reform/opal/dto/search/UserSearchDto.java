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
public class UserSearchDto implements ToJsonString {

    private String userId;
    private String username;
    private String password;
    private String description;
}
