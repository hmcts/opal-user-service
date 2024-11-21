package uk.gov.hmcts.reform.opal.authorisation.aspect;

import uk.gov.hmcts.reform.opal.authorisation.model.Permissions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The <code>AuthorizedBusinessUnitUserHasPermission</code> annotation is used to authorise or deny execution
 * of a business method
 * based on the role.
 * If the given role has the permission then only execution will be allowed, otherwise PermissionNotAllowedException
 * will be thrown.
 * For example:
 * The role can be one of the argument of the annotated method.
 * <pre>
 *      &#064;AuthorizedBusinessUnitUserHasPermission(Permissions.ACCOUNT_ENQUIRY)
 *      public void businessMethod(BusinessUnitUser role) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthorizedBusinessUnitUserHasPermission {
    Permissions value();
}
