/**
* CGI OPAL Program
*
* MODULE      : insert_suspense_test_roles.sql
*
* DESCRIPTION : Create TEST ONLY View and Manage Suspense Items
*               roles for the Fines domain.
*
* VERSION HISTORY:
*
* Date          Author          Version    Nature of Change
* ----------    -----------     -------    --------------------------------------
* 09/10/2026    Imran Chandio   1.0        PO-10192 Create Suspense test-only roles.
*
**/

INSERT INTO public.roles (
    role_id,
    version_number,
    opal_domain_id,
    role_name,
    application_function_list
)
SELECT
    nextval('public.role_id_seq'),
    1,
    d.opal_domain_id,
    p.role_name,
    ARRAY[p.permission_name::public.t_permissions_enum]
FROM public.domain d
CROSS JOIN (
    VALUES
        (
            'TEST ONLY - View Suspense Items',
            'VIEW_SUSPENSE_ITEMS'
        ),
        (
            'TEST ONLY - Manage Suspense Items',
            'MANAGE_SUSPENSE_ITEMS'
        )
) AS p(role_name, permission_name)
WHERE d.opal_domain_name = 'Fines'
  AND NOT EXISTS (
      SELECT 1
      FROM public.roles r
      WHERE r.opal_domain_id = d.opal_domain_id
        AND r.role_name = p.role_name
  );