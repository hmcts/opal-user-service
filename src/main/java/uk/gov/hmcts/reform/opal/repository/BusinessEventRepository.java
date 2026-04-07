package uk.gov.hmcts.reform.opal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.opal.entity.BusinessEventEntity;

@Repository
public interface BusinessEventRepository extends JpaRepository<BusinessEventEntity, Long> {
}
