package uk.gov.hmcts.reform.opal.authorisation.aspect;

import uk.gov.hmcts.reform.opal.authorisation.model.Permissions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthorizedAnyBusinessUnitUserHasPermission {
    Permissions value();
}
