package uk.gov.hmcts.reform.opal.authorisation.model;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionDescriptor;

@Slf4j
public enum Permissions {
    CREATE_MANAGE_DRAFT_ACCOUNTS(1, "Create and Manage Draft Accounts"),
    ACCOUNT_ENQUIRY_NOTES(2, "Account Enquiry - Account Notes"),
    ACCOUNT_ENQUIRY(3, "Account Enquiry"),
    COLLECTION_ORDER(4, "Collection Order"),
    CHECK_VALIDATE_DRAFT_ACCOUNTS(5, "Check and Validate Draft Accounts"),
    SEARCH_AND_VIEW_ACCOUNTS(6, "Search and view accounts"),
    ACCOUNT_MAINTENANCE(7, "Account Maintenance"),
    ADD_ACCOUNT_ACTIVITY_NOTES(8, "Add Account Activity Notes"),
    AMEND_PAYMENT_TERMS(9, "Amend Payment Terms"),
    ENTER_ENFORCEMENT(10, "Enter Enforcement"),
    VIEW_CREDITOR_BACS(11, "View Creditor BACS"),
    ADD_AND_REMOVE_PAYMENT_HOLD(12, "Add and Remove payment hold"),
    CONSOLIDATE(13, "Consolidate"),
    OPERATIONAL_REPORT_BY_ENFORCEMENT(14, "Operational report by enforcement"),
    OPERATIONAL_REPORT_BY_PAYMENTS(15, "Operational report by payments");

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

    public static Permissions toPermissionOrNull(String functionDescription) {
        for (Permissions permission : Permissions.values()) {
            if (permission.description.equals(functionDescription)) {
                return permission;
            }
        }
        log.error("Permission could not be mapped: {}", functionDescription);
        return null;
    }
}
