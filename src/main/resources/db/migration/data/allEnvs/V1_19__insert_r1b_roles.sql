/**
* CGI OPAL Program
*
* MODULE      : insert_r1b_roles.sql
*
* DESCRIPTION : Populate ROLES with the R1B role definitions for the Fines domain.
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    ----------------------------------------------------------------------------
* 13/04/2026    C Cho       1.0         PO-2830 Populate R1B roles and mapped permissions for the Fines domain.
*
**/

WITH r1b_roles(role_name, opal_domain_id, application_function_list) AS (
    VALUES
    (
        'Enf Admin',
        1,
        ARRAY[
            'Account Maintenance',
            'Add Account Activity Notes',
            'Amend Payment Terms',
            'Enter Enforcement',
            'Search and view accounts'
        ]::varchar(200)[]
    ),
    (
        'Enf Admin Enhanced',
        1,
        ARRAY[
            'Add Account Activity Notes',
            'Amend Payment Terms',
            'Enter Enforcement',
            'Search and view accounts'
        ]::varchar(200)[]
    ),
    (
        'Cluster CT Access',
        1,
        ARRAY[
            'Add Account Activity Notes',
            'Search and view accounts'
        ]::varchar(200)[]
    ),
    (
        'Cash Admin',
        1,
        ARRAY[
            'Add Account Activity Notes',
            'Search and view accounts',
            'View Creditor BACS'
        ]::varchar(200)[]
    ),
    (
        'Cash Enhanced',
        1,
        ARRAY[
            'Add Account Activity Notes',
            'Search and view accounts',
            'View Creditor BACS'
        ]::varchar(200)[]
    ),
    (
        'Contact Centre Enforcement',
        1,
        ARRAY[
            'Account Maintenance',
            'Add Account Activity Notes',
            'Amend Payment Terms',
            'Enter Enforcement',
            'Search and view accounts',
            'View Creditor BACS'
        ]::varchar(200)[]
    )
),
current_roles AS (
    SELECT r.role_id,
           r.version_number,
           r.opal_domain_id,
           r.role_name,
           r.application_function_list
      FROM roles r
      JOIN (
          SELECT role_name,
                 opal_domain_id,
                 MAX(version_number) AS max_version_number
            FROM roles
           GROUP BY role_name, opal_domain_id
      ) latest_role
        ON latest_role.role_name = r.role_name
       AND latest_role.opal_domain_id = r.opal_domain_id
       AND latest_role.max_version_number = r.version_number
),
target_roles AS (
    SELECT
        COALESCE(current_roles.role_id, nextval('role_id_seq')) AS role_id,
        CASE
            WHEN current_roles.role_id IS NULL THEN 1
            WHEN COALESCE(
                (
                    SELECT array_agg(permission ORDER BY permission)
                    FROM (
                        SELECT DISTINCT permission
                        FROM unnest(current_roles.application_function_list) AS permission
                    ) ordered_permissions
                ),
                ARRAY[]::varchar(200)[]
            ) IS DISTINCT FROM COALESCE(
                (
                    SELECT array_agg(permission ORDER BY permission)
                    FROM (
                        SELECT DISTINCT permission
                        FROM unnest(
                            COALESCE(current_roles.application_function_list, ARRAY[]::varchar(200)[])
                            || r1b_roles.application_function_list
                        ) AS permission
                    ) ordered_permissions
                ),
                ARRAY[]::varchar(200)[]
            )
            THEN current_roles.version_number + 1
            ELSE current_roles.version_number
        END AS version_number,
        r1b_roles.opal_domain_id,
        r1b_roles.role_name,
        COALESCE(
            (
                SELECT array_agg(permission ORDER BY permission)
                FROM (
                    SELECT DISTINCT permission
                    FROM unnest(
                        CASE
                            WHEN current_roles.role_id IS NULL THEN r1b_roles.application_function_list
                            ELSE COALESCE(current_roles.application_function_list, ARRAY[]::varchar(200)[])
                                 || r1b_roles.application_function_list
                        END
                    ) AS permission
                ) ordered_permissions
            ),
            ARRAY[]::varchar(200)[]
        ) AS application_function_list,
        COALESCE(
            (
                SELECT array_agg(permission ORDER BY permission)
                FROM (
                    SELECT DISTINCT permission
                    FROM unnest(r1b_roles.application_function_list) AS permission
                    EXCEPT
                    SELECT DISTINCT permission
                    FROM unnest(COALESCE(current_roles.application_function_list, ARRAY[]::varchar(200)[])) AS permission
                ) added_permissions
            ),
            ARRAY[]::varchar(200)[]
        ) AS added_permissions,
        COALESCE(
            (
                SELECT array_agg(permission ORDER BY permission)
                FROM (
                    SELECT DISTINCT permission
                    FROM unnest(COALESCE(current_roles.application_function_list, ARRAY[]::varchar(200)[])) AS permission
                    EXCEPT
                    SELECT DISTINCT permission
                    FROM unnest(
                        CASE
                            WHEN current_roles.role_id IS NULL THEN r1b_roles.application_function_list
                            ELSE COALESCE(current_roles.application_function_list, ARRAY[]::varchar(200)[])
                                 || r1b_roles.application_function_list
                        END
                    ) AS permission
                ) removed_permissions
            ),
            ARRAY[]::varchar(200)[]
        ) AS removed_permissions,
        current_roles.role_id IS NULL
            OR COALESCE(
                (
                    SELECT array_agg(permission ORDER BY permission)
                    FROM (
                        SELECT DISTINCT permission
                        FROM unnest(current_roles.application_function_list) AS permission
                    ) ordered_permissions
                ),
                ARRAY[]::varchar(200)[]
            ) IS DISTINCT FROM COALESCE(
                (
                    SELECT array_agg(permission ORDER BY permission)
                    FROM (
                        SELECT DISTINCT permission
                        FROM unnest(
                            COALESCE(current_roles.application_function_list, ARRAY[]::varchar(200)[])
                            || r1b_roles.application_function_list
                        ) AS permission
                    ) ordered_permissions
                ),
                ARRAY[]::varchar(200)[]
            ) AS requires_insert,
        current_roles.role_id IS NOT NULL
            AND (
                COALESCE(
                    (
                        SELECT array_agg(permission ORDER BY permission)
                        FROM (
                            SELECT DISTINCT permission
                            FROM unnest(current_roles.application_function_list) AS permission
                        ) ordered_permissions
                    ),
                    ARRAY[]::varchar(200)[]
                ) IS DISTINCT FROM COALESCE(
                    (
                        SELECT array_agg(permission ORDER BY permission)
                        FROM (
                            SELECT DISTINCT permission
                            FROM unnest(
                                COALESCE(current_roles.application_function_list, ARRAY[]::varchar(200)[])
                                || r1b_roles.application_function_list
                            ) AS permission
                        ) ordered_permissions
                    ),
                    ARRAY[]::varchar(200)[]
                )
            ) AS permissions_changed
    FROM r1b_roles
    LEFT JOIN current_roles
      ON current_roles.role_name = r1b_roles.role_name
     AND current_roles.opal_domain_id = r1b_roles.opal_domain_id
),
inserted_roles AS (
    INSERT INTO roles
    (
        role_id,
        version_number,
        opal_domain_id,
        role_name,
        application_function_list
    )
    SELECT
        target_roles.role_id,
        target_roles.version_number,
        target_roles.opal_domain_id,
        target_roles.role_name,
        target_roles.application_function_list
    FROM target_roles
    WHERE target_roles.requires_insert
      AND NOT EXISTS (
          SELECT 1
          FROM roles existing_role
          WHERE existing_role.role_name = target_roles.role_name
            AND existing_role.opal_domain_id = target_roles.opal_domain_id
            AND existing_role.version_number = target_roles.version_number
      )
    RETURNING role_id, version_number, opal_domain_id, role_name
),
changed_roles AS (
    SELECT inserted_roles.role_id,
           inserted_roles.version_number,
           inserted_roles.opal_domain_id,
           inserted_roles.role_name,
           target_roles.added_permissions,
           target_roles.removed_permissions
      FROM inserted_roles
      JOIN target_roles
        ON target_roles.role_id = inserted_roles.role_id
       AND target_roles.version_number = inserted_roles.version_number
       AND target_roles.opal_domain_id = inserted_roles.opal_domain_id
       AND target_roles.role_name = inserted_roles.role_name
     WHERE target_roles.permissions_changed
),
affected_users AS (
    SELECT DISTINCT buu.user_id,
                    changed_roles.role_id
      FROM changed_roles
      JOIN business_unit_user_roles bur
        ON bur.role_id = changed_roles.role_id
      JOIN business_unit_users buu
        ON buu.business_unit_user_id = bur.business_unit_user_id
)
INSERT INTO business_events
(
    business_event_id,
    event_type,
    subject_user_id,
    initiator_user_id,
    event_details,
    event_date
)
SELECT
    nextval('business_event_id_seq'),
    'PERMISSIONS_ASSOCIATED_TO_ROLE_AMENDED'::t_event_type_enum,
    affected_users.user_id,
    -1, --Opal system user
    json_build_object(
        'role_id', changed_roles.role_id,
        'role_version', changed_roles.version_number,
        'added_permissions', to_json(changed_roles.added_permissions),
        'removed_permissions', to_json(changed_roles.removed_permissions)
    )::json,
    CURRENT_TIMESTAMP
FROM affected_users
JOIN changed_roles
  ON changed_roles.role_id = affected_users.role_id;
