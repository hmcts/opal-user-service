package uk.gov.hmcts.reform.opal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "v_current_roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleEntity {

    @Id
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "version_number", nullable = false)
    private Long versionNumber;

    @ManyToOne
    @JoinColumn(name = "opal_domain_id", nullable = false)
    private DomainEntity domain;

    @Column(name = "role_name", nullable = false)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "application_function_list", nullable = false)
    private List<String> applicationFunctionList;
}
