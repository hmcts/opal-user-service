/**
* OPAL Program
*
* MODULE      : link_opal_test_user_to_admin_bu_for_auto_enforcement.sql
*
* DESCRIPTION : Link the opal-test user to the Admin Business Unit, via new business_unit_users
*               Assign to Auto Enforcement test role
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ---------------------------------------------------------------------------------------
* 20/10/2026    T McCallion    1.0         PO-10702 - Link system test user to the Admin BU and assign Auto Enforcement permission
*
**/

WITH users AS (
    SELECT user_id 
      FROM users 
     WHERE token_name = 'opal-test'
),
ae_role AS (
    SELECT role_id
      FROM roles
     WHERE role_name = 'TEST ONLY - Auto Enforcement' 
),
new_buu AS (
    INSERT INTO business_unit_users (business_unit_user_id, business_unit_id, user_id)
        SELECT 'T1' || bu.business_unit_code,
               bu.business_unit_id,
               u.user_id
          FROM business_units bu
          JOIN domain d
            ON d.opal_domain_id = bu.opal_domain_id
           AND d.opal_domain_name = 'Fines' 
          CROSS JOIN users u
         WHERE bu.business_unit_type = 'Admin'
    RETURNING business_unit_user_id
)
INSERT INTO business_unit_user_roles (business_unit_user_role_id, business_unit_user_id, role_id)
SELECT NEXTVAL('business_unit_user_role_id_seq'),
       nbu.business_unit_user_id,
       r.role_id
  FROM new_buu nbu
  CROSS JOIN ae_role r;