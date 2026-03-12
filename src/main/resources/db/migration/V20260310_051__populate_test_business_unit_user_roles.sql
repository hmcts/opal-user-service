/**
* CGI OPAL Program
*
* MODULE      : populate_test_business_unit_user_roles.sql
*
* DESCRIPTION : Populates BUSINESS_UNIT_USER_ROLES records for test-only user role assignments.
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    ----------------------------------------------------------------------------
* 10/03/2026    C Cho       1.0         PO-2827 Assign test-only BU users to domain-consistent roles based on obsoleted USER_ENTITLEMENTS data.
*
**/

WITH test_user_role_pairs AS (
    SELECT DISTINCT buu.business_unit_user_id,
                    r.role_id
      FROM users u
      JOIN business_unit_users buu
        ON buu.user_id = u.user_id
      JOIN business_units bu
        ON bu.business_unit_id = buu.business_unit_id
      JOIN user_entitlements ue
        ON ue.business_unit_user_id = buu.business_unit_user_id
      JOIN application_functions af
        ON af.application_function_id = ue.application_function_id
      JOIN roles r
        ON r.opal_domain_id = bu.opal_domain_id
       AND r.version_number = 1
       AND r.is_active = true
       AND r.role_name = LEFT('TEST ONLY - ' || af.function_name, 100)
     WHERE bu.opal_domain_id IS NOT NULL
       AND (
            UPPER(u.token_preferred_username) LIKE 'OPAL-TEST%@HMCTS.NET'
            OR UPPER(u.token_preferred_username) LIKE 'OPAL-DEMO-%@HMCTS.NET'
       )
)
INSERT INTO business_unit_user_roles
(
 business_unit_user_role_id
,business_unit_user_id
,role_id
)
SELECT nextval('business_unit_user_role_id_seq')
     , t.business_unit_user_id
     , t.role_id
  FROM test_user_role_pairs t
 WHERE NOT EXISTS
       (
           SELECT 1
             FROM business_unit_user_roles bur
            WHERE bur.business_unit_user_id = t.business_unit_user_id
              AND bur.role_id = t.role_id
       );
