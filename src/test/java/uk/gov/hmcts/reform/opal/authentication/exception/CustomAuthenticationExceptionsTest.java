package uk.gov.hmcts.reform.opal.authentication.exception;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import uk.gov.hmcts.opal.common.user.authentication.exception.CustomAuthenticationExceptions;
import uk.gov.hmcts.opal.common.logging.LogUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.opal.common.dto.ToJsonString.getObjectMapper;

class CustomAuthenticationExceptionsTest {

    private CustomAuthenticationExceptions userCustomAuthenticationExceptions;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter output;

    @BeforeEach
    void setUp() throws IOException {
        userCustomAuthenticationExceptions = new CustomAuthenticationExceptions();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        output = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(output));
    }

    @Test
    void commenceShouldReturnUnauthorizedProblemDetail() throws IOException, ServletException {
        AuthenticationException authException = mock(AuthenticationException.class);

        try (MockedStatic<LogUtil> logUtilMock = mockStatic(LogUtil.class)) {
            logUtilMock.when(LogUtil::getOrCreateOpalOperationId).thenReturn("op-auth");

            userCustomAuthenticationExceptions.commence(request, response, authException);
        }

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ProblemDetail body = getObjectMapper().readValue(output.toString(), ProblemDetail.class);
        assertNotNull(body);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), body.getStatus());
        assertEquals("Unauthorized", body.getTitle());
        assertEquals("https://hmcts.gov.uk/problems/unauthorized", body.getType().toString());
        assertEquals("op-auth", body.getProperties().get("operation_id"));
        assertEquals(false, body.getProperties().get("retriable"));
    }

    @Test
    void handleShouldReturnForbiddenProblemDetail() throws IOException, ServletException {
        AccessDeniedException accessDeniedException = mock(AccessDeniedException.class);

        try (MockedStatic<LogUtil> logUtilMock = mockStatic(LogUtil.class)) {
            logUtilMock.when(LogUtil::getOrCreateOpalOperationId).thenReturn("op-forbidden");

            userCustomAuthenticationExceptions.handle(request, response, accessDeniedException);
        }

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ProblemDetail body = getObjectMapper().readValue(output.toString(), ProblemDetail.class);
        assertNotNull(body);
        assertEquals(HttpStatus.FORBIDDEN.value(), body.getStatus());
        assertEquals("Forbidden", body.getTitle());
        assertEquals("https://hmcts.gov.uk/problems/forbidden", body.getType().toString());
        assertEquals("op-forbidden", body.getProperties().get("operation_id"));
        assertEquals(false, body.getProperties().get("retriable"));
    }
}
