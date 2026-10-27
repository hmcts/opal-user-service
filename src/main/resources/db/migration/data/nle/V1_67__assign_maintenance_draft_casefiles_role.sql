/**
* CGI OPAL Program
*
* MODULE      : assign_maintenance_draft_casefiles_role.sql
*
* DESCRIPTION : Assign the test-only Create and Manage Draft Casefiles role to
*               opal-test and opal-test-10 in Business Unit 44 only.
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 28/09/2026    Chris Larkin   1.0         PO-10300 Assign the test-only Draft Casefiles role to both approved BU44 users.
*
**/

-- Create only missing BU44 memberships.
INSERT INTO public.business_unit_users (
    business_unit_user_id, business_unit_id, user_id
)
SELECT t.membership_id, 44, t.user_id
FROM (VALUES ('L044JG', 500000000::bigint),
             ('L044AO', 500000009::bigint)) AS t(membership_id, user_id)
WHERE NOT EXISTS (
    SELECT 1 FROM public.business_unit_users buu
    WHERE buu.user_id = t.user_id
      AND buu.business_unit_id = 44
);

-- Assign the role to the two approved users at BU44.
INSERT INTO public.business_unit_user_roles (
    business_unit_user_role_id, business_unit_user_id, role_id
)
SELECT nextval('public.business_unit_user_role_id_seq'),
       buu.business_unit_user_id,
       r.role_id
FROM public.business_unit_users buu
JOIN public.users u ON u.user_id = buu.user_id
JOIN (VALUES (500000000::bigint, 'opal-test'),
             (500000009::bigint, 'opal-test-10')) AS t(user_id, token_name)
  ON t.user_id = u.user_id AND t.token_name = u.token_name
JOIN public.business_units bu
  ON bu.business_unit_id = buu.business_unit_id
JOIN public.domain d ON d.opal_domain_id = bu.opal_domain_id
JOIN public.roles r ON r.opal_domain_id = d.opal_domain_id
WHERE bu.business_unit_id = 44
  AND d.opal_domain_name = 'Maintenance'
  AND r.role_name = 'TEST ONLY - Create and Manage Draft Casefiles'
  AND r.version_number = 1
ON CONFLICT (business_unit_user_id, role_id) DO NOTHING;
