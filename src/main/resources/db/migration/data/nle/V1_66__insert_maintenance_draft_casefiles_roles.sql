/**
* CGI OPAL Program
*
* MODULE      : insert_maintenance_draft_casefiles_roles.sql
*
* DESCRIPTION : Create the test-only creator and checker Draft Casefiles roles in NLE.
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 29/09/2026    Chris Larkin   1.0         PO-10300 / PO-10661 Create the test-only creator and checker Draft Casefiles roles in NLE.
*
**/

INSERT INTO public.roles (
    role_id,
    version_number,
    opal_domain_id,
    role_name,
    application_function_list
)
VALUES (
    nextval('public.role_id_seq'),
    1,
    (SELECT opal_domain_id FROM public.domain WHERE opal_domain_name = 'Maintenance'),
    'TEST ONLY - Create and Manage Draft Casefiles',
    ARRAY['CREATE_MANAGE_DRAFT_CASEFILES']::public.t_permissions_enum[]
), (
    nextval('public.role_id_seq'),
    1,
    (SELECT opal_domain_id FROM public.domain WHERE opal_domain_name = 'Maintenance'),
    'TEST ONLY - Check and validate draft Casefiles',
    ARRAY['CHECK_VALIDATE_DRAFT_CASEFILES']::public.t_permissions_enum[]
);
