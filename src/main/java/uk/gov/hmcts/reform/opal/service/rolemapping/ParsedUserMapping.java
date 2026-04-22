package uk.gov.hmcts.reform.opal.service.rolemapping;

import java.util.Map;
import java.util.Set;

public record ParsedUserMapping(
    String emailAddress,
    Map<String, Set<String>> businessUnitToRoles
) {
}
