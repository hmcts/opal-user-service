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
SELECT r.role_id,
       r.version_number,
       r.opal_domain_id,
       r.role_name,
       r.application_function_list
  FROM public.roles r
 INNER JOIN (
    SELECT role_id, max(version_number) AS max_version_number
      FROM public.roles
     GROUP BY role_id
 ) roles_agg
    ON r.role_id = roles_agg.role_id
   AND r.version_number = roles_agg.max_version_number;
