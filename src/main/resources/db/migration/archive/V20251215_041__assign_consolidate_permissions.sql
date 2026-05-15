/**
* CGI OPAL Program
*
* MODULE      : assign_consolidate_permissions.sql
*
* DESCRIPTION : Assign new Consolidate permission to business unit user accounts
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    --------    --------    ----------------------------------------------------------------------------------------------------------------------------
* 15/12/2025    C Larkin    1.0         PO-2423 - Users - Assign new permission for Consolidate to the appropriate business units and user accounts.

*
**/
DO $$
DECLARE
    v_app_fn_id BIGINT;
BEGIN

    -- Get the application_function_id from APPLICATION_FUNCTION for the Consolidate permission using STRICT
    SELECT application_function_id 
    INTO STRICT v_app_fn_id 
    FROM application_functions 
    WHERE function_name = 'Consolidate';
    
    --Insert new records, for the system test users, into USER_ENTITLEMENTS for the new APPLICATION_FUNCTION record 
    -- for the Consolidate permission
    WITH max_ue AS (
        SELECT max(user_entitlement_id) AS max_ue_id
          FROM user_entitlements
    )
    INSERT INTO user_entitlements (user_entitlement_id, business_unit_user_id, application_function_id)
        SELECT max_ue.max_ue_id + ROW_NUMBER() OVER (ORDER BY buu.business_unit_user_id) AS user_entitlement_id
             , buu.business_unit_user_id
             , v_app_fn_id AS application_function_id
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