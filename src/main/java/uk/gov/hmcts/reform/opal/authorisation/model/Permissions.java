package uk.gov.hmcts.reform.opal.authorisation.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionDescriptor;
import uk.gov.hmcts.reform.opal.mappers.UserStateMapper;

public enum Permissions {
    CREATE_MANAGE_DRAFT_ACCOUNTS(1, "Create and Manage Draft Accounts"),
    ACCOUNT_ENQUIRY_NOTES(2, "Account Enquiry - Account Notes"),
    ACCOUNT_ENQUIRY(3, "Account Enquiry"),
    COLLECTION_ORDER(4, "Collection Order"),
    CHECK_VALIDATE_DRAFT_ACCOUNTS(5, "Check and Validate Draft Accounts"),
    SEARCH_AND_VIEW_ACCOUNTS(6, "Search and View Accounts");
    public static final Permissions[] DRAFT_ACCOUNT_PERMISSIONS = {
        CREATE_MANAGE_DRAFT_ACCOUNTS, CHECK_VALIDATE_DRAFT_ACCOUNTS
    };

    static Logger log = LoggerFactory.getLogger(UserStateMapper.class);

    public final long id;

    public final String description;

    Permissions(long id, String description) {
        this.id = id;
        this.description = description;
    }

    public PermissionDescriptor getDescriptor() {
        return new PermissionDescriptor() {
            @Override
            public long getId() {
                return id;
            }

            @Override
            public String getDescription() {
                return description;
            }
        };
    }

    public static Permissions toPermissionOrNull(String function) {
        try {
            return Permissions.valueOf(function);
        } catch (IllegalArgumentException e) {
            log.error("Permission could not be mapped: {}", function);
            return null;
        }
    }
}
