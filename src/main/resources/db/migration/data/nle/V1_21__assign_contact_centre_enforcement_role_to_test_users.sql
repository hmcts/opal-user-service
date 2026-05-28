/**
* CGI OPAL Program
*
* MODULE      : assign_contact_centre_enforcement_role_to_test_users.sql
*
* DESCRIPTION : Assign the existing Contact Centre Enforcement role to opal-user-201 through
*               opal-user-1600
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    ----------------------------------------------------------------------------
* 27/05/2026    C Cho       1.0         PO-3916 Assign Contact Centre Enforcement role to performance test users.
*
**/

WITH current_roles AS (
    SELECT r.role_id,
           r.version_number,
           r.opal_domain_id,
           r.role_name,
           r.application_function_list
      FROM roles r
      JOIN (
          SELECT role_name,
                 opal_domain_id,
                 MAX(version_number) AS max_version_number
            FROM roles
           GROUP BY role_name, opal_domain_id
      ) latest_role
        ON latest_role.role_name = r.role_name
       AND latest_role.opal_domain_id = r.opal_domain_id
       AND latest_role.max_version_number = r.version_number
),
target_role AS (
    SELECT
        current_roles.role_id
    FROM current_roles
    WHERE current_roles.role_name = 'Contact Centre Enforcement'
),
target_users AS (
    SELECT DISTINCT
        u.user_id,
        buu.business_unit_user_id
    FROM users u
    JOIN business_unit_users buu
      ON buu.user_id = u.user_id
    WHERE substring(
          u.token_preferred_username FROM '^opal-user-([0-9]+)@dev\.platform\.hmcts\.net$'
      )::integer BETWEEN 201 AND 1600
),
target_assignments AS (
    SELECT DISTINCT
        target_users.business_unit_user_id,
        target_role.role_id
    FROM target_users
    CROSS JOIN target_role
    WHERE NOT EXISTS (
        SELECT 1
        FROM business_unit_user_roles bur
        WHERE bur.business_unit_user_id = target_users.business_unit_user_id
          AND bur.role_id = target_role.role_id
    )
)
INSERT INTO business_unit_user_roles
(
    business_unit_user_role_id,
    business_unit_user_id,
    role_id
)
SELECT
    nextval('business_unit_user_role_id_seq'),
    target_assignments.business_unit_user_id,
    target_assignments.role_id
FROM target_assignments;
