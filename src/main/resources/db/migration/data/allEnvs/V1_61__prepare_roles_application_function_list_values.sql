/**
* OPAL Program
*
* MODULE      : prepare_roles_application_function_list_values.sql
*
* DESCRIPTION : Prepare role permission values for enum conversion
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 21/08/2026    C Cho          1.0         PO-5763 - Prepare role permission values for enum conversion
*
**/

-- ---------------------------------------------------------------------------
-- Replace each old display-name permission value in
-- roles.application_function_list with its new enum value.
--
-- For each row:
--   1. unnest() expands the array into one row per permission.
--   2. WITH ORDINALITY tags each element with its original position (ord)
--      so we can preserve the original order.
--   3. The CASE statement maps each old display-name to its new enum value.
--   4. ARRAY(...) collects the mapped values back into an array.
-- ---------------------------------------------------------------------------

UPDATE roles
   SET application_function_list = ARRAY(
     SELECT CASE perm
                WHEN 'Account Enquiry'                      THEN 'ACCOUNT_ENQUIRY'
                WHEN 'Account Enquiry - Account Notes'      THEN 'ACCOUNT_ENQUIRY_NOTES'
                WHEN 'Account Maintenance'                  THEN 'ACCOUNT_MAINTENANCE'
                WHEN 'Account Maintenance - Minor Creditor' THEN 'ACCOUNT_MAINTENANCE_MINOR_CREDITOR'
                WHEN 'Add Account Activity Notes'           THEN 'ADD_ACCOUNT_ACTIVITY_NOTES'
                WHEN 'Add and Remove payment hold'          THEN 'ADD_AND_REMOVE_PAYMENT_HOLD'
                WHEN 'Amend Payment Terms'                  THEN 'AMEND_PAYMENT_TERMS'
                WHEN 'Auto Enforcement'                     THEN 'AUTO_ENFORCEMENT'
                WHEN 'Check and Validate Draft Accounts'    THEN 'CHECK_VALIDATE_DRAFT_ACCOUNTS'
                WHEN 'Collection Order'                     THEN 'COLLECTION_ORDER'
                WHEN 'Consolidate'                          THEN 'CONSOLIDATE'
                WHEN 'Create and Manage Draft Accounts'     THEN 'CREATE_MANAGE_DRAFT_ACCOUNTS'
                WHEN 'Create Interface Files'               THEN 'CREATE_INTERFACE_FILES'
                WHEN 'Enter Enforcement'                    THEN 'ENTER_ENFORCEMENT'
                WHEN 'Operational report by enforcement'    THEN 'OPERATIONAL_REPORT_BY_ENFORCEMENT'
                WHEN 'Operational report by payments'       THEN 'OPERATIONAL_REPORT_BY_PAYMENTS'
                WHEN 'Process and Allocate Payments'        THEN 'PROCESS_AND_ALLOCATE_PAYMENTS'
                WHEN 'Search and view accounts'             THEN 'SEARCH_AND_VIEW_ACCOUNTS'
                WHEN 'View Creditor BACS'                   THEN 'VIEW_CREDITOR_BACS'
                WHEN 'View Interface Files'                 THEN 'VIEW_INTERFACE_FILES'
                ELSE perm
            END
     FROM unnest(application_function_list) WITH ORDINALITY AS u(perm, ord)
     ORDER BY ord
   );