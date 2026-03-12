/**
* CGI OPAL Program
*
* MODULE      : populate_test_roles_from_user_entitlements.sql
*
* DESCRIPTION : Populates ROLES records for test-only user permissions per Opal domain.
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    ----------------------------------------------------------------------------
* 10/03/2026    C Cho       1.0         PO-2827 Populate test-only roles from obsoleted USER_ENTITLEMENTS/APPLICATION_FUNCTIONS data.
*
**/

WITH test_permissions_by_domain AS (
    SELECT DISTINCT bu.opal_domain_id,
                    af.function_name
      FROM users u
      JOIN business_unit_users buu
        ON buu.user_id = u.user_id
      JOIN business_units bu
        ON bu.business_unit_id = buu.business_unit_id
      JOIN user_entitlements ue
        ON ue.business_unit_user_id = buu.business_unit_user_id
      JOIN application_functions af
        ON af.application_function_id = ue.application_function_id
     WHERE bu.opal_domain_id IS NOT NULL
       AND (
            UPPER(u.token_preferred_username) LIKE 'OPAL-TEST%@HMCTS.NET'
            OR UPPER(u.token_preferred_username) LIKE 'OPAL-DEMO-%@HMCTS.NET'
       )
)
INSERT INTO roles
(
 role_id
,version_number
,opal_domain_id
,role_name
,is_active
,application_function_list
)
SELECT nextval('role_id_seq')
     , 1 AS version_number
     , t.opal_domain_id
     , LEFT('TEST ONLY - ' || t.function_name, 100) AS role_name
     , true AS is_active
     , ARRAY[t.function_name]::varchar(200)[] AS application_function_list
  FROM test_permissions_by_domain t
 WHERE NOT EXISTS
       (
           SELECT 1
             FROM roles r
            WHERE r.opal_domain_id = t.opal_domain_id
              AND r.version_number = 1
              AND r.role_name = LEFT('TEST ONLY - ' || t.function_name, 100)
       );
