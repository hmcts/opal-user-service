package uk.gov.hmcts.reform.opal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "business_unit_user_roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class BusinessUnitUserRoleEntity {

    @Id
    @Column(name = "business_unit_user_role_id")
    private Long businessUnitUserRoleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_unit_user_id", nullable = false)
    private BusinessUnitUserEntity businessUnitUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", referencedColumnName = "role_id")
    private RoleEntity role;
}
