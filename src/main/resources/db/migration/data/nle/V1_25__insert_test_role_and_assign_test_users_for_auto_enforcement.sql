/**
* OPAL Program
*
* MODULE      : insert_test_role_and_assign_test_users_for_auto_enforcement.sql
*
* DESCRIPTION : Create new TEST ONLY role for Auto Enforcement and assign system test users to it
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 05/06/2026    T McCallion    1.0         PO-2451 - User - Create new permission – Auto Enforcement
*
**/

--Insert new TEST ONLY role for Auto Enforcement
WITH new_role AS (
    INSERT INTO roles (role_id, version_number, opal_domain_id, role_name, application_function_list) VALUES
        (NEXTVAL('role_id_seq'), 1, 1, 'TEST ONLY - Auto Enforcement', '{"Auto Enforcement"}')
    RETURNING role_id 
)
--Assign new role to system test users
INSERT INTO business_unit_user_roles (business_unit_user_role_id, business_unit_user_id, role_id)
    SELECT NEXTVAL('business_unit_user_role_id_seq'), buu.business_unit_user_id, new_role.role_id
      FROM users u
      JOIN business_unit_users buu 
        ON buu.user_id = u.user_id
     CROSS JOIN new_role   
     WHERE u.token_preferred_username LIKE 'opal-test%'
       AND u.token_preferred_username NOT LIKE 'opal-test-2@%'
       AND NOT (u.token_preferred_username LIKE 'opal-test-10@%' AND buu.business_unit_id = 73) --H&F, K&C (West London)
;