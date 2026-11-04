/**
* OPAL Program
*
* MODULE      : link_system_user_to_admin_bu_and_new_admin_role.sql
*
* DESCRIPTION : Link the System user (opal-system-user) to the Admin Business Unit, via new business_unit_users
*               Create new System Admin role and assign permissions (CREATE_INTERFACE_FILES, VIEW_INTERFACE_FILES)
*               Correct domain for Create/View Interface Files Roles
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    -------------------------------------------------------------
* 20/10/2026    T McCallion    1.0         PO-10698 - Create test only role for System User and Admin BU
*
**/

--Create Business Unit Users for the Admin BUs
INSERT INTO business_unit_users (business_unit_user_id, business_unit_id, user_id)
SELECT 'S1' || bu.business_unit_code
     , bu.business_unit_id
     , -1
  FROM business_units bu
  JOIN domain d
    ON d.opal_domain_id = bu.opal_domain_id
   AND d.opal_domain_name IN ('Fines', 'Confiscation', 'File-Handler')
 WHERE bu.business_unit_type = 'Admin'
ORDER BY bu.opal_domain_id
ON CONFLICT (business_unit_user_id)
DO NOTHING;


WITH target_domains AS (
    SELECT d.opal_domain_id
    FROM domain d
    WHERE d.opal_domain_name IN ('Fines', 'Confiscation', 'File-Handler')
),
maybe_inserted_roles AS (
    INSERT INTO roles (
        role_id,
        version_number,
        opal_domain_id,
        role_name,
        application_function_list
    )
    SELECT
        nextval('role_id_seq'),
        1,
        td.opal_domain_id,
        'TEST ONLY - System User - Admin',
        ARRAY['VIEW_INTERFACE_FILES', 'CREATE_INTERFACE_FILES']::t_permissions_enum[]
    FROM target_domains td
    WHERE NOT EXISTS (
        SELECT 1
        FROM roles r
        WHERE r.opal_domain_id = td.opal_domain_id
          AND r.role_name = 'TEST ONLY - System User - Admin'
    )
    RETURNING role_id, opal_domain_id
),
existing_roles AS (
    SELECT DISTINCT ON (td.opal_domain_id)
        r.role_id,
        td.opal_domain_id
      FROM target_domains td
      JOIN roles r ON r.opal_domain_id = td.opal_domain_id
     WHERE r.role_name = 'TEST ONLY - System User - Admin'
    ORDER BY td.opal_domain_id, r.version_number DESC, r.role_id DESC
),
target_roles AS (
    SELECT role_id, opal_domain_id 
      FROM maybe_inserted_roles
    UNION ALL    
    SELECT er.role_id, er.opal_domain_id
      FROM existing_roles er
     WHERE NOT EXISTS (
        SELECT 1 FROM maybe_inserted_roles mir
         WHERE mir.opal_domain_id = er.opal_domain_id
     )
),
target_assignments AS (
    SELECT DISTINCT
        buu.business_unit_user_id,
        tr.role_id
      FROM users u
      JOIN business_unit_users buu
        ON buu.user_id = u.user_id
      JOIN business_units bu
        ON bu.business_unit_id = buu.business_unit_id
      JOIN target_roles tr
        ON tr.opal_domain_id = bu.opal_domain_id
     WHERE u.token_name = 'opal-system-user'
)
INSERT INTO business_unit_user_roles (
    business_unit_user_role_id,
    business_unit_user_id,
    role_id
)
SELECT
    nextval('business_unit_user_role_id_seq'),
    ta.business_unit_user_id,
    ta.role_id
FROM target_assignments ta
ON CONFLICT (business_unit_user_id, role_id)
DO NOTHING;

--Correct domain for Interface Files Roles
UPDATE roles 
   SET opal_domain_id = 1
 WHERE role_name IN ('TEST ONLY - Create Interface Files', 'TEST ONLY - View Interface Files')
   AND opal_domain_id = 5;
