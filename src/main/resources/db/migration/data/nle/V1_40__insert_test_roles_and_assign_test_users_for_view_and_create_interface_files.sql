/**
* OPAL Program
*
* MODULE      : insert_test_roles_and_assign_test_users_for_view_and_create_interface_files.sql
*
* DESCRIPTION : Create new TEST ONLY roles for View and Create Interface Files (File-Handler) and assign test users
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 01/07/2026    T McCallion    1.0         PO-7204 - User - Create new permissions for Interface Files
*
**/

--Insert new TEST ONLY roles for Interface Files (File-Handler)
WITH new_roles AS (
    INSERT INTO roles (role_id, version_number, opal_domain_id, role_name, application_function_list)
     VALUES (NEXTVAL('role_id_seq'), 1, (SELECT opal_domain_id FROM domain WHERE opal_domain_name = 'File-Handler'),
             'TEST ONLY - View Interface Files', ARRAY['View Interface Files'])
          , (NEXTVAL('role_id_seq'), 1, (SELECT opal_domain_id FROM domain WHERE opal_domain_name = 'File-Handler'),
             'TEST ONLY - Create Interface Files', ARRAY['Create Interface Files'])
    RETURNING role_id
)
--Assign new roles to system test users
INSERT INTO business_unit_user_roles (business_unit_user_role_id, business_unit_user_id, role_id)
    SELECT NEXTVAL('business_unit_user_role_id_seq'), buu.business_unit_user_id, new_roles.role_id
      FROM users u
      JOIN business_unit_users buu 
        ON buu.user_id = u.user_id
     CROSS JOIN new_roles   
     WHERE u.token_preferred_username LIKE 'opal-test%'
       AND u.token_preferred_username NOT LIKE 'opal-test-2@%'
       AND NOT (u.token_preferred_username LIKE 'opal-test-10@%' AND buu.business_unit_id = 73) --H&F, K&C (West London)
;