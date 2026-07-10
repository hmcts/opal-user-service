/**
* OPAL Program
*
* MODULE      : link_system_user_to_all_business_units.sql
*
* DESCRIPTION : Link the system user to all business units
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 01/07/2026    T McCallion    1.0         PO-7204 - User - Create new permissions for Interface Files
*
**/

INSERT INTO business_unit_users (business_unit_user_id, business_unit_id, user_id)
    SELECT 'S' || TO_CHAR(bu.business_unit_id, 'FM000') || '01' AS business_unit_user_id
         , bu.business_unit_id 
         , -1 AS user_id  --opal-system-user
      FROM business_units bu
     WHERE business_unit_type = 'Accounting Division'
ON CONFLICT (business_unit_user_id)
DO NOTHING;