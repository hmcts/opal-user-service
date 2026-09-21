/**
* OPAL Program
*
* MODULE      : alter_roles_application_function_list_to_enum.sql
*
* DESCRIPTION : Alter role permission list to enum array
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 21/08/2026    C Cho          1.0         PO-5763 - Alter role permission list to enum array
*
**/

CREATE TYPE t_permissions_enum AS ENUM (
    'ACCOUNT_ENQUIRY',
    'ACCOUNT_ENQUIRY_NOTES',
    'ACCOUNT_MAINTENANCE',
    'ACCOUNT_MAINTENANCE_MINOR_CREDITOR',
    'ADD_ACCOUNT_ACTIVITY_NOTES',
    'ADD_AND_REMOVE_PAYMENT_HOLD',
    'AMEND_PAYMENT_TERMS',
    'AUTO_ENFORCEMENT',
    'CHECK_VALIDATE_DRAFT_ACCOUNTS',
    'COLLECTION_ORDER',
    'CONSOLIDATE',
    'CREATE_MANAGE_DRAFT_ACCOUNTS',
    'CREATE_INTERFACE_FILES',
    'ENTER_ENFORCEMENT',
    'OPERATIONAL_REPORT_BY_ENFORCEMENT',
    'OPERATIONAL_REPORT_BY_PAYMENTS',
    'PROCESS_AND_ALLOCATE_PAYMENTS',
    'SEARCH_AND_VIEW_ACCOUNTS',
    'VIEW_CREDITOR_BACS',
    'VIEW_INTERFACE_FILES'
);

DROP VIEW IF EXISTS v_current_roles;

DROP INDEX IF EXISTS roles_application_function_list_gin_idx;

ALTER TABLE roles
    ALTER COLUMN application_function_list DROP DEFAULT;

ALTER TABLE roles
    ALTER COLUMN application_function_list TYPE t_permissions_enum[]
    USING application_function_list::text[]::t_permissions_enum[];

ALTER TABLE roles
    ALTER COLUMN application_function_list SET DEFAULT ARRAY[]::t_permissions_enum[];

CREATE INDEX roles_application_function_list_gin_idx
    ON roles USING gin (application_function_list);