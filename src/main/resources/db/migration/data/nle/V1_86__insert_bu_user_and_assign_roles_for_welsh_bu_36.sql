/**
* CGI OPAL Program
*
* MODULE      : insert_bu_user_and_assign_roles_for_welsh_bu_36.sql
*
* DESCRIPTION : Configure a creator and a checker test user in Welsh BU 36 so draft accounts can be
*               created by one user and checked and validated by another.
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ---------------------------------------------------------------------------------------
* 23/11/2026    S Abbasi       1.0         PO-11640 - DB - Configure submit and check users for Welsh BU 36
*
**/


-- ================================================================
-- BU 36 = 'LCIS (South + Mid Glamorgan)', code 0113, welsh_language = true
--
-- opal-test-5 (creator):
--   New BU 36 mapping L036FO: TEST ONLY - Create and Manage Draft Accounts
--   Existing BU 60 (Dyfed Powys) mapping L060FO is unchanged
--
-- opal-test-8 (checker), existing BU 36 mapping L036DO:
--   Add TEST ONLY - Account Maintenance
--   Add TEST ONLY - Add Account Activity Notes
--   Existing roles are unchanged
-- ================================================================


-- Insert business_unit_users record for opal-test-5 and BU 36
INSERT INTO business_unit_users (business_unit_user_id, business_unit_id, user_id)
    SELECT 'L' || LPAD(bu.business_unit_id::TEXT, 3, '0') || 'FO' AS business_unit_user_id
         , bu.business_unit_id
         , u.user_id
      FROM business_units bu
     CROSS JOIN (SELECT user_id FROM users WHERE token_name = 'opal-test-5') u
     WHERE bu.business_unit_id = 36
ON CONFLICT (business_unit_user_id)
DO NOTHING;


-- Assign the existing TEST ONLY roles to the BU 36 mappings
WITH required_assignments (
    token_name,
    business_unit_id,
    role_name
) AS (
    VALUES
        ('opal-test-5', 36, 'TEST ONLY - Create and Manage Draft Accounts'),
        ('opal-test-8', 36, 'TEST ONLY - Account Maintenance'),
        ('opal-test-8', 36, 'TEST ONLY - Add Account Activity Notes')
),
existing_roles AS (
    SELECT DISTINCT ON (r.role_name)
        r.role_id,
        r.role_name,
        r.opal_domain_id
      FROM roles r
      JOIN domain d
        ON d.opal_domain_id = r.opal_domain_id
     WHERE d.opal_domain_name = 'Fines'
       AND r.role_name IN (SELECT role_name FROM required_assignments)
    ORDER BY r.role_name, r.version_number DESC, r.role_id DESC
),
target_assignments AS (
    SELECT DISTINCT
        buu.business_unit_user_id,
        er.role_id
      FROM required_assignments ra
      JOIN users u
        ON u.token_name = ra.token_name
      JOIN business_unit_users buu
        ON buu.user_id = u.user_id
       AND buu.business_unit_id = ra.business_unit_id
      JOIN business_units bu
        ON bu.business_unit_id = buu.business_unit_id
      JOIN existing_roles er
        ON er.role_name = ra.role_name
       AND er.opal_domain_id = bu.opal_domain_id
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
