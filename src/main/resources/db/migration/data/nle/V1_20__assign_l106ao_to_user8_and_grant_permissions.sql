/**
* CGI OPAL Program
*
* MODULE      : assign_l106ao_to_user8_and_grant_permissions.sql
*
* DESCRIPTION : Create the L106AO business unit user mapping for opal-test-8 and assign all application functions.
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    -----------------------------------------------------------------------------------
* 13/05/2026    C Cho       1.0         PO-2989 Create L106AO for user 500000007 and grant all application functions.
*
**/

INSERT INTO business_unit_users
(
 business_unit_user_id
,business_unit_id
,user_id
)
SELECT 'L106AO'
     , 106
     , 500000007
WHERE NOT EXISTS
      (
          SELECT 1
            FROM business_unit_users
           WHERE business_unit_user_id = 'L106AO'
      );

WITH max_ue AS (
    SELECT COALESCE(MAX(user_entitlement_id), 0) AS max_ue_id
      FROM user_entitlements
),
missing_entitlements AS (
    SELECT 'L106AO' AS business_unit_user_id
         , af.application_function_id
      FROM application_functions af
     WHERE NOT EXISTS
           (
               SELECT 1
                 FROM user_entitlements ue
                WHERE ue.business_unit_user_id = 'L106AO'
                  AND ue.application_function_id = af.application_function_id
           )
)
INSERT INTO user_entitlements
(
 user_entitlement_id
,business_unit_user_id
,application_function_id
)
SELECT max_ue.max_ue_id + ROW_NUMBER() OVER (ORDER BY me.application_function_id) AS user_entitlement_id
     , me.business_unit_user_id
     , me.application_function_id
  FROM missing_entitlements me
 CROSS JOIN max_ue;
