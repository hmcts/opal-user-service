package uk.gov.hmcts.reform.opal.authentication.aspect;


import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.common.exceptions.standard.UnauthorizedException;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;
import uk.gov.hmcts.reform.opal.authorisation.model.LogActions;
import uk.gov.hmcts.reform.opal.dto.AddLogAuditDetailDto;
import uk.gov.hmcts.reform.opal.service.opal.LogAuditDetailService;
import uk.gov.hmcts.reform.opal.service.opal.UserStateService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogAuditDetailsAspectTest {

    private static final UserStateV2 USER_STATE = UserStateV2.builder()
        .username("name")
        .userId(123L)
        .build();

    @Mock
    private LogAuditDetailService logAuditDetailService;
    @Mock
    private UserStateService userStateService;

    @InjectMocks
    private LogAuditDetailsAspect logAuditDetailsAspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private LogAuditDetail logAuditDetail;

    @Nested
    class WriteLogAuditDetail {


        @Test
        void writeLogAuditDetail_shouldProceedAndLogAuditDetails() throws Throwable {

            when(userStateService.getUserStateUsingAuthToken()).thenReturn(USER_STATE);
            when(logAuditDetail.action()).thenReturn(LogActions.LOG_IN);
            when(logAuditDetail.defaultJsonRequest()).thenReturn("{}");

            Object expectedReturnValue = new Object();
            when(joinPoint.proceed()).thenReturn(expectedReturnValue);

            Object returnValue = logAuditDetailsAspect.writeLogAuditDetail(joinPoint, logAuditDetail);

            verify(logAuditDetailService).writeLogAuditDetail(any(AddLogAuditDetailDto.class));
            assertEquals(expectedReturnValue, returnValue);
        }

        @Test
        void writeLogAuditDetail_shouldHandleMissingRequestHeaderExceptionGracefully() throws Throwable {
            when(userStateService.getUserStateUsingAuthToken())
                .thenThrow(new UnauthorizedException("test","Authorization"));
            when(logAuditDetail.action()).thenReturn(LogActions.LOG_IN);

            Object expectedReturnValue = new Object();
            when(joinPoint.proceed()).thenReturn(expectedReturnValue);

            Object returnValue = logAuditDetailsAspect.writeLogAuditDetail(joinPoint, logAuditDetail);

            verify(logAuditDetailService, never()).writeLogAuditDetail(any(AddLogAuditDetailDto.class));
            verify(joinPoint).proceed();
            assertEquals(expectedReturnValue, returnValue);
        }

        @Test
        void writeLogAuditDetail_shouldHandleGeneralExceptionGracefully() throws Throwable {
            when(userStateService.getUserStateUsingAuthToken()).thenThrow(new RuntimeException("Test Exception"));
            when(logAuditDetail.action()).thenReturn(LogActions.LOG_OUT);

            Object expectedReturnValue = new Object();
            when(joinPoint.proceed()).thenReturn(expectedReturnValue);

            Object returnValue = logAuditDetailsAspect.writeLogAuditDetail(joinPoint, logAuditDetail);

            verify(logAuditDetailService, never()).writeLogAuditDetail(any(AddLogAuditDetailDto.class));
            verify(joinPoint).proceed();
            assertEquals(expectedReturnValue, returnValue);
        }

    }

    @Nested
    class WriteAuditLog {


        @Test
        void writeAuditLog_shouldWriteAuditLog() {

            when(userStateService.getUserStateUsingAuthToken()).thenReturn(USER_STATE);
            when(logAuditDetail.action()).thenReturn(LogActions.LOG_IN);
            when(logAuditDetail.defaultJsonRequest()).thenReturn("{}");

            logAuditDetailsAspect.writeAuditLog(logAuditDetail);

            ArgumentCaptor<AddLogAuditDetailDto> captor = ArgumentCaptor.forClass(AddLogAuditDetailDto.class);
            verify(logAuditDetailService).writeLogAuditDetail(captor.capture());

            AddLogAuditDetailDto capturedDto = captor.getValue();
            assertEquals(LogActions.LOG_IN, capturedDto.getLogAction());
            assertEquals(123, capturedDto.getUserId());
            assertEquals("{}", capturedDto.getJsonRequest());
        }

        @Test
        void writeAuditLog_shouldHandleMissingRequestHeaderExceptionGracefully() {
            when(userStateService.getUserStateUsingAuthToken())
                .thenThrow(new UnauthorizedException("Test","Authorization"));
            when(logAuditDetail.action()).thenReturn(LogActions.LOG_IN);

            logAuditDetailsAspect.writeAuditLog(logAuditDetail);

            verify(logAuditDetailService, never()).writeLogAuditDetail(any(AddLogAuditDetailDto.class));
        }

        @Test
        void writeAuditLog_shouldHandleGeneralExceptionGracefully() {
            when(userStateService.getUserStateUsingAuthToken()).thenThrow(new RuntimeException("Test Exception"));
            when(logAuditDetail.action()).thenReturn(LogActions.LOG_IN);

            logAuditDetailsAspect.writeAuditLog(logAuditDetail);

            verify(logAuditDetailService, never()).writeLogAuditDetail(any(AddLogAuditDetailDto.class));
        }
    }
}
