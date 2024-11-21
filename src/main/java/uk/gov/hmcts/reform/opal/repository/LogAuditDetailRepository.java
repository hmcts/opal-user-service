package uk.gov.hmcts.reform.opal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.opal.entity.LogAuditDetailEntity;

@Repository
public interface LogAuditDetailRepository extends JpaRepository<LogAuditDetailEntity, Long>,
    JpaSpecificationExecutor<LogAuditDetailEntity> {

}
