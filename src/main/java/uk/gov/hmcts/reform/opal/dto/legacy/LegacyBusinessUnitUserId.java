package uk.gov.hmcts.reform.opal.dto.legacy;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegacyBusinessUnitUserId {

    @JsonProperty("business_unit_user_id")
    @XmlElement(name = "business_unit_user_id")
    private String businessUnitUserId;

    @JsonProperty("business_unit_id")
    @XmlElement(name = "business_unit_id")
    private String businessUnitId;
}
