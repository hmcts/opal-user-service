package uk.gov.hmcts.reform.opal.exception;

import lombok.Getter;
import uk.gov.hmcts.reform.opal.util.Versioned;

import java.util.Optional;

@Getter
public class ResourceConflictException extends RuntimeException {

    private final String resourceType;
    private final String resourceId;
    private final String conflictReason;
    private final Versioned versioned;

    public ResourceConflictException(String resourceType, Object resourceId, String conflictReason,
                                     Versioned versioned) {

        this.resourceType = resourceType;
        this.resourceId = Optional.ofNullable(resourceId).map(Object::toString).orElse("<null>");
        this.conflictReason = conflictReason;
        this.versioned = versioned;
    }
}
