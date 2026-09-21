/**
* OPAL Program
*
* MODULE      : recreate_v_current_roles.sql
*
* DESCRIPTION : Recreate current roles view
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 21/08/2026    C Cho          1.0         PO-5763 - Recreate v_current_roles after role permission enum conversion
*
**/

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