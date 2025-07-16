package uk.gov.hmcts.reform.opal.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Entity
@Table(name = "user_entitlements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "userEntitlementId")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class UserEntitlementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_entitlement_id_seq_generator")
    @SequenceGenerator(name = "user_entitlement_id_seq_generator", sequenceName = "user_entitlement_id_seq",
        allocationSize = 1)
    @Column(name = "user_entitlement_id")
    private Long userEntitlementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_unit_user_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private BusinessUnitUserEntity businessUnitUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_function_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private ApplicationFunctionEntity applicationFunction;

    public String getBusinessUnitUserId() {
        return Optional.ofNullable(businessUnitUser)
            .map(BusinessUnitUserEntity::getBusinessUnitUserId).orElse(null);
    }

    public Long getApplicationFunctionId() {
        return Optional.ofNullable(applicationFunction)
            .map(ApplicationFunctionEntity::getApplicationFunctionId).orElse(null);
    }

    public String getFunctionName() {
        return Optional.ofNullable(applicationFunction)
            .map(ApplicationFunctionEntity::getFunctionName).orElse(null);
    }


}
