/**
* CGI OPAL Program
*
* MODULE      : assign_operational_report_permissions.sql
*
* DESCRIPTION : Add new operational report permissions
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    --------    --------    -----------------------------------------------------------------
* 19/12/2025    C Cho       1.0         PO-2275 - PO-2276 - Add operational report permissions for enforcement and payments
*
**/

-- Declare variables to store IDs
DO $$
DECLARE
    v_next_id BIGINT;
    v_report_enforcement_id BIGINT;
    v_report_payments_id BIGINT;
BEGIN
    -- Get the next available ID for the new permissions
    SELECT MAX(application_function_id) INTO v_next_id FROM application_functions;
    
    v_report_enforcement_id := v_next_id + 1;
    v_report_payments_id := v_next_id + 2;
    
    INSERT INTO application_functions (application_function_id, function_name)
    VALUES 
        (v_report_enforcement_id, 'Operational report by enforcement'),
        (v_report_payments_id, 'Operational report by payments');
    
    -- Insert new records, for the system test users, into USER_ENTITLEMENTS for both operational report permissions
    WITH max_ue AS (
        SELECT max(user_entitlement_id) AS max_ue_id
          FROM user_entitlements
    )
    INSERT INTO user_entitlements (user_entitlement_id, business_unit_user_id, application_function_id)
        SELECT max_ue.max_ue_id + ROW_NUMBER() OVER (ORDER BY buu.business_unit_user_id) AS user_entitlement_id
             , buu.business_unit_user_id
             , v_report_enforcement_id AS application_function_id
          FROM users u 
          JOIN business_unit_users buu
            ON u.user_id = buu.user_id
         CROSS JOIN max_ue
         WHERE u.token_preferred_username LIKE 'opal-test%@HMCTS.NET'
           AND u.token_preferred_username != 'opal-test-2@HMCTS.NET'
           AND NOT EXISTS (
                           SELECT 1 
                           FROM   business_unit_users b 
                           WHERE  b.user_id = u.user_id 
                           AND    b.business_unit_id = 73
                           AND    b.business_unit_user_id = buu.business_unit_user_id 
                           AND    u.token_preferred_username = 'opal-test-10@HMCTS.NET'
          );

    WITH max_ue AS (
        SELECT max(user_entitlement_id) AS max_ue_id
          FROM user_entitlements
    )
    INSERT INTO user_entitlements (user_entitlement_id, business_unit_user_id, application_function_id)
        SELECT max_ue.max_ue_id + ROW_NUMBER() OVER (ORDER BY buu.business_unit_user_id) AS user_entitlement_id
             , buu.business_unit_user_id
             , v_report_payments_id AS application_function_id
          FROM users u 
          JOIN business_unit_users buu
            ON u.user_id = buu.user_id
         CROSS JOIN max_ue
         WHERE u.token_preferred_username LIKE 'opal-test%@HMCTS.NET'
           AND u.token_preferred_username != 'opal-test-2@HMCTS.NET'
           AND NOT EXISTS (
                           SELECT 1 
                           FROM   business_unit_users b 
                           WHERE  b.user_id = u.user_id 
                           AND    b.business_unit_id = 73
                           AND    b.business_unit_user_id = buu.business_unit_user_id 
                           AND    u.token_preferred_username = 'opal-test-10@HMCTS.NET'
          );
END $$;
