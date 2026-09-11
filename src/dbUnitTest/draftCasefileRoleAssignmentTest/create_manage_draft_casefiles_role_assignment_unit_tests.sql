\set ON_ERROR_STOP on

BEGIN;

CREATE TEMPORARY TABLE po_10300_expected_unchanged_state (
    table_name text PRIMARY KEY,
    row_digest text NOT NULL
) ON COMMIT DROP;

INSERT INTO po_10300_expected_unchanged_state (table_name, row_digest)
VALUES
    ('users', :'users_digest'),
    ('business_units', :'business_units_digest'),
    ('domain', :'domain_digest'),
    ('business_events', :'business_events_digest');

DO $assertions$
DECLARE
    v_maintenance_domain_id smallint;
    v_count integer;
    v_expected_digest text;
    v_actual_digest text;
    v_sequence_next_value bigint;
    v_max_role_id bigint;
BEGIN
    SELECT count(*)
      INTO v_count
      FROM pg_enum e
      JOIN pg_type t
        ON t.oid = e.enumtypid
      JOIN pg_namespace n
        ON n.oid = t.typnamespace
     WHERE n.nspname = 'public'
       AND t.typname = 't_permissions_enum'
       AND e.enumlabel = 'CREATE_MANAGE_DRAFT_CASEFILES';

    IF v_count <> 1 THEN
        RAISE EXCEPTION
            'Expected one CREATE_MANAGE_DRAFT_CASEFILES enum label, found %',
            v_count;
    END IF;

    SELECT count(*), min(d.opal_domain_id)
      INTO v_count, v_maintenance_domain_id
      FROM public.domain d
     WHERE d.opal_domain_name = 'Maintenance';

    IF v_count <> 1 THEN
        RAISE EXCEPTION
            'Expected exactly one Maintenance domain, found %',
            v_count;
    END IF;

    SELECT count(*)
      INTO v_count
      FROM public.roles r
     WHERE r.role_id = 55
       AND r.version_number = 1
       AND r.opal_domain_id = v_maintenance_domain_id
       AND r.role_name = 'Maintenance Draft Casefiles'
       AND r.application_function_list IS NOT DISTINCT FROM
           ARRAY['CREATE_MANAGE_DRAFT_CASEFILES']::public.t_permissions_enum[];

    IF v_count <> 1 THEN
        RAISE EXCEPTION
            'Role 55 does not have the exact approved identity and sole permission';
    END IF;

    SELECT count(*)
      INTO v_count
      FROM public.roles r
     WHERE r.role_name = 'Maintenance Draft Casefiles'
       AND r.opal_domain_id = v_maintenance_domain_id;

    IF v_count <> 1 THEN
        RAISE EXCEPTION
            'Expected one Maintenance Draft Casefiles identity across all versions, found %',
            v_count;
    END IF;

    SELECT s.last_value
           + CASE WHEN s.is_called THEN seq.seqincrement ELSE 0 END,
           (SELECT max(r.role_id) FROM public.roles r)
      INTO v_sequence_next_value, v_max_role_id
      FROM public.role_id_seq s
      JOIN pg_sequence seq
        ON seq.seqrelid = 'public.role_id_seq'::regclass;

    IF v_sequence_next_value <= COALESCE(v_max_role_id, 0) THEN
        RAISE EXCEPTION
            'role_id_seq next allocation % is not above maximum stored role ID %',
            v_sequence_next_value,
            v_max_role_id;
    END IF;

    SELECT count(*)
      INTO v_count
      FROM public.users u
     WHERE u.user_id = 500000000
       AND u.token_name = 'opal-test';

    IF v_count <> 1
       OR (SELECT count(*) FROM public.users WHERE token_name = 'opal-test') <> 1 THEN
        RAISE EXCEPTION
            'Expected exactly one opal-test user with user ID 500000000';
    END IF;

    SELECT count(*)
      INTO v_count
      FROM public.business_units bu
     WHERE bu.business_unit_id = 67
       AND bu.business_unit_name =
           'CAO - Inner London Maintenance (Wells Street)'
       AND bu.opal_domain_id = v_maintenance_domain_id;

    IF v_count <> 1 THEN
        RAISE EXCEPTION
            'Business Unit 67 does not match the approved Maintenance target';
    END IF;

    SELECT count(*)
      INTO v_count
      FROM public.business_unit_users buu
     WHERE buu.business_unit_user_id = 'L067JG'
       AND buu.business_unit_id = 67
       AND buu.user_id = 500000000;

    IF v_count <> 1
       OR (SELECT count(*)
             FROM public.business_unit_users
            WHERE business_unit_id = 67
              AND user_id = 500000000) <> 1 THEN
        RAISE EXCEPTION
            'L067JG is not the unique approved user and Business Unit relationship';
    END IF;

    SELECT count(*)
      INTO v_count
      FROM public.business_unit_user_roles buur
     WHERE buur.business_unit_user_id = 'L067JG'
       AND buur.role_id = 55;

    IF v_count <> 1 THEN
        RAISE EXCEPTION
            'Expected exactly one L067JG assignment to role 55, found %',
            v_count;
    END IF;

    IF (SELECT r.opal_domain_id FROM public.roles r WHERE r.role_id = 55)
       <> (SELECT bu.opal_domain_id FROM public.business_units bu WHERE bu.business_unit_id = 67)
       OR (SELECT r.opal_domain_id FROM public.roles r WHERE r.role_id = 55)
          <> v_maintenance_domain_id THEN
        RAISE EXCEPTION
            'Role 55 and Business Unit 67 are not aligned to the Maintenance domain';
    END IF;

    SELECT row_digest
      INTO v_expected_digest
      FROM po_10300_expected_unchanged_state
     WHERE table_name = 'users';
    SELECT md5(coalesce(string_agg(row_to_json(u)::text, E'\n' ORDER BY u.user_id), ''))
      INTO v_actual_digest
      FROM public.users u;
    IF v_actual_digest IS DISTINCT FROM v_expected_digest THEN
        RAISE EXCEPTION 'USERS changed during the migration under test';
    END IF;

    SELECT row_digest
      INTO v_expected_digest
      FROM po_10300_expected_unchanged_state
     WHERE table_name = 'business_units';
    SELECT md5(coalesce(string_agg(row_to_json(bu)::text, E'\n' ORDER BY bu.business_unit_id), ''))
      INTO v_actual_digest
      FROM public.business_units bu;
    IF v_actual_digest IS DISTINCT FROM v_expected_digest THEN
        RAISE EXCEPTION 'BUSINESS_UNITS changed during the migration under test';
    END IF;

    SELECT row_digest
      INTO v_expected_digest
      FROM po_10300_expected_unchanged_state
     WHERE table_name = 'domain';
    SELECT md5(coalesce(string_agg(row_to_json(d)::text, E'\n' ORDER BY d.opal_domain_id), ''))
      INTO v_actual_digest
      FROM public.domain d;
    IF v_actual_digest IS DISTINCT FROM v_expected_digest THEN
        RAISE EXCEPTION 'DOMAIN changed during the migration under test';
    END IF;

    SELECT row_digest
      INTO v_expected_digest
      FROM po_10300_expected_unchanged_state
     WHERE table_name = 'business_events';
    SELECT md5(coalesce(string_agg(row_to_json(be)::text, E'\n' ORDER BY be.business_event_id), ''))
      INTO v_actual_digest
      FROM public.business_events be;
    IF v_actual_digest IS DISTINCT FROM v_expected_digest THEN
        RAISE EXCEPTION 'BUSINESS_EVENTS changed during the migration under test';
    END IF;
END
$assertions$;

COMMIT;
