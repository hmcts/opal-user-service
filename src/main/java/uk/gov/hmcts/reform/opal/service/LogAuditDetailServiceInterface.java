package uk.gov.hmcts.reform.opal.service;

import uk.gov.hmcts.reform.opal.dto.search.LogAuditDetailSearchDto;
import uk.gov.hmcts.reform.opal.entity.LogAuditDetailEntity;

import java.util.List;

public interface LogAuditDetailServiceInterface {

    LogAuditDetailEntity getLogAuditDetail(long logAuditDetailId);

    List<LogAuditDetailEntity> searchLogAuditDetails(LogAuditDetailSearchDto criteria);
}
