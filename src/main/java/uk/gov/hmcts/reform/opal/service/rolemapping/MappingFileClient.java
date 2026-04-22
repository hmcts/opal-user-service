package uk.gov.hmcts.reform.opal.service.rolemapping;

import java.io.IOException;

public interface MappingFileClient {
    MappingFileSnapshot readSnapshot() throws IOException;
}
