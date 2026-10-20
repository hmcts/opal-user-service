/**
* CGI OPAL Program
*
* MODULE      : insert_test_role_and_assign_users_for_manage_alternate_payment_references.sql
*
* DESCRIPTION : Create the test-only Manage Alternate Payment References role and assign required users
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 20/10/2026    T Tang         1.0         PO-10881 - Create new permission for Manage Alternate Payment References
*
**/

WITH target_domains AS (
    SELECT d.opal_domain_id
    FROM domain d
    WHERE d.opal_domain_name = 'Fines'
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
        'TEST ONLY - Manage Alternate Payment References',
        ARRAY['Manage Alternate Payment References']::varchar(200)[]
    FROM target_domains td
    WHERE NOT EXISTS (
        SELECT 1
        FROM roles r
        WHERE r.opal_domain_id = td.opal_domain_id
          AND r.role_name = 'TEST ONLY - Manage Alternate Payment References'
    )
    RETURNING role_id, opal_domain_id
),
existing_roles AS (
    SELECT DISTINCT ON (td.opal_domain_id)
        r.role_id,
        td.opal_domain_id
      FROM target_domains td
      JOIN roles r ON r.opal_domain_id = td.opal_domain_id
     WHERE r.role_name = 'TEST ONLY - Manage Alternate Payment References'
    ORDER BY td.opal_domain_id, r.version_number DESC, r.role_id DESC
),
target_roles AS (
    SELECT role_id, opal_domain_id
      FROM maybe_inserted_roles
    UNION ALL
    SELECT er.role_id, er.opal_domain_id
      FROM existing_roles er
     WHERE NOT EXISTS (
        SELECT 1
          FROM maybe_inserted_roles mir
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
        OR (
            u.token_name LIKE 'opal-test%'
            AND u.token_name <> 'opal-test-2'
            AND NOT (
                u.token_name = 'opal-test-10'
                AND bu.business_unit_name = 'H&F, K&C (West London)'
            )
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
FROM target_assignments ta
ON CONFLICT (business_unit_user_id, role_id)
DO NOTHING;
