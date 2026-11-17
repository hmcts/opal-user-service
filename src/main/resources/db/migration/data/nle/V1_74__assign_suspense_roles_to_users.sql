/**
* CGI OPAL Program
*
* MODULE      : assign_suspense_roles_to_users.sql
*
* DESCRIPTION : Assign Suspense permissions to the OPAL System
*               user and approved System Test users in NLE.
*
* VERSION HISTORY:
*
* Date          Author          Version    Nature of Change
* ----------    -----------     -------    --------------------------------------
* 09/10/2026    Imran Chandio   1.0        PO-10192  Assign Suspense test permissions.
*
**/

-- ================================================================
-- 1. ASSIGN BOTH SUSPENSE ROLES TO OPAL SYSTEM USER
--    FOR ALL EXISTING FINES BUSINESS UNIT MEMBERSHIPS
-- ================================================================

INSERT INTO public.business_unit_user_roles (
    business_unit_user_role_id,
    business_unit_user_id,
    role_id
)
SELECT
    nextval('public.business_unit_user_role_id_seq'),
    buu.business_unit_user_id,
    r.role_id
FROM public.users u
JOIN public.business_unit_users buu
    ON buu.user_id = u.user_id
JOIN public.business_units bu
    ON bu.business_unit_id = buu.business_unit_id
JOIN public.domain d
    ON d.opal_domain_id = bu.opal_domain_id
JOIN public.roles r
    ON r.opal_domain_id = d.opal_domain_id
WHERE u.token_name = 'opal-system-user'
  AND u.is_system_user = TRUE
  AND d.opal_domain_name = 'Fines'
  AND r.role_name IN (
      'TEST ONLY - View Suspense Items',
      'TEST ONLY - Manage Suspense Items'
  )
  AND r.version_number = 1
ON CONFLICT (business_unit_user_id, role_id)
DO NOTHING;


-- ================================================================
-- 2. ASSIGN SUSPENSE ROLES TO APPROVED SYSTEM TEST USERS
--
-- opal-test:
--   BU 73: View + Manage
--   BU 77: View + Manage
--
-- opal-test-10:
--   BU 73: View
--   BU 77: View + Manage
--
-- opal-test-2:
--   No changes
-- ================================================================

WITH required_assignments (
    token_name,
    business_unit_id,
    role_name
) AS (
    VALUES
        (
            'opal-test',
            73,
            'TEST ONLY - View Suspense Items'
        ),
        (
            'opal-test',
            73,
            'TEST ONLY - Manage Suspense Items'
        ),
        (
            'opal-test',
            77,
            'TEST ONLY - View Suspense Items'
        ),
        (
            'opal-test',
            77,
            'TEST ONLY - Manage Suspense Items'
        ),
        (
            'opal-test-10',
            73,
            'TEST ONLY - View Suspense Items'
        ),
        (
            'opal-test-10',
            77,
            'TEST ONLY - View Suspense Items'
        ),
        (
            'opal-test-10',
            77,
            'TEST ONLY - Manage Suspense Items'
        )
)
INSERT INTO public.business_unit_user_roles (
    business_unit_user_role_id,
    business_unit_user_id,
    role_id
)
SELECT
    nextval('public.business_unit_user_role_id_seq'),
    buu.business_unit_user_id,
    r.role_id
FROM required_assignments ra
JOIN public.users u
    ON u.token_name = ra.token_name
JOIN public.business_unit_users buu
    ON buu.user_id = u.user_id
   AND buu.business_unit_id = ra.business_unit_id
JOIN public.business_units bu
    ON bu.business_unit_id = buu.business_unit_id
JOIN public.domain d
    ON d.opal_domain_id = bu.opal_domain_id
   AND d.opal_domain_name = 'Fines'
JOIN public.roles r
    ON r.opal_domain_id = d.opal_domain_id
   AND r.role_name = ra.role_name
   AND r.version_number = 1
ON CONFLICT (business_unit_user_id, role_id)
DO NOTHING;