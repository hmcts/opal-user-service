package uk.gov.hmcts.reform.opal.service.rolemapping;

import com.azure.storage.blob.BlobClient;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleUploadService {

    private final BlobClient blobClient;
    private final UserRoleMappingParser userRoleMappingParser;

    @Transactional
    public MappingFileProcessingResult validateAndUploadRoleMappingCsv(MultipartFile csv) {
        MappingFileProcessingResult roleMappingResponseDto;
        try (Reader reader = new InputStreamReader(csv.getInputStream(), StandardCharsets.UTF_8)) {
            roleMappingResponseDto = userRoleMappingParser.parse(reader);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to upload role mapping file", e);
        }

        if (roleMappingResponseDto.invalidEmails().isEmpty()) {
            try {
                blobClient.upload(csv.getInputStream(), csv.getSize(), true);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to upload role mapping file", e);
            }
        }

        return roleMappingResponseDto;
    }

}
