package uk.gov.hmcts.reform.opal.service.rolemapping;

import com.azure.storage.blob.BlobClient;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AzureBlobMappingFileClient implements MappingFileClient {

    private final BlobClient blobClient;

    @Override
    public MappingFileSnapshot readSnapshot() {
        String lastModifiedAt = blobClient.getProperties().getLastModified().toString();
        InputStream content = blobClient.openInputStream();
        return new MappingFileSnapshot(lastModifiedAt, content);
    }
}
