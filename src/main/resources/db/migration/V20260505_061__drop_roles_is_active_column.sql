/**
* CGI OPAL Program
*
* MODULE      : drop_roles_is_active_column.sql
*
* DESCRIPTION : Drop IS_ACTIVE column from the ROLES table.
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    ----------------------------------------------------------------------------
* 01/05/2026    C Cho       1.0         PO-3769 Drop IS_ACTIVE column from ROLES.
*
**/

-- Drop the view that depends on the is_active column
DROP VIEW IF EXISTS v_current_roles;

-- Drop the is_active column
ALTER TABLE roles
DROP COLUMN is_active;

-- Recreate the view without the is_active column
CREATE OR REPLACE VIEW v_current_roles AS
SELECT 
    role_id,
    business_unit_id,
    role_name,
    role_description,
    access_permission
FROM roles;