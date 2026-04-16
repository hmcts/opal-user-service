/**
* CGI OPAL Program
*
* MODULE      : insert_r1a_roles.sql
*
* DESCRIPTION : Populate ROLES with the R1a role definitions for the Fines domain.
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    ----------------------------------------------------------------------------
* 08/04/2026    C Cho       1.0         PO-2829 Populate R1a roles and mapped permissions for the Fines domain.
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
    'Enf Authorised Functions',
    TRUE,
    ARRAY['Collection Order']
),
(
    nextval('role_id_seq'),
    1,
    1,
    'Enf Admin',
    TRUE,
    ARRAY['Create and Manage Draft Accounts']
),
(
    nextval('role_id_seq'),
    1,
    1,
    'Enf Admin Enhanced',
    TRUE,
    ARRAY['Check and Validate Draft Accounts']
);