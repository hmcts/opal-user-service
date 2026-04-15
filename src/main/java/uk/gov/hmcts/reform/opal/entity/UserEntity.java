package uk.gov.hmcts.reform.opal.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.opal.common.dto.Versioned;


@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "userId")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class UserEntity implements Versioned {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_id_seq_generator")
    @SequenceGenerator(name = "user_id_seq_generator", sequenceName = "user_id_seq", allocationSize = 1)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "token_preferred_username", length = 100, nullable = false)
    @EqualsAndHashCode.Exclude
    private String username;

    @Column(name = "password", length = 1000)
    @EqualsAndHashCode.Exclude
    private String password;

    @Column(name = "description", length = 300)
    @EqualsAndHashCode.Exclude
    private String description;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "token_subject", length = 100, unique = true)
    private String tokenSubject;

    @Column(name = "token_name", length = 100)
    private String tokenName;

    @Column(name = "version_number")
    @Version
    private Long versionNumber;

    @Column(name = "last_login_date")
    private LocalDateTime lastLoginDate;

    // Use a Set to avoid duplicate entries when fetching across multi-valued joins.
    @OneToMany(mappedBy = "user")
    private Set<BusinessUnitUserEntity> businessUnitUsers;

    @Override
    public BigInteger getVersion() {
        return Optional.ofNullable(versionNumber).map(BigInteger::valueOf).orElse(BigInteger.ZERO);
    }

}
