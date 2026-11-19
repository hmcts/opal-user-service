/**
* CGI OPAL Program
*
* MODULE      : assign_exclusive_suspense_roles_to_test_users.sql
*
* DESCRIPTION : Assign the View Suspense Items test role to opal-test-3
*               and the Manage Suspense Items test role to opal-test-4
*               for their existing Fines business unit memberships in NLE.
*
* VERSION HISTORY:
*
* Date          Author          Version    Nature of Change
* ----------    -----------     -------    --------------------------------------
* 13/11/2026    P Brumby        1.0        PO-11479 Assign separate View and Manage Suspense test permissions.
*
**/

-- ================================================================
-- ASSIGN SEPARATE SUSPENSE ROLES TO EXISTING SYSTEM TEST USERS
--
-- opal-test-3:
--   View Suspense Items only.
--   Existing seeded Fines membership: BU 26.
--
-- opal-test-4:
--   Manage Suspense Items only.
--   Existing seeded Fines membership: BU 47.
--   BU 111 belongs to Maintenance and is excluded.
--
-- Reuse the version 1 test roles created by V1_73.
-- Prior migrations through V1_74 assign neither suspense role
-- to either target account, so no role removal is required.
--
-- Apply to all existing Fines memberships for the two users.
-- Preserve unrelated role assignments and existing memberships.
-- Do not create users, memberships or role definitions.
-- ================================================================

WITH required_assignments (
    token_name,
    role_name
) AS (
    VALUES
        (
            'opal-test-3',
            'TEST ONLY - View Suspense Items'
        ),
        (
            'opal-test-4',
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
