package uk.gov.hmcts.reform.opal.service.rolemapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.azure.storage.blob.BlobClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserRoleUploadServiceTest {

    @Mock
    private BlobClient blobClient;

    @Mock
    private UserRoleMappingParser userRoleMappingParser;

    @InjectMocks
    private UserRoleUploadService service;

    @Test
    void validateAndUploadRoleMappingCsv_uploadsFileWhenAllEmailsAreValid() throws IOException {
        MockMultipartFile csv = csvFile();
        MappingFileProcessingResult result = validMappingResult();
        given(userRoleMappingParser.parse(any())).willReturn(result);

        MappingFileProcessingResult actual = service.validateAndUploadRoleMappingCsv(csv);

        assertSame(result, actual);
        verify(blobClient).upload(any(), eq(csv.getSize()), eq(true));
    }

    @Test
    void validateAndUploadRoleMappingCsv_doesNotUploadWhenEmailsAreInvalid() throws IOException {
        MockMultipartFile csv = csvFile();
        MappingFileProcessingResult result = new MappingFileProcessingResult(List.of(), Set.of("bad@example.com"), Set.of());
        given(userRoleMappingParser.parse(any())).willReturn(result);

        MappingFileProcessingResult actual = service.validateAndUploadRoleMappingCsv(csv);

        assertSame(result, actual);
        verify(blobClient, never()).upload(any(), anyLong(), eq(true));
    }

    @Test
    void validateAndUploadRoleMappingCsv_returnsBadRequestWhenParsingFails() throws IOException {
        MockMultipartFile csv = csvFile();
        given(userRoleMappingParser.parse(any())).willThrow(new IOException("invalid CSV"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> service.validateAndUploadRoleMappingCsv(csv));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Failed to parse role mapping file", exception.getReason());
        verify(blobClient, never()).upload(any(), anyLong(), eq(true));
    }

    @Test
    void validateAndUploadRoleMappingCsv_returnsBadRequestWhenUploadFails() throws IOException {
        MultipartFile csv = mock(MultipartFile.class);
        given(csv.getInputStream())
            .willReturn(new ByteArrayInputStream(new byte[0]))
            .willThrow(new IOException("storage error"));
        MappingFileProcessingResult result = validMappingResult();
        given(userRoleMappingParser.parse(any())).willReturn(result);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> service.validateAndUploadRoleMappingCsv(csv));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Failed to upload role mapping file", exception.getReason());
        verify(blobClient, never()).upload(any(), anyLong(), eq(true));
    }

    private MockMultipartFile csvFile() {
        return new MockMultipartFile(
            "file",
            "mappings.csv",
            "text/csv",
            "email_address,business_unit_id,role_id".getBytes(StandardCharsets.UTF_8)
        );
    }

    private MappingFileProcessingResult validMappingResult() {
        ParsedUserMapping userMapping = new ParsedUserMapping(
            "valid@example.com",
            Map.of("ROLE_MAPPING", Set.of("BUSINESS_UNIT"))
        );
        return new MappingFileProcessingResult(List.of(userMapping), Set.of(), Set.of());
    }
}
