package uk.gov.hmcts.reform.opal.service.legacy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.opal.common.legacy.model.ErrorResponse;
import uk.gov.hmcts.opal.common.legacy.service.GatewayService;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyGetUserRequest;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyGetUserResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyUserServiceTest {

    @Mock
    private GatewayService gatewayService;

    @InjectMocks
    private LegacyUserService legacyUserService;

    @Test
    void getUser_buildsRequestFromEmailAddress() {
        GatewayService.Response<LegacyGetUserResponse> expected = new GatewayService.Response<>(
            HttpStatus.OK,
            LegacyGetUserResponse.builder().count(2).libraUserIds(java.util.List.of("SU001", "SU002")).build()
        );

        when(gatewayService.postToGateway(
            eq("GetSystemUserIdsByEmail"),
            eq(LegacyGetUserResponse.class),
            eq(LegacyGetUserRequest.builder().emailAddress("legacy.user@hmcts.net").build()),
            isNull())).thenReturn(expected);

        GatewayService.Response<LegacyGetUserResponse> result = legacyUserService.getUser("legacy.user@hmcts.net");

        assertSame(expected, result);
    }

    @Test
    void getUser_passesProvidedRequestToGateway() {
        LegacyGetUserRequest request = LegacyGetUserRequest.builder().emailAddress("another.user@hmcts.net").build();
        GatewayService.Response<LegacyGetUserResponse> expected =
            new GatewayService.Response<>(HttpStatus.OK, (LegacyGetUserResponse) null);

        when(gatewayService.postToGateway(
            eq("GetSystemUserIdsByEmail"),
            eq(LegacyGetUserResponse.class),
            eq(request),
            isNull())).thenReturn(expected);

        GatewayService.Response<LegacyGetUserResponse> result = legacyUserService.getUser(request);

        assertSame(expected, result);
    }

    @Test
    void getUser_usesLegacyActionType() {
        LegacyGetUserRequest request = LegacyGetUserRequest.builder().emailAddress("third.user@hmcts.net").build();
        GatewayService.Response<LegacyGetUserResponse> expected =
            new GatewayService.Response<>(HttpStatus.OK, (LegacyGetUserResponse) null);

        when(gatewayService.postToGateway(
            eq("GetSystemUserIdsByEmail"),
            eq(LegacyGetUserResponse.class),
            eq(request),
            isNull())).thenReturn(expected);

        legacyUserService.getUser(request);

        ArgumentCaptor<LegacyGetUserRequest> requestCaptor = ArgumentCaptor.forClass(LegacyGetUserRequest.class);

        verify(gatewayService).postToGateway(
            eq("GetSystemUserIdsByEmail"),
            eq(LegacyGetUserResponse.class),
            requestCaptor.capture(),
            isNull());

        assertEquals("third.user@hmcts.net", requestCaptor.getValue().getEmailAddress());
    }

    @Test
    void getUser_rejectsNullRequest() {
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> legacyUserService.getUser((LegacyGetUserRequest) null));

        assertEquals("request must not be null", exception.getMessage());
    }

    @Test
    void getUser_returnsExceptionResponse() {
        LegacyGetUserRequest request = LegacyGetUserRequest.builder().emailAddress("exception.user@hmcts.net").build();
        GatewayService.Response<LegacyGetUserResponse> expected = new GatewayService.Response<>(
            HttpStatus.INTERNAL_SERVER_ERROR,
            new RuntimeException("gateway boom"),
            "raw exception body");

        when(gatewayService.postToGateway(
            eq("GetSystemUserIdsByEmail"),
            eq(LegacyGetUserResponse.class),
            eq(request),
            isNull())).thenReturn(expected);

        GatewayService.Response<LegacyGetUserResponse> result = legacyUserService.getUser(request);

        assertSame(expected, result);
    }

    @Test
    void getUser_returnsLegacyFailureResponse() {
        LegacyGetUserRequest request =
            LegacyGetUserRequest.builder().emailAddress("legacy.failure@hmcts.net").build();
        GatewayService.Response<LegacyGetUserResponse> expected =
            new GatewayService.Response<>(HttpStatus.INTERNAL_SERVER_ERROR, "legacy failure body");

        when(gatewayService.postToGateway(
            eq("GetSystemUserIdsByEmail"),
            eq(LegacyGetUserResponse.class),
            eq(request),
            isNull())).thenReturn(expected);

        GatewayService.Response<LegacyGetUserResponse> result = legacyUserService.getUser(request);

        assertSame(expected, result);
    }

    @Test
    void getUser_returnsEmbeddedLegacyErrorResponse() {
        LegacyGetUserRequest request =
            LegacyGetUserRequest.builder().emailAddress("error.response@hmcts.net").build();
        GatewayService.Response<LegacyGetUserResponse> expected = new GatewayService.Response<>(
            HttpStatus.OK,
            LegacyGetUserResponse.builder()
                .errorResponse(ErrorResponse.builder().errorCode("ERR001").errorMessage("legacy rejected").build())
                .build());

        when(gatewayService.postToGateway(
            eq("GetSystemUserIdsByEmail"),
            eq(LegacyGetUserResponse.class),
            eq(request),
            isNull())).thenReturn(expected);

        GatewayService.Response<LegacyGetUserResponse> result = legacyUserService.getUser(request);

        assertSame(expected, result);
    }
}
