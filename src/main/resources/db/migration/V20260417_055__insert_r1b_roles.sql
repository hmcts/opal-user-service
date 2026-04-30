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

INSERT INTO roles
(
    role_id,
    version_number,
    opal_domain_id,
    role_name,
    is_active,
    application_function_list
)
VALUES
(
    nextval('role_id_seq'),
    1,
    1,
    'Enf Admin',
    TRUE,
    ARRAY[
        'Account Maintenance',
        'Add Account Activity Notes',
        'Amend Payment Terms',
        'Enter Enforcement',
        'Search and view accounts'
    ]
),
(
    nextval('role_id_seq'),
    1,
    1,
    'Enf Admin Enhanced',
    TRUE,
    ARRAY[
        'Add Account Activity Notes',
        'Amend Payment Terms',
        'Enter Enforcement',
        'Search and view accounts'
    ]
),
(
    nextval('role_id_seq'),
    1,
    1,
    'Cluster CT Access',
    TRUE,
    ARRAY[
        'Add Account Activity Notes',
        'Search and view accounts'
    ]
),
(
    nextval('role_id_seq'),
    1,
    1,
    'Cash Admin',
    TRUE,
    ARRAY[
        'Add Account Activity Notes',
        'Search and view accounts',
        'View Creditor BACS'
    ]
),
(
    nextval('role_id_seq'),
    1,
    1,
    'Cash Enhanced',
    TRUE,
    ARRAY[
        'Add Account Activity Notes',
        'Search and view accounts',
        'View Creditor BACS'
    ]
),
(
    nextval('role_id_seq'),
    1,
    1,
    'Contact Centre Enforcement',
    TRUE,
    ARRAY[
        'Account Maintenance',
        'Add Account Activity Notes',
        'Amend Payment Terms',
        'Enter Enforcement',
        'Search and view accounts',
        'View Creditor BACS'
    ]
);