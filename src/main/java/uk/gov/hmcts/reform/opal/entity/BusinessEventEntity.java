package uk.gov.hmcts.reform.opal.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRawValue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "business_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BusinessEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "business_event_id_seq_generator")
    @SequenceGenerator(name = "business_event_id_seq_generator", sequenceName = "business_event_id_seq",
        allocationSize = 1)
    @Column(name = "business_event_id", nullable = false)
    private Long businessEventId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "event_type", nullable = false, columnDefinition = "t_event_type_enum")
    private BusinessEventLogType eventType;

    @Column(name = "subject_user_id", nullable = false)
    private Long subjectUserId;

    @Column(name = "initiator_user_id", nullable = false)
    private Long initiatorUserId;

    @Column(name = "event_details", nullable = false, columnDefinition = "json")
    @ColumnTransformer(write = "?::json")
    @JsonRawValue
    private String eventDetails;
}
