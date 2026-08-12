/**
* CGI OPAL Program
*
* MODULE      : insert_test_role_and_assign_users_for_account_maintenance_minor_creditor.sql
*
* DESCRIPTION : Create the test-only Account Maintenance - Minor Creditor role and assign required users
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    ----------------------------------------------------------------------------
* 10/08/2026    C Cho       1.0         PO-9736 Create Account Maintenance - Minor Creditor role and user assignments.
*
**/

WITH fines_domain AS (
    SELECT d.opal_domain_id
    FROM domain d
    WHERE d.opal_domain_name = 'Fines'
),
inserted_role AS (
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
        fd.opal_domain_id,
        'TEST ONLY - Account Maintenance - Minor Creditor',
        ARRAY['Account Maintenance - Minor Creditor']::varchar(200)[]
    FROM fines_domain fd
    WHERE NOT EXISTS (
        SELECT 1
        FROM roles r
        WHERE r.opal_domain_id = fd.opal_domain_id
          AND r.role_name = 'TEST ONLY - Account Maintenance - Minor Creditor'
    )
    RETURNING role_id
),
existing_role AS (
    SELECT r.role_id
    FROM roles r
    JOIN fines_domain fd
      ON fd.opal_domain_id = r.opal_domain_id
    WHERE r.role_name = 'TEST ONLY - Account Maintenance - Minor Creditor'
    ORDER BY r.version_number DESC, r.role_id DESC
    LIMIT 1
),
target_role AS (
    SELECT role_id
    FROM inserted_role
    UNION ALL
    SELECT er.role_id
    FROM existing_role er
    WHERE NOT EXISTS (
        SELECT 1
        FROM inserted_role
    )
),
system_user_assignments AS (
    SELECT DISTINCT
        buu.business_unit_user_id,
        tr.role_id
    FROM users u
    JOIN business_unit_users buu
      ON buu.user_id = u.user_id
    CROSS JOIN target_role tr
    WHERE u.token_name = 'opal-system-user'
),
target_test_user_assignments AS (
    SELECT DISTINCT
        buu.business_unit_user_id,
        tr.role_id
    FROM users u
    JOIN business_unit_users buu
      ON buu.user_id = u.user_id
    JOIN business_units bu
      ON bu.business_unit_id = buu.business_unit_id
    CROSS JOIN target_role tr
    WHERE u.token_name LIKE 'opal-test%'
      AND u.token_name <> 'opal-test-2'
      AND NOT (
          u.token_name = 'opal-test-10'
          AND bu.business_unit_name = 'H&F, K&C (West London)'
      )
),
target_assignments AS (
    SELECT
        sua.business_unit_user_id,
        sua.role_id
    FROM system_user_assignments sua
    UNION
    SELECT
        ttua.business_unit_user_id,
        ttua.role_id
    FROM target_test_user_assignments ttua
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
