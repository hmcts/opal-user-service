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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Entity
@Table(name = "business_unit_user_roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "businessUnitUserRoleId")
public class BusinessUnitUserRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "business_unit_user_role_id_seq_generator")
    @SequenceGenerator(
        name = "business_unit_user_role_id_seq_generator",
        sequenceName = "business_unit_user_role_id_seq",
        allocationSize = 1
    )
    @Column(name = "business_unit_user_role_id")
    private Long businessUnitUserRoleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_unit_user_id", nullable = false)
    private BusinessUnitUserEntity businessUnitUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", referencedColumnName = "role_id")
    private RoleEntity role;

    public String getBusinessUnitUserId() {
        return Optional.ofNullable(businessUnitUser)
            .map(BusinessUnitUserEntity::getBusinessUnitUserId).orElse(null);
    }

    public Long getRoleId() {
        return Optional.ofNullable(role)
            .map(RoleEntity::getRoleId)
            .orElse(null);
    }
}
