-- Insert users from Flyway script V20240729_003
INSERT INTO users (user_id, token_preferred_username, token_subject, token_name, password, description, created_date, version_number)
VALUES (-1, 'opal-system-user', 'opal-system-user', 'opal-system-user', NULL, 'System user for business event initiator auditing', CURRENT_TIMESTAMP, 0);

INSERT INTO users (user_id, token_preferred_username, token_subject, token_name, password, description, created_date, version_number)
VALUES (500000000, 'opal-test@HMCTS.NET', 'k9LpT2xVqR8m','Pablo', 'password', 'User with 7 business units', CURRENT_TIMESTAMP, 0);

INSERT INTO users (user_id, token_preferred_username, token_subject, description, token_name, version_number,
                   created_date, activation_date)
VALUES (500000001, 'opal-test-2@HMCTS.NET', 'GfsHbIMt49WjQ', 'User with no business units', NULL, 0,
        CURRENT_TIMESTAMP, NULL),
       (500000002, 'update-user@HMCTS.NET', 'BmMfmuTT9pEdG', 'User for testing \`update\`', NULL, 0,
        CURRENT_TIMESTAMP, to_date('2026-04-13', 'YYYY-MM-DD')),
       (500000003, 'test-user@HMCTS.NET', 'jjqwGAERGW43', 'Test User for testing', 'Pablo', 2,
        CURRENT_TIMESTAMP, NULL),
       (500000004, 'test-user@HMCTS.NET', '7324-fh42dEsr', 'Test User for testing', 'Pablo', 2,
        CURRENT_TIMESTAMP, NULL),
       (500000005, 'update-user@HMCTS.NET', 'QeJjwoWnY-kBmMfm', 'Test User for testing \`update\`', 'Pablo', 7,
        CURRENT_TIMESTAMP, NULL),
       (500000006, 'no-go-user@HMCTS.NET', '8hqucbw874fg3', 'User with business units but no entitlements', 'No Permissions', 3,
        CURRENT_TIMESTAMP, NULL);


-- Insert business units that are referenced in the business_unit_users script
INSERT INTO business_units (business_unit_id, business_unit_name, business_unit_code, business_unit_type,
                            opal_domain_id)
VALUES (61, 'Test BU 61', 'T61', 'Accounting Division', 1),
       (67, 'Test BU 67', 'T67', 'Accounting Division', 1),
       (68, 'Test BU 68', 'T68', 'Accounting Division', 1),
       (69, 'Test BU 69', 'T69', 'Accounting Division', 1),
       (70, 'Test BU 70', 'T70', 'Accounting Division', 1),
       (71, 'Test BU 71', 'T71', 'Accounting Division', 1),
       (73, 'Test BU 73', 'T73', 'Accounting Division', 1);


-- Link User 500000000 to Business Units from Flyway script V20240730_005
INSERT INTO business_unit_users (business_unit_user_id, business_unit_id, user_id)
VALUES ('L065JG', 70, 500000000),
       ('L066JG', 68, 500000000),
       ('L067JG', 73, 500000000),
       ('L073JG', 71, 500000000),
       ('L077JG', 67, 500000000),
       ('L078JG', 69, 500000000),
       ('L080JG', 61, 500000000),
       ('L081JG', 67, 500000006),
       ('L082JG', 69, 500000006);

-- Grant permissions to User 500000000 from Flyway script V20240730_007
-- Granting a subset for a focused test

INSERT INTO roles (role_id, version_number, opal_domain_id, role_name, application_function_list)
VALUES (1,1, 1, 'Fines_Role_1',  ARRAY['Create and Manage Draft Accounts', 'Account Enquiry - Account Notes','Account Maintenance']),
       (1,2, 1, 'Fines_Role_1',  ARRAY['Create and Manage Draft Accounts', 'Account Enquiry','Account Maintenance']),
       (2,1, 1, 'Fines_Role_2',  ARRAY['Collection Order','Account Maintenance']),
       (2,2, 1, 'Fines_Role_2',  ARRAY['Check and Validate Draft Accounts', 'Search and view accounts']),
       (2,3, 1, 'Fines_Role_2',  ARRAY['Collection Order', 'Check and Validate Draft Accounts', 'Search and view accounts']),
       (3,1, 2, 'Confiscation_Role_3',  ARRAY['Create and Manage Draft Accounts']),
       (3,2, 2, 'Confiscation_Role_3',  ARRAY['Create and Manage Draft Accounts', 'Collection Order']);

INSERT INTO business_unit_user_roles(business_unit_user_role_id, business_unit_user_id, role_id)
VALUES (1,'L065JG', 1),
       (2,'L065JG', 2);

SELECT setval(
    'business_unit_user_role_id_seq',
    COALESCE((SELECT MAX(business_unit_user_role_id) FROM business_unit_user_roles), 1),
    true
);
