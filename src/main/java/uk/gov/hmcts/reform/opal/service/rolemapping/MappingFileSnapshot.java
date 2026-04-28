package uk.gov.hmcts.reform.opal.service.rolemapping;

import java.io.InputStream;

public record MappingFileSnapshot(String lastModifiedAt, InputStream content) {
}
