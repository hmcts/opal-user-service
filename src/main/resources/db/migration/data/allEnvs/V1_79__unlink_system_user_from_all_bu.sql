/**
* OPAL Program
*
* MODULE      : unlink_system_user_from_all_bu.sql
*
* DESCRIPTION : Remove the System user (opal-system-user) from all Roles (business_unit_user_roles)
*               Unlink the System user (opal-system-user) from all Business Units (business_unit_users)
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    -------------------------------------------------------------
* 20/10/2026    T McCallion    1.0         PO-10698 - Create test only role for System User and Admin BU
*
**/

--Remove the opal-system-user from all Roles
DELETE FROM business_unit_user_roles
 WHERE business_unit_user_id IN (
    SELECT buu.business_unit_user_id
      FROM business_unit_users buu
     WHERE buu.user_id = -1
);

--Unlink the opal-system-user from all Business Units
DELETE FROM business_unit_users
 WHERE user_id = -1;
