package uk.gov.hmcts.reform.opal.authorisation.aspect;

import lombok.Getter;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.reform.opal.authorisation.model.Permissions;

import java.util.Collection;

@Getter
public class PermissionNotAllowedException
    extends uk.gov.hmcts.opal.common.user.authorisation.exception.PermissionNotAllowedException {

    private final Permissions[] permission;
    private final transient BusinessUnitUser businessUnitUser;

    public PermissionNotAllowedException(Permissions... value) {
        super(value);
        this.permission = value;
        this.businessUnitUser = null;
    }

    public PermissionNotAllowedException(Short buIds, Permissions... value) {
        super(buIds, value);
        this.permission = value;
        this.businessUnitUser = null;
    }

    public PermissionNotAllowedException(Collection<Short> buIds, Permissions... value) {
        super(buIds, value);
        this.permission = value;
        this.businessUnitUser = null;
    }

    public PermissionNotAllowedException(Permissions permission,
                                         BusinessUnitUser businessUnitUser) {
        super(permission + " permission is not enabled for the business unit user: "
                  + businessUnitUser.getBusinessUnitUserId(), businessUnitUser.getBusinessUnitId(), permission);
        this.permission = new Permissions[] {permission};
        this.businessUnitUser = businessUnitUser;
    }
}
