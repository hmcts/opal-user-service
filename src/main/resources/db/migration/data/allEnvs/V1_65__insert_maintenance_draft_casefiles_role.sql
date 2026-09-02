/**
* CGI OPAL Program
*
* MODULE      : insert_maintenance_draft_casefiles_role.sql
*
* DESCRIPTION : Create the Maintenance Draft Casefiles role in every environment.
**/

DO $migration$
DECLARE
    v_maintenance_domain_id smallint;
    v_domain_count integer;
    v_role_id_count integer;
    v_exact_role_count integer;
    v_competing_identity_count integer;
    v_max_role_id bigint;
    v_sequence_last_value bigint;
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

    SELECT count(*)
      INTO v_competing_identity_count
      FROM public.roles r
     WHERE r.role_name = 'Maintenance Draft Casefiles'
       AND r.opal_domain_id = v_maintenance_domain_id
       AND r.version_number = 1
       AND r.role_id <> 55;

    IF v_competing_identity_count <> 0 THEN
        RAISE EXCEPTION
            'Maintenance Draft Casefiles version 1 already exists under a role ID other than 55';
    END IF;

    IF v_role_id_count = 0 THEN
        INSERT INTO public.roles (
            role_id,
            version_number,
            opal_domain_id,
            role_name,
            application_function_list
        )
        VALUES (
            55,
            1,
            v_maintenance_domain_id,
            'Maintenance Draft Casefiles',
            ARRAY['CREATE_MANAGE_DRAFT_CASEFILES']::public.t_permissions_enum[]
        );
    ELSIF v_role_id_count <> 1 OR v_exact_role_count <> 1 THEN
        RAISE EXCEPTION
            'Role ID 55 exists but does not exactly match Maintenance Draft Casefiles version 1';
    END IF;

    SELECT max(r.role_id)
      INTO v_max_role_id
      FROM public.roles r;

    SELECT s.last_value
      INTO v_sequence_last_value
      FROM public.role_id_seq s;

    PERFORM setval(
        'public.role_id_seq',
        greatest(v_max_role_id, v_sequence_last_value),
        true
    );

    SELECT count(*)
      INTO v_exact_role_count
      FROM public.roles r
     WHERE r.role_id = 55
       AND r.version_number = 1
       AND r.opal_domain_id = v_maintenance_domain_id
       AND r.role_name = 'Maintenance Draft Casefiles'
       AND r.application_function_list IS NOT DISTINCT FROM
           ARRAY['CREATE_MANAGE_DRAFT_CASEFILES']::public.t_permissions_enum[];

    IF v_exact_role_count <> 1 THEN
        RAISE EXCEPTION
            'Maintenance Draft Casefiles role postcondition failed';
    END IF;

    SELECT s.last_value
      INTO v_sequence_last_value
      FROM public.role_id_seq s;

    IF v_sequence_last_value < v_max_role_id THEN
        RAISE EXCEPTION
            'role_id_seq value % is below maximum role ID %',
            v_sequence_last_value,
            v_max_role_id;
    END IF;
END
$migration$;
