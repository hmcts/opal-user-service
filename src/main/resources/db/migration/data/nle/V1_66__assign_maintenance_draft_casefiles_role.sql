/**
* CGI OPAL Program
*
* MODULE      : assign_maintenance_draft_casefiles_role.sql
*
* DESCRIPTION : Assign the Maintenance Draft Casefiles role to the approved NLE membership.
**/

DO $migration$
DECLARE
    v_maintenance_domain_id smallint;
    v_domain_count integer;
    v_user_id_count integer;
    v_user_identity_count integer;
    v_user_name_count integer;
    v_business_unit_count integer;
    v_role_id_count integer;
    v_exact_role_count integer;
    v_membership_relationship_count integer;
    v_membership_id_count integer;
    v_existing_membership_id varchar(6);
    v_assignment_count integer;
BEGIN
    SELECT count(*), min(d.opal_domain_id)
      INTO v_domain_count, v_maintenance_domain_id
      FROM public.domain d
     WHERE d.opal_domain_name = 'Maintenance';

    IF v_domain_count <> 1 THEN
        RAISE EXCEPTION
            'Expected exactly one Maintenance domain, found %',
            v_domain_count;
    END IF;

    SELECT count(*)
      INTO v_user_id_count
      FROM public.users u
     WHERE u.user_id = 500000000;

    SELECT count(*)
      INTO v_user_identity_count
      FROM public.users u
     WHERE u.user_id = 500000000
       AND u.token_name = 'opal-test';

    SELECT count(*)
      INTO v_user_name_count
      FROM public.users u
     WHERE u.token_name = 'opal-test';

    IF v_user_id_count <> 1
       OR v_user_identity_count <> 1
       OR v_user_name_count <> 1 THEN
        RAISE EXCEPTION
            'Expected exactly one opal-test user with user ID 500000000';
    END IF;

    SELECT count(*)
      INTO v_business_unit_count
      FROM public.business_units bu
     WHERE bu.business_unit_id = 67
       AND bu.business_unit_name =
           'CAO - Inner London Maintenance (Wells Street)'
       AND bu.opal_domain_id = v_maintenance_domain_id;

    IF v_business_unit_count <> 1 THEN
        RAISE EXCEPTION
            'Business Unit 67 does not exactly match the approved Maintenance target';
    END IF;

    SELECT count(*)
      INTO v_role_id_count
      FROM public.roles r
     WHERE r.role_id = 55;

    SELECT count(*)
      INTO v_exact_role_count
      FROM public.roles r
     WHERE r.role_id = 55
       AND r.version_number = 1
       AND r.opal_domain_id = v_maintenance_domain_id
       AND r.role_name = 'Maintenance Draft Casefiles'
       AND r.application_function_list IS NOT DISTINCT FROM
           ARRAY['CREATE_MANAGE_DRAFT_CASEFILES']::public.t_permissions_enum[];

    IF v_role_id_count <> 1 OR v_exact_role_count <> 1 THEN
        RAISE EXCEPTION
            'Role 55 does not exactly match the approved Maintenance Draft Casefiles role';
    END IF;

    SELECT count(*), min(buu.business_unit_user_id)
      INTO v_membership_relationship_count, v_existing_membership_id
      FROM public.business_unit_users buu
     WHERE buu.user_id = 500000000
       AND buu.business_unit_id = 67;

    IF v_membership_relationship_count > 1 THEN
        RAISE EXCEPTION
            'Multiple memberships exist for user 500000000 and Business Unit 67';
    END IF;

    IF v_membership_relationship_count = 1
       AND v_existing_membership_id <> 'L067JG' THEN
        RAISE EXCEPTION
            'The approved relationship exists under membership ID % instead of L067JG',
            v_existing_membership_id;
    END IF;

    SELECT count(*)
      INTO v_membership_id_count
      FROM public.business_unit_users buu
     WHERE buu.business_unit_user_id = 'L067JG';

    IF v_membership_relationship_count = 0 THEN
        IF v_membership_id_count <> 0 THEN
            RAISE EXCEPTION
                'Membership ID L067JG belongs to a different user or Business Unit';
        END IF;

        INSERT INTO public.business_unit_users (
            business_unit_user_id,
            business_unit_id,
            user_id
        )
        VALUES ('L067JG', 67, 500000000);
    ELSIF v_membership_id_count <> 1 THEN
        RAISE EXCEPTION
            'Expected exactly one L067JG membership, found %',
            v_membership_id_count;
    END IF;

    SELECT count(*)
      INTO v_assignment_count
      FROM public.business_unit_user_roles buur
     WHERE buur.business_unit_user_id = 'L067JG'
       AND buur.role_id = 55;

    IF v_assignment_count = 0 THEN
        INSERT INTO public.business_unit_user_roles (
            business_unit_user_role_id,
            business_unit_user_id,
            role_id
        )
        VALUES (
            nextval('public.business_unit_user_role_id_seq'),
            'L067JG',
            55
        );
    ELSIF v_assignment_count <> 1 THEN
        RAISE EXCEPTION
            'Expected at most one L067JG assignment to role 55, found %',
            v_assignment_count;
    END IF;

    SELECT count(*)
      INTO v_membership_relationship_count
      FROM public.business_unit_users buu
     WHERE buu.business_unit_user_id = 'L067JG'
       AND buu.user_id = 500000000
       AND buu.business_unit_id = 67;

    SELECT count(*)
      INTO v_assignment_count
      FROM public.business_unit_user_roles buur
     WHERE buur.business_unit_user_id = 'L067JG'
       AND buur.role_id = 55;

    IF v_membership_relationship_count <> 1 OR v_assignment_count <> 1 THEN
        RAISE EXCEPTION
            'NLE membership or role assignment postcondition failed';
    END IF;
END
$migration$;
