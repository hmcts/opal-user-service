/**
* OPAL Program
*
* MODULE      : assign_system_user_to_all_roles.sql
*
* DESCRIPTION : Assign the system user (opal-system-user) to all roles
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 01/07/2026    T McCallion    1.0         PO-7204 - User - Create new permissions for Interface Files
*                                          Assign the system user (opal-system-user) to all roles (allEnvs)
*
**/
WITH all_roles AS (
    SELECT DISTINCT role_id
      FROM roles  
)
--Assign roles to the system user (opal-system-user)
INSERT INTO business_unit_user_roles (business_unit_user_role_id, business_unit_user_id, role_id)
    SELECT NEXTVAL('business_unit_user_role_id_seq'), buu.business_unit_user_id, all_roles.role_id
      FROM users u
      JOIN business_unit_users buu 
        ON buu.user_id = u.user_id
     CROSS JOIN all_roles   
     WHERE u.user_id = -1 --opal-system-user
ON CONFLICT (business_unit_user_id, role_id)
DO NOTHING;
