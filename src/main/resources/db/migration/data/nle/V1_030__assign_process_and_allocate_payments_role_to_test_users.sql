/**
* CGI OPAL Program
*
* MODULE      : assign_process_and_allocate_payments_role_to_test_users.sql
*
* DESCRIPTION : Create the test-only Process and Allocate Payments role and assign it to test users
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    ----------------------------------------------------------------------------
* 15/06/2026    C Cho       1.0         PO-2619 Create Process and Allocate Payments role and assign it to test users.
*
**/

WITH inserted_role AS (
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
        1,
        'TEST ONLY - Process and Allocate Payments',
        ARRAY['Process and Allocate Payments']::varchar(200)[]
    WHERE NOT EXISTS (
        SELECT 1
        FROM roles r
        WHERE r.opal_domain_id = 1
          AND r.role_name = 'TEST ONLY - Process and Allocate Payments'
    )
    RETURNING role_id
),
existing_role AS (
    SELECT r.role_id
    FROM roles r
    WHERE r.role_name = 'TEST ONLY - Process and Allocate Payments'
      AND r.opal_domain_id = 1
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
target_users AS (
    SELECT DISTINCT
        buu.business_unit_user_id
    FROM users u
    JOIN business_unit_users buu
      ON buu.user_id = u.user_id
    JOIN business_units bu
      ON bu.business_unit_id = buu.business_unit_id
    WHERE u.token_name LIKE 'opal-test%'
      AND u.token_name <> 'opal-test-2'
      AND NOT (
          u.token_name = 'opal-test-10'
          AND bu.business_unit_name = 'H&F, K&C (West London)'
      )
),
target_assignments AS (
    SELECT
        tu.business_unit_user_id,
        tr.role_id
    FROM target_users tu
    CROSS JOIN target_role tr
    WHERE NOT EXISTS (
        SELECT 1
        FROM business_unit_user_roles bur
        WHERE bur.business_unit_user_id = tu.business_unit_user_id
          AND bur.role_id = tr.role_id
    )
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
FROM target_assignments ta;
