\set ON_ERROR_STOP on
BEGIN;
CREATE EXTENSION IF NOT EXISTS pgtap;
SELECT no_plan();

-- Fixture-local snapshots start after Flyway has applied the full migration chain.
CREATE FUNCTION pg_temp.snapshot(exclude_casefiles boolean DEFAULT false)
RETURNS jsonb LANGUAGE plpgsql AS $$
DECLARE
    table_name text;
    predicate text;
    digest text;
    result jsonb := '{}'::jsonb;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'users', 'business_units', 'domain', 'business_events', 'roles',
        'business_unit_users', 'business_unit_user_roles'
    ] LOOP
        predicate := '';
        IF exclude_casefiles AND table_name = 'roles' THEN
            predicate := $filter$WHERE NOT (role_name = 'TEST ONLY - Check and validate draft Casefiles'
                AND opal_domain_id IN (SELECT opal_domain_id FROM public.domain
                                      WHERE opal_domain_name = 'Maintenance'))$filter$;
        ELSIF exclude_casefiles AND table_name = 'business_unit_users' THEN
            predicate := 'WHERE NOT (business_unit_id = 44 AND user_id IN (500000009))';
        ELSIF exclude_casefiles AND table_name = 'business_unit_user_roles' THEN
            predicate := $filter$WHERE NOT (role_id IN (
                SELECT r.role_id FROM public.roles r JOIN public.domain d USING (opal_domain_id)
                 WHERE r.role_name = 'TEST ONLY - Check and validate draft Casefiles'
                   AND d.opal_domain_name = 'Maintenance'
            ) AND business_unit_user_id IN (
                SELECT business_unit_user_id FROM public.business_unit_users
                 WHERE business_unit_id = 44 AND user_id IN (500000009)
            ))$filter$;
        END IF;
        EXECUTE format($query$
            SELECT md5(coalesce(string_agg(row_to_json(t)::text, E'\n'
                ORDER BY row_to_json(t)::text), '')) FROM (SELECT * FROM public.%I %s)t
        $query$, table_name, predicate) INTO digest;
        result := result || jsonb_build_object(table_name, digest);
    END LOOP;
    RETURN result;
END $$;



-- These helpers and fixtures exist only inside this rollback-only test transaction.
CREATE FUNCTION pg_temp.casefiles_role_id() RETURNS bigint LANGUAGE sql AS $$
    SELECT r.role_id FROM public.roles r JOIN public.domain d USING (opal_domain_id)
     WHERE r.role_name = 'TEST ONLY - Check and validate draft Casefiles'
       AND d.opal_domain_name = 'Maintenance' AND r.version_number = 1;
$$;
CREATE FUNCTION pg_temp.role_sql() RETURNS text LANGUAGE sql AS $$
    SELECT pg_read_file('/tmp/opal-db-migrations/data/nle/V1_66__insert_maintenance_draft_casefiles_roles.sql');
$$;
CREATE FUNCTION pg_temp.assignment_sql() RETURNS text LANGUAGE sql AS $$
    SELECT pg_read_file('/tmp/opal-db-migrations/data/nle/V1_68__assign_maintenance_check_draft_casefiles_role.sql');
$$;
CREATE FUNCTION pg_temp.clear_target_memberships() RETURNS void LANGUAGE sql AS $$
    DELETE FROM public.business_unit_user_roles WHERE business_unit_user_id IN (
        SELECT business_unit_user_id FROM public.business_unit_users
         WHERE business_unit_id = 44 AND user_id IN (500000009)
    );
    DELETE FROM public.business_unit_users
     WHERE business_unit_id = 44 AND user_id IN (500000009);
$$;
CREATE FUNCTION pg_temp.granted_memberships() RETURNS text[] LANGUAGE sql AS $$
    SELECT array_agg(buu.business_unit_user_id::text ORDER BY buu.user_id)
      FROM public.business_unit_users buu
      JOIN public.business_unit_user_roles a USING (business_unit_user_id)
     WHERE buu.business_unit_id = 44 AND buu.user_id IN (500000009)
       AND a.role_id = pg_temp.casefiles_role_id();
$$;
CREATE TEMP TABLE expected_state (state jsonb);


-- -----------------------------------------------------------------------------
-- Scenario: The checker permission and role match the approved contract.
-- Setup: Read the schema and roles after the full Flyway migration chain.
-- Expected: One exact permission and role, version 1, with a usable role sequence.
-- -----------------------------------------------------------------------------
SELECT is((SELECT count(*) FROM pg_enum e JOIN pg_type t ON t.oid = e.enumtypid
            JOIN pg_namespace n ON n.oid = t.typnamespace
            WHERE n.nspname = 'public' AND t.typname = 't_permissions_enum'
              AND e.enumlabel = 'CHECK_VALIDATE_DRAFT_CASEFILES'), 1::bigint,
          'Exact Casefiles enum label exists once');
SELECT is((SELECT count(*) FROM public.roles r JOIN public.domain d USING (opal_domain_id)
            WHERE r.role_name = 'TEST ONLY - Check and validate draft Casefiles'
              AND d.opal_domain_name = 'Maintenance'), 1::bigint,
          'One test-only Maintenance role across all versions');
SELECT is((SELECT version_number FROM public.roles WHERE role_id = pg_temp.casefiles_role_id()),
          1::bigint, 'Role is version 1');
SELECT is((SELECT application_function_list::text[] FROM public.roles
            WHERE role_id = pg_temp.casefiles_role_id()),
          ARRAY['CHECK_VALIDATE_DRAFT_CASEFILES']::text[], 'Role has exactly the intended permission');
SELECT ok((SELECT s.last_value + CASE WHEN s.is_called THEN q.seqincrement ELSE 0 END
             FROM public.role_id_seq s JOIN pg_sequence q ON q.seqrelid = 'public.role_id_seq'::regclass)
          > (SELECT max(role_id) FROM public.roles), 'Next role ID remains above stored IDs');

-- -----------------------------------------------------------------------------
-- Scenario: Only the approved BU44 users receive checker access.
-- Setup: Read all assignments for the migrated checker role.
-- Expected: Only opal-test-10 at BU44 has the checker role.
-- -----------------------------------------------------------------------------
SELECT results_eq(
    $$SELECT u.user_id, u.token_name, bu.business_unit_id, bu.business_unit_code, d.opal_domain_name
        FROM public.business_unit_user_roles a
        JOIN public.business_unit_users buu USING (business_unit_user_id)
        JOIN public.users u USING (user_id)
        JOIN public.business_units bu USING (business_unit_id)
        JOIN public.domain d USING (opal_domain_id)
       WHERE a.role_id = pg_temp.casefiles_role_id() ORDER BY u.user_id$$,
    $$VALUES (500000009::bigint,'opal-test-10'::varchar,44::smallint,'0097'::varchar,'Maintenance'::varchar)$$,
    'Only opal-test-10 at BU44 receives the checker role; no other BU or system grant');
SELECT is(pg_temp.granted_memberships(), ARRAY['L044AO'], 'New memberships use the agreed IDs');


-- -----------------------------------------------------------------------------
-- Scenario: Checker access remains separate while both users retain creator access.
-- Setup: Read opal-test checker grants and both BU44 creator assignments.
-- Expected: opal-test has no checker grant in any BU; both users retain creator access.
-- -----------------------------------------------------------------------------
SELECT is((SELECT count(*) FROM public.business_unit_user_roles a
    JOIN public.business_unit_users buu USING (business_unit_user_id)
    WHERE buu.user_id = 500000000 AND a.role_id = pg_temp.casefiles_role_id()),
    0::bigint, 'opal-test has no checker grant in any Business Unit');
SELECT results_eq(
    $$SELECT buu.user_id FROM public.business_unit_user_roles a
        JOIN public.business_unit_users buu USING (business_unit_user_id)
        JOIN public.roles r USING (role_id)
        JOIN public.domain d USING (opal_domain_id)
        WHERE buu.business_unit_id = 44
          AND r.role_name = 'TEST ONLY - Create and Manage Draft Casefiles'
          AND d.opal_domain_name = 'Maintenance' ORDER BY buu.user_id$$,
    $$VALUES (500000000::bigint), (500000009::bigint)$$,
    'Both approved BU44 users retain creator access');


-- -----------------------------------------------------------------------------
-- Scenario: Duplicate role creation fails without changing stored rows.
-- Setup: Snapshot the seven tables and rerun the actual shared role INSERT.
-- Expected: A unique constraint rejects the duplicate and all table rows are unchanged.
-- -----------------------------------------------------------------------------
INSERT INTO expected_state SELECT pg_temp.snapshot();
SELECT throws_ok(pg_temp.role_sql(), '23505', NULL, 'Plain role creation rejects a duplicate');
SELECT is(pg_temp.snapshot(), (SELECT state FROM expected_state), 'Duplicate role failure leaves tables unchanged');

-- Capture observations inside rollback-only fixtures, then emit TAP outside the
-- subtransaction so pgTAP's own transactional counters are never rolled back.
CREATE FUNCTION pg_temp.assignment_scenario(scenario text) RETURNS SETOF text LANGUAGE plpgsql AS $$
DECLARE
    before_state jsonb;
    after_state jsonb;
    first_state jsonb;
    repeated_state jsonb;
    actual_memberships text[];
    expected_memberships text[] := ARRAY['L044AO'];
    assignment_count bigint;
    expected_role_id bigint;
    actual_role_id bigint;
BEGIN
    BEGIN
        PERFORM pg_temp.clear_target_memberships();
        IF scenario = 'existing' THEN
            INSERT INTO public.business_unit_users VALUES
                ('Y044AO',44,500000009);
            INSERT INTO public.business_unit_user_roles (business_unit_user_id, role_id)
            VALUES ('Y044AO',pg_temp.casefiles_role_id()), ('Y044AO',2);
            INSERT INTO public.business_unit_user_roles (business_unit_user_id, role_id)
            SELECT 'Y044AO', r.role_id FROM public.roles r
            JOIN public.domain d USING (opal_domain_id)
            WHERE r.role_name = 'TEST ONLY - Create and Manage Draft Casefiles'
              AND d.opal_domain_name = 'Maintenance' AND r.version_number = 1;
            expected_memberships := ARRAY['Y044AO'];
        ELSIF scenario = 'generated' THEN
            -- The shared migration creates both roles atomically. Remove both in
            -- this rollback-only fixture before exercising a new allocation.
            DELETE FROM public.business_unit_user_roles WHERE role_id IN (
                SELECT r.role_id FROM public.roles r JOIN public.domain d USING (opal_domain_id)
                WHERE d.opal_domain_name = 'Maintenance' AND r.role_name IN (
                    'TEST ONLY - Create and Manage Draft Casefiles',
                    'TEST ONLY - Check and validate draft Casefiles'));
            DELETE FROM public.roles r USING public.domain d
            WHERE r.opal_domain_id = d.opal_domain_id AND d.opal_domain_name = 'Maintenance'
              AND r.role_name IN ('TEST ONLY - Create and Manage Draft Casefiles',
                                  'TEST ONLY - Check and validate draft Casefiles');
            PERFORM setval('public.role_id_seq', greatest(
                (SELECT last_value FROM public.role_id_seq), (SELECT max(role_id) FROM public.roles)
            ) + 100, true);
            SELECT last_value + 2 * q.seqincrement INTO expected_role_id
              FROM public.role_id_seq JOIN pg_sequence q ON q.seqrelid = 'public.role_id_seq'::regclass;
            EXECUTE pg_temp.role_sql();
            actual_role_id := pg_temp.casefiles_role_id();
        ELSIF scenario <> 'fresh' THEN
            RAISE EXCEPTION 'Unknown test scenario %', scenario;
        END IF;
        before_state := pg_temp.snapshot(true);
        EXECUTE pg_temp.assignment_sql();
        actual_memberships := pg_temp.granted_memberships();
        SELECT count(*) INTO assignment_count FROM public.business_unit_user_roles
         WHERE role_id = pg_temp.casefiles_role_id();
        after_state := pg_temp.snapshot(true);
        first_state := pg_temp.snapshot();
        EXECUTE pg_temp.assignment_sql();
        repeated_state := pg_temp.snapshot();
        RAISE SQLSTATE 'P1066';
    EXCEPTION WHEN SQLSTATE 'P1066' THEN NULL;
    END;
    RETURN NEXT is(actual_memberships, expected_memberships, scenario || ': expected memberships are created/reused');
    RETURN NEXT is(assignment_count, 1::bigint, scenario || ': exactly one checker grant, no other access');
    RETURN NEXT is(after_state, before_state, scenario || ': unrelated rows and other permissions preserved');
    RETURN NEXT is(repeated_state, first_state, scenario || ': repeat assignment changes no rows');
    IF scenario = 'generated' THEN
        RETURN NEXT is(actual_role_id, expected_role_id, 'Different sequence-generated role ID is used');
    END IF;
END $$;

-- -----------------------------------------------------------------------------
-- Scenario: Missing BU44 memberships are created and assignments are repeatable.
-- Setup: Remove target memberships inside a rollback-only fixture and run actual SQL.
-- Expected: Agreed membership IDs and grants; unrelated rows and repeat runs stay unchanged.
-- -----------------------------------------------------------------------------
SELECT * FROM pg_temp.assignment_scenario('fresh');

-- -----------------------------------------------------------------------------
-- Scenario: Existing memberships and permissions are preserved.
-- Setup: Use alternative membership IDs with existing grants in a rollback-only fixture.
-- Expected: Membership IDs are reused, missing grants added and existing access preserved.
-- -----------------------------------------------------------------------------
SELECT * FROM pg_temp.assignment_scenario('existing');

-- -----------------------------------------------------------------------------
-- Scenario: Assignments resolve sequence-generated role IDs.
-- Setup: Recreate both roles after advancing the sequence inside a rollback-only fixture.
-- Expected: The checker uses its newly allocated ID and receives exactly the approved grants.
-- -----------------------------------------------------------------------------
SELECT * FROM pg_temp.assignment_scenario('generated');

CREATE FUNCTION pg_temp.failure_scenario(scenario text) RETURNS SETOF text LANGUAGE plpgsql AS $$
DECLARE
    before_state jsonb;
    after_state jsonb;
    actual_sqlstate text := '00000';
    expected_sqlstate text;
BEGIN
    BEGIN
        PERFORM pg_temp.clear_target_memberships();
        IF scenario = 'membership collision' THEN
            INSERT INTO public.business_unit_users VALUES ('L044AO',67,500000003);
            expected_sqlstate := '23505';
        ELSIF scenario = 'second INSERT failure' THEN
            ALTER TABLE public.business_unit_user_roles ADD CONSTRAINT po_10661_reject_second_target
                CHECK (business_unit_user_id <> 'L044AO');
            expected_sqlstate := '23514';
        ELSE
            RAISE EXCEPTION 'Unknown test scenario %', scenario;
        END IF;
        before_state := pg_temp.snapshot();
        BEGIN
            EXECUTE pg_temp.assignment_sql();
        EXCEPTION WHEN OTHERS THEN
            actual_sqlstate := SQLSTATE;
        END;
        after_state := pg_temp.snapshot();
        RAISE SQLSTATE 'P1066';
    EXCEPTION WHEN SQLSTATE 'P1066' THEN NULL;
    END;
    RETURN NEXT is(actual_sqlstate, expected_sqlstate, scenario || ': real SQL constraint rejects the change');
    RETURN NEXT is(after_state, before_state, scenario || ': both inserts roll back completely');
END $$;

-- -----------------------------------------------------------------------------
-- Scenario: A membership ID collision rejects the complete assignment operation.
-- Setup: Reserve the target membership ID for another user and execute actual SQL.
-- Expected: The primary-key failure leaves every table unchanged.
-- -----------------------------------------------------------------------------
SELECT * FROM pg_temp.failure_scenario('membership collision');

-- -----------------------------------------------------------------------------
-- Scenario: A role-assignment failure rolls back newly created memberships.
-- Setup: Inject a rejecting CHECK constraint on the assignment table and run actual SQL.
-- Expected: The second INSERT fails and both INSERT operations roll back completely.
-- -----------------------------------------------------------------------------
SELECT * FROM pg_temp.failure_scenario('second INSERT failure');

-- -----------------------------------------------------------------------------
-- Scenario: All controlled fixtures restore the original table contents.
-- Setup: Compare the seven tables with the snapshot taken before the scenarios.
-- Expected: Every stored row is restored; sequence gaps are allowed.
-- -----------------------------------------------------------------------------
SELECT is(pg_temp.snapshot(), (SELECT state FROM expected_state), 'All fixtures restore the original seven tables');
SELECT * FROM finish();
ROLLBACK;
