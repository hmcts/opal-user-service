package uk.gov.hmcts.reform.opal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;

@Entity
@Table(name = "domain")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DomainEntity {

    @Id
    @Column(name = "opal_domain_id")
    @JdbcTypeCode(Types.SMALLINT)
    @Max(Short.MAX_VALUE)
    @Min(Short.MIN_VALUE)
    private Integer id;

    @Column(name = "opal_domain_name")
    private String name;

}
