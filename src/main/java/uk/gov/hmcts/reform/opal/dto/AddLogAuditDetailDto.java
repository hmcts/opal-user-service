package uk.gov.hmcts.reform.opal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;
import uk.gov.hmcts.reform.opal.authorisation.model.LogActions;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
public class AddLogAuditDetailDto {

    @NonNull
    private Long userId;
    @NonNull
    private LogActions logAction;
    private String accountNumber;
    private Short businessUnitId;
    @NonNull
    private String jsonRequest;

}
