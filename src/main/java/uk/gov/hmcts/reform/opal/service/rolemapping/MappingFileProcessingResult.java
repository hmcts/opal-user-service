package uk.gov.hmcts.reform.opal.service.rolemapping;

import java.util.List;
import java.util.Set;

public record MappingFileProcessingResult(
    List<ParsedUserMapping> validUsers,
    Set<String> invalidEmails
) {
}
