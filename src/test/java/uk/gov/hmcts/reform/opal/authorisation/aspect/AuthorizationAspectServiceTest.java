package uk.gov.hmcts.reform.opal.authorisation.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.reform.opal.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.reform.opal.authorisation.model.Permission;
import uk.gov.hmcts.reform.opal.authorisation.model.UserState;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AuthorizationAspectService.class)
@ExtendWith(MockitoExtension.class)
class AuthorizationAspectServiceTest {

    static final BusinessUnitUser BUSINESS_UNIT_USER = BusinessUnitUser.builder()
        .businessUnitId((short) 12)
        .businessUnitUserId("BU123")
        .permissions(Set.of(
            Permission.builder()
                .permissionId(1L)
                .permissionName("Notes")
                .build()))
        .build();

    static final UserState USER_STATE = UserState.builder()
        .userId(123L).userName("John Smith")
        .businessUnitUser(Set.of(BUSINESS_UNIT_USER))
        .build();

    @MockBean
    private HttpServletRequest servletRequest;

    @Autowired
    private AuthorizationAspectService authorizationAspectService;


    @Nested
    class GetAccessTokenParam {

        @Mock
        private ProceedingJoinPoint joinPoint;

        @Mock
        private MethodSignature methodSignature;

        @Test
        void testGetAccessTokenParam_exceptionHandling() {
            when(joinPoint.getSignature()).thenThrow(new RuntimeException("Exception"));

            Optional<String> result = authorizationAspectService.getAccessTokenParam(joinPoint);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class GetAuthorization {
        @Test
        void getAuthorization_WhenAuthHeaderValueNotNull_ReturnsOptionalWithValue() {
            String authHeaderValue = "Bearer token";

            Optional<String> result = authorizationAspectService.getAuthorization(authHeaderValue);

            assertEquals(Optional.of(authHeaderValue), result);
        }

        @Test
        void getAuthorization_WhenRequestAttributesNotNull_ReturnsOptionalWithValue() {
            String authHeaderValue = "Bearer token";
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));
            when(servletRequest.getHeader(AuthorizationAspectService.AUTHORIZATION)).thenReturn(authHeaderValue);

            Optional<String> result = authorizationAspectService.getAuthorization(null);

            assertEquals(Optional.of(authHeaderValue), result);
        }

        @Test
        void getAuthorization_WhenRequestAttributesNull_ReturnsOptionalEmpty() {
            RequestContextHolder.setRequestAttributes(null);

            Optional<String> result = authorizationAspectService.getAuthorization(null);

            assertEquals(Optional.empty(), result);
        }
    }

    @Nested
    class GetBusinessUnitUser {
        @Test
        void getBusinessUnitUser_WhenInvalidArguments() {
            Object[] args = {"invalid"};
            String expectedMessage = "Can't infer the role for user John Smith."
                + " Annotated method needs to have arguments of types"
                + " (BusinessUnitUser).";

            BusinessUnitUserNotFoundException exception = assertThrows(
                BusinessUnitUserNotFoundException.class,
                () -> authorizationAspectService.getBusinessUnitUser(args, USER_STATE)
            );

            assertEquals(expectedMessage, exception.getMessage());
        }

        @Test
        void getBusinessUnitUser_WhenRoleArgument() {
            BusinessUnitUser expectedBusinessUnitUser = BUSINESS_UNIT_USER;
            Object[] args = {expectedBusinessUnitUser};

            BusinessUnitUser actualBusinessUnitUser = authorizationAspectService
                .getBusinessUnitUser(args, USER_STATE);

            assertEquals(expectedBusinessUnitUser, actualBusinessUnitUser);
        }
    }

    @Nested
    class GetArgument {
        @Test
        void testGetArgumentWithInvalidArgument() {
            Object[] args = {"someString", 42};

            Optional<UserState> result = authorizationAspectService.getArgument(args, UserState.class);

            assertTrue(result.isEmpty());
        }

        @Test
        void testGetArgumentWithEmptyArgs() {
            Object[] args = {};

            Optional<UserState> result = authorizationAspectService.getArgument(args, UserState.class);

            assertTrue(result.isEmpty());
        }

        @Test
        void testGetStringWithValidArgument() {
            String str = "testString";
            Object[] args = {USER_STATE, str, 42};

            Optional<String> result = authorizationAspectService.getArgument(args, String.class);

            assertTrue(result.isPresent());
            assertEquals(str, result.get());
        }

        @Test
        void testGetUserStateWithValidArgument() {
            String str = "testString";
            Object[] args = {USER_STATE, str, 42};

            Optional<UserState> result = authorizationAspectService.getArgument(args, UserState.class);

            assertTrue(result.isPresent());
            assertEquals(USER_STATE, result.get());
        }
    }
}
