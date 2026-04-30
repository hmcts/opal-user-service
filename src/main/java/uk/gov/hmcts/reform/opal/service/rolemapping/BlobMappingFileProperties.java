package uk.gov.hmcts.reform.opal.service.rolemapping;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "blob.storage")
public class BlobMappingFileProperties {

    private String connectionString;
    private String containerName;
    private String blobName;

}
