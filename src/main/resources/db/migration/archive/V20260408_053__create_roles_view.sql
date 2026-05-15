/**
* CGI OPAL Program
*
* MODULE      : create_roles_view.sql
*
* DESCRIPTION : Create a view of roles which shows only the latest versions
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 31/03/2026    S Reed         1.0         PO-2816 - Update user state to use Role based access control (RBAC), domains
                                                      and cache (Opal Mode)
*
**/

CREATE OR REPLACE VIEW v_current_roles AS
SELECT R.*
FROM public.roles R
INNER JOIN (
  SELECT role_id, max(version_number) AS max_version_number
  FROM public.roles
  GROUP BY role_id
) roles_agg ON R.role_id=roles_agg.role_id AND R.version_number=roles_agg.max_version_number;
