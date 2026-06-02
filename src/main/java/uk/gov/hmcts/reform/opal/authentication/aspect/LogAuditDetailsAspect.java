package uk.gov.hmcts.reform.opal.authentication.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.common.exceptions.standard.UnauthorizedException;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;
import uk.gov.hmcts.reform.opal.dto.AddLogAuditDetailDto;
import uk.gov.hmcts.reform.opal.service.opal.LogAuditDetailService;
import uk.gov.hmcts.reform.opal.service.opal.UserStateService;

@Aspect
@Component
@Slf4j(topic = "opal.LogAuditDetailsAspect")
@RequiredArgsConstructor
public class LogAuditDetailsAspect {

    private final UserStateService userStateService;
    private final LogAuditDetailService logAuditDetailService;

    @Around("@annotation(logAuditDetail)")
    public Object writeLogAuditDetail(ProceedingJoinPoint joinPoint,
                                      LogAuditDetail logAuditDetail
    ) throws Throwable {
        writeAuditLog(logAuditDetail);
        return joinPoint.proceed();
    }

    public void writeAuditLog(LogAuditDetail logAuditDetail) {
        try {
            UserStateV2 userState = userStateService.getUserStateUsingAuthToken();

            AddLogAuditDetailDto logAuditDetailDto = AddLogAuditDetailDto.builder()
                .logAction(logAuditDetail.action())
                .userId(userState.getUserId())
                .jsonRequest(logAuditDetail.defaultJsonRequest())
                .build();

            logAuditDetailService.writeLogAuditDetail(logAuditDetailDto);
            log.info("LogAuditDetails logged action {} for user id {}", logAuditDetail.action(), userState.getUserId());
        } catch (UnauthorizedException exception) {
            log.warn("Can't log action {} details as missing JWT access token", logAuditDetail.action());
        } catch (Exception exception) {
            log.error("Error writing audit log action:: {}", logAuditDetail.action(), exception);
        }
    }

}
