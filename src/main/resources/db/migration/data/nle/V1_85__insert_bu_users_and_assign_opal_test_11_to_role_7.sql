/**
* CGI OPAL Program
*
* MODULE      : insert_bu_users_and_assign_opal_test_11_to_role_7.sql
*
* DESCRIPTION : Create business_unit_users for BUs 73, 77 for user opal-test-11 and assign to role 'TEST ONLY - Check and Validate Draft Accounts'
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ---------------------------------------------------------------------------------------
* 20/11/2026    T McCallion    1.0         PO-11694 - DB - Configure a West London draft validator and re-enable APR Cypress tests
*
**/

--Insert business_unit_users records for opal-test-11 and BU's 73,77
INSERT INTO business_unit_users (business_unit_user_id, business_unit_id, user_id)
    SELECT 'L' || LPAD(bu.business_unit_id::TEXT, 3, '0') || 'ZZ' AS business_unit_user_id
         , bu.business_unit_id
         , u.user_id
      FROM business_units bu
     CROSS JOIN (SELECT user_id FROM users WHERE token_name = 'opal-test-11') u
     WHERE bu.business_unit_id IN (73, 77)
ON CONFLICT (business_unit_user_id)
DO NOTHING;


WITH target_domains AS (
    SELECT d.opal_domain_id
    FROM domain d
    WHERE d.opal_domain_name IN ('Fines') --, 'Confiscation')
),
existing_roles AS (
    SELECT DISTINCT ON (td.opal_domain_id)
        r.role_id,
        td.opal_domain_id
      FROM target_domains td
      JOIN roles r ON r.opal_domain_id = td.opal_domain_id
     WHERE r.role_name = 'TEST ONLY - Check and Validate Draft Accounts'
    ORDER BY td.opal_domain_id, r.version_number DESC, r.role_id DESC
),
target_assignments AS (
    SELECT DISTINCT
        buu.business_unit_user_id,
        r.role_id
      FROM users u
      JOIN business_unit_users buu
        ON buu.user_id = u.user_id
      JOIN business_units bu
        ON bu.business_unit_id = buu.business_unit_id
      JOIN existing_roles r
        ON r.opal_domain_id = bu.opal_domain_id
     WHERE u.token_name = 'opal-test-11'
       AND bu.business_unit_id IN (73, 77) -- 73 = 'H&F, K&C (West London)', 77 = 'Lambeth and Southwark'
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
