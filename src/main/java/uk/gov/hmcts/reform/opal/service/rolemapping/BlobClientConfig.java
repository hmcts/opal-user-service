package uk.gov.hmcts.reform.opal.service.rolemapping;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class BlobClientConfig {

    private final BlobMappingFileProperties properties;

    @Bean
    public BlobClient blobClient() {
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
            .connectionString(properties.getConnectionString())
            .buildClient();

        BlobContainerClient containerClient = serviceClient.getBlobContainerClient(properties.getContainerName());
        return containerClient.getBlobClient(properties.getBlobName());
    }
}
