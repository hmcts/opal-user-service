package uk.gov.hmcts.reform.opal.dto.legacy;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.opal.common.legacy.model.ErrorResponse;
import uk.gov.hmcts.opal.common.legacy.model.HasErrorResponse;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "response")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegacyGetBusinessUnitUserIdsResponse implements HasErrorResponse {

    @JsonProperty("count")
    @XmlElement(name = "count")
    private Integer count;

    @Builder.Default
    @JsonProperty("business_unit_user_ids")
    @XmlElementWrapper(name = "business_unit_user_ids")
    @XmlElement(name = "business_unit_user_ids_element")
    private List<LegacyBusinessUnitUserId> businessUnitUserIds = new ArrayList<>();

    @XmlElement(name = "error_response")
    private ErrorResponse errorResponse;
}
