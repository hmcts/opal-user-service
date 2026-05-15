/**
* CGI OPAL Program
*
* MODULE      : create_new_demo_users_and_assign_permissions.sql
*
* DESCRIPTION : Insert new Users for the Demo environment and assign their permissions
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 21/01/2026    T McCallion    1.0         PO-2720 - Create Opal Demo users and configure permissions
*
**/
DO $$
DECLARE
    v_user_id_1 BIGINT;
    v_user_id_2 BIGINT;
    v_user_id_3 BIGINT;
BEGIN
    -- Get the next available USER_ID for the new users
    SELECT MAX(user_id) + 1 INTO v_user_id_1 FROM users;

    v_user_id_2 = v_user_id_1 + 1;
    v_user_id_3 = v_user_id_1 + 2;
    
    --Insert new Demo users into USERS
    INSERT INTO users (user_id, token_preferred_username, "password", description, opal_domain_id, status, token_subject, token_name, version_number)
    VALUES (v_user_id_1, 'opal-demo-1@HMCTS.NET', NULL, 'Demo account', NULL, 'active', '-aH4x-ITJTjaJUWHRhlClzuw6By1-pUEUGZZHVi4pxo', 'opal-demo-1', NULL)
         , (v_user_id_2, 'opal-demo-2@HMCTS.NET', NULL, 'Demo account', NULL, 'active', 'lw6hGkOpAXSCdja6isQRJr84n9KtBrz3nCgllwG-r_g', 'opal-demo-2', NULL)
         , (v_user_id_3, 'opal-demo-3@HMCTS.NET', NULL, 'Demo account', NULL, 'active', '9EXU_oIluIQXA2tADVq_uan161y871DBcI_xwV7xKDM', 'opal-demo-3', NULL);

   
    --Insert into BUSINESS_UNIT_USERS for the Demo users. All 3 users have access to BUs: 36, 52, 105, 125
    -- The values used for business_unit_users are fictitious and are not real GoB accounts
    INSERT INTO business_unit_users (business_unit_user_id, business_unit_id, user_id)
    VALUES ('L036D1', 36, v_user_id_1)
         , ('L052D1', 52, v_user_id_1)
         , ('L105D1', 105, v_user_id_1)
         , ('L125D1', 125, v_user_id_1)
         , ('L036D2', 36, v_user_id_2)
         , ('L052D2', 52, v_user_id_2)
         , ('L105D2', 105, v_user_id_2)
         , ('L125D2', 125, v_user_id_2)
         , ('L036D3', 36, v_user_id_3)
         , ('L052D3', 52, v_user_id_3)
         , ('L105D3', 105, v_user_id_3)
         , ('L125D3', 125, v_user_id_3);
    

    --Assign permissions (USER_ENTITLEMENTS) for opal-demo-1@HMCTS.NET and opal-demo-2@HMCTS.NET
    --   All permissions for BU's 36, 52, 105. 4 permissions (1,3,5,6) for BU 125 (Surrey)
    WITH max_ue AS (
        SELECT max(user_entitlement_id) AS max_ue_id
          FROM user_entitlements
    ),
    all_app_fun AS (
        SELECT application_function_id 
          FROM application_functions
    )
    INSERT INTO user_entitlements (user_entitlement_id, business_unit_user_id, application_function_id)
        SELECT max_ue.max_ue_id + ROW_NUMBER() OVER (ORDER BY buu.business_unit_user_id, all_app_fun.application_function_id) AS user_entitlement_id
             , buu.business_unit_user_id
             , all_app_fun.application_function_id
          FROM users u 
          JOIN business_unit_users buu
            ON u.user_id = buu.user_id
         CROSS JOIN max_ue
         CROSS JOIN all_app_fun
         WHERE u.token_preferred_username IN ('opal-demo-1@HMCTS.NET','opal-demo-2@HMCTS.NET')
           AND buu.business_unit_id IN (36, 52, 105);

    WITH max_ue AS (
        SELECT max(user_entitlement_id) AS max_ue_id
          FROM user_entitlements
    ),
    all_app_fun AS (
        SELECT application_function_id 
          FROM application_functions
    )
    INSERT INTO user_entitlements (user_entitlement_id, business_unit_user_id, application_function_id)
        SELECT max_ue.max_ue_id + ROW_NUMBER() OVER (ORDER BY buu.business_unit_user_id, all_app_fun.application_function_id) AS user_entitlement_id
             , buu.business_unit_user_id
             , all_app_fun.application_function_id
          FROM users u 
          JOIN business_unit_users buu
            ON u.user_id = buu.user_id
         CROSS JOIN max_ue
         CROSS JOIN all_app_fun
         WHERE u.token_preferred_username IN ('opal-demo-1@HMCTS.NET','opal-demo-2@HMCTS.NET')
           AND buu.business_unit_id = 125 --Surrey
           AND all_app_fun.application_function_id IN (1,3,5,6);


    --Assign permissions (USER_ENTITLEMENTS) for opal-demo-3@HMCTS.NET
    --   4 permissions (1,3,5,6) for BU's 36, 52, 105, 125
    WITH max_ue AS (
        SELECT max(user_entitlement_id) AS max_ue_id
          FROM user_entitlements
    ),
    all_app_fun AS (
        SELECT application_function_id 
          FROM application_functions
    )
    INSERT INTO user_entitlements (user_entitlement_id, business_unit_user_id, application_function_id)
        SELECT max_ue.max_ue_id + ROW_NUMBER() OVER (ORDER BY buu.business_unit_user_id, all_app_fun.application_function_id) AS user_entitlement_id
             , buu.business_unit_user_id
             , all_app_fun.application_function_id
          FROM users u 
          JOIN business_unit_users buu
            ON u.user_id = buu.user_id
         CROSS JOIN max_ue
         CROSS JOIN all_app_fun
         WHERE u.token_preferred_username = 'opal-demo-3@HMCTS.NET'
           AND buu.business_unit_id IN (36, 52, 105, 125)
           AND all_app_fun.application_function_id IN (1,3,5,6);

END $$;