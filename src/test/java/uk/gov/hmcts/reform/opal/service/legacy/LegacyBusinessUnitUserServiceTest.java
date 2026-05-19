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
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyBusinessUnitUserId;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyGetBusinessUnitUserIdsRequest;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyGetBusinessUnitUserIdsResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyBusinessUnitUserServiceTest {

    @Mock
    private GatewayService gatewayService;

    @InjectMocks
    private LegacyBusinessUnitUserService legacyBusinessUnitUserService;

    @Test
    void getBusinessUnitUserIds_buildsRequestFromLibraUserIds() {
        GatewayService.Response<LegacyGetBusinessUnitUserIdsResponse> expected = new GatewayService.Response<>(
            HttpStatus.OK,
            LegacyGetBusinessUnitUserIdsResponse.builder()
                .count(2)
                .businessUnitUserIds(List.of(
                    LegacyBusinessUnitUserId.builder()
                        .businessUnitUserId("L066JG")
                        .businessUnitId("66")
                        .build(),
                    LegacyBusinessUnitUserId.builder()
                        .businessUnitUserId("L067JG")
                        .businessUnitId("67")
                        .build()))
                .build());

        when(gatewayService.postToGateway(eq("GetBUUserIdsBySystemUserIds"),
            eq(LegacyGetBusinessUnitUserIdsResponse.class),
            eq(LegacyGetBusinessUnitUserIdsRequest.builder().libraUserIds(List.of("SU001", "SU002")).build()),
            isNull())).thenReturn(expected);

        GatewayService.Response<LegacyGetBusinessUnitUserIdsResponse> result =
            legacyBusinessUnitUserService.getBusinessUnitUserIds(List.of("SU001", "SU002"));

        assertSame(expected, result);
    }

    @Test
    void getBusinessUnitUserIds_passesProvidedRequestToGateway() {
        LegacyGetBusinessUnitUserIdsRequest request =
            LegacyGetBusinessUnitUserIdsRequest.builder().libraUserIds(List.of("SU003")).build();
        GatewayService.Response<LegacyGetBusinessUnitUserIdsResponse> expected =
            new GatewayService.Response<>(HttpStatus.OK, (LegacyGetBusinessUnitUserIdsResponse) null);

        when(gatewayService.postToGateway(eq("GetBUUserIdsBySystemUserIds"),
            eq(LegacyGetBusinessUnitUserIdsResponse.class), eq(request), isNull())).thenReturn(expected);

        GatewayService.Response<LegacyGetBusinessUnitUserIdsResponse> result =
            legacyBusinessUnitUserService.getBusinessUnitUserIds(request);

        assertSame(expected, result);
    }

    @Test
    void getBusinessUnitUserIds_usesLegacyActionType() {
        LegacyGetBusinessUnitUserIdsRequest request =
            LegacyGetBusinessUnitUserIdsRequest.builder().libraUserIds(List.of("SU004")).build();
        GatewayService.Response<LegacyGetBusinessUnitUserIdsResponse> expected =
            new GatewayService.Response<>(HttpStatus.OK, (LegacyGetBusinessUnitUserIdsResponse) null);

        when(gatewayService.postToGateway(eq("GetBUUserIdsBySystemUserIds"),
            eq(LegacyGetBusinessUnitUserIdsResponse.class), eq(request), isNull())).thenReturn(expected);

        legacyBusinessUnitUserService.getBusinessUnitUserIds(request);

        ArgumentCaptor<LegacyGetBusinessUnitUserIdsRequest> requestCaptor =
            ArgumentCaptor.forClass(LegacyGetBusinessUnitUserIdsRequest.class);

        verify(gatewayService).postToGateway(eq("GetBUUserIdsBySystemUserIds"),
            eq(LegacyGetBusinessUnitUserIdsResponse.class), requestCaptor.capture(), isNull());

        assertEquals(List.of("SU004"), requestCaptor.getValue().getLibraUserIds());
    }

    @Test
    void getBusinessUnitUserIds_rejectsNullRequest() {
        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> legacyBusinessUnitUserService.getBusinessUnitUserIds((LegacyGetBusinessUnitUserIdsRequest) null));

        assertEquals("request must not be null", exception.getMessage());
    }

    @Test
    void getBusinessUnitUserIds_returnsExceptionResponse() {
        LegacyGetBusinessUnitUserIdsRequest request =
            LegacyGetBusinessUnitUserIdsRequest.builder().libraUserIds(List.of("SU005")).build();
        GatewayService.Response<LegacyGetBusinessUnitUserIdsResponse> expected = new GatewayService.Response<>(
            HttpStatus.INTERNAL_SERVER_ERROR, new RuntimeException("gateway boom"), "raw exception body");

        when(gatewayService.postToGateway(eq("GetBUUserIdsBySystemUserIds"),
            eq(LegacyGetBusinessUnitUserIdsResponse.class), eq(request), isNull())).thenReturn(expected);

        GatewayService.Response<LegacyGetBusinessUnitUserIdsResponse> result =
            legacyBusinessUnitUserService.getBusinessUnitUserIds(request);

        assertSame(expected, result);
    }

    @Test
    void getBusinessUnitUserIds_returnsLegacyFailureResponse() {
        LegacyGetBusinessUnitUserIdsRequest request =
            LegacyGetBusinessUnitUserIdsRequest.builder().libraUserIds(List.of("SU006")).build();
        GatewayService.Response<LegacyGetBusinessUnitUserIdsResponse> expected =
            new GatewayService.Response<>(HttpStatus.INTERNAL_SERVER_ERROR, "legacy failure body");

        when(gatewayService.postToGateway(eq("GetBUUserIdsBySystemUserIds"),
            eq(LegacyGetBusinessUnitUserIdsResponse.class), eq(request), isNull())).thenReturn(expected);

        GatewayService.Response<LegacyGetBusinessUnitUserIdsResponse> result =
            legacyBusinessUnitUserService.getBusinessUnitUserIds(request);

        assertSame(expected, result);
    }

    @Test
    void getBusinessUnitUserIds_returnsEmbeddedLegacyErrorResponse() {
        LegacyGetBusinessUnitUserIdsRequest request =
            LegacyGetBusinessUnitUserIdsRequest.builder().libraUserIds(List.of("SU007")).build();
        GatewayService.Response<LegacyGetBusinessUnitUserIdsResponse> expected = new GatewayService.Response<>(
            HttpStatus.OK,
            LegacyGetBusinessUnitUserIdsResponse.builder()
                .errorResponse(ErrorResponse.builder().errorCode("ERR001").errorMessage("legacy rejected").build())
                .build());

        when(gatewayService.postToGateway(eq("GetBUUserIdsBySystemUserIds"),
            eq(LegacyGetBusinessUnitUserIdsResponse.class), eq(request), isNull())).thenReturn(expected);

        GatewayService.Response<LegacyGetBusinessUnitUserIdsResponse> result =
            legacyBusinessUnitUserService.getBusinessUnitUserIds(request);

        assertSame(expected, result);
    }
}
