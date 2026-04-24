-- Insert users from Flyway script V20240729_003
INSERT INTO users (user_id, token_preferred_username, token_subject, token_name, password, description, created_date, version_number)
VALUES (500000000, 'opal-test@HMCTS.NET', 'k9LpT2xVqR8m','Pablo', 'password', 'User with 7 business units', CURRENT_TIMESTAMP, 0);

INSERT INTO users (user_id, token_preferred_username, token_subject, description, token_name, version_number,
                   created_date)
VALUES (500000001, 'opal-test-2@HMCTS.NET', 'GfsHbIMt49WjQ', 'User with no business units', NULL, 0,
        CURRENT_TIMESTAMP),
       (500000002, 'update-user@HMCTS.NET', 'BmMfmuTT9pEdG', 'User for testing \`update\`', NULL, 0,
        CURRENT_TIMESTAMP),
       (500000003, 'test-user@HMCTS.NET', 'jjqwGAERGW43', 'Test User for testing', 'Pablo', 2,
        CURRENT_TIMESTAMP),
       (500000004, 'test-user@HMCTS.NET', '7324-fh42dEsr', 'Test User for testing', 'Pablo', 2,
        CURRENT_TIMESTAMP),
       (500000005, 'update-user@HMCTS.NET', 'QeJjwoWnY-kBmMfm', 'Test User for testing \`update\`', 'Pablo', 7,
        CURRENT_TIMESTAMP),
       (500000006, 'no-go-user@HMCTS.NET', '8hqucbw874fg3', 'User with business units but no entitlements', 'No Permissions', 3,
        CURRENT_TIMESTAMP);


-- Insert business units that are referenced in the business_unit_users script
INSERT INTO business_units (business_unit_id, business_unit_name, business_unit_code, business_unit_type,
                            opal_domain_id)
VALUES (61, 'Test BU 61', 'T61', 'TEST', 1),
       (67, 'Test BU 67', 'T67', 'TEST', 1),
       (68, 'Test BU 68', 'T68', 'TEST', 1),
       (69, 'Test BU 69', 'T69', 'TEST', 1),
       (70, 'Test BU 70', 'T70', 'TEST', 1),
       (71, 'Test BU 71', 'T71', 'TEST', 1),
       (73, 'Test BU 73', 'T73', 'TEST', 1);


-- Insert application functions (permissions) from Flyway script V20240730_006
INSERT INTO application_functions (application_function_id, function_name)
VALUES (35, 'Manual Account Creation'),
       (41, 'Account Enquiry - Account Notes'),
       (54, 'Account Enquiry'),
       (500, 'Collection Order');

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
INSERT INTO user_entitlements (user_entitlement_id, business_unit_user_id, application_function_id)
VALUES (112687, 'L065JG', 41), -- BU 70 gets 'Account Enquiry - Account Notes'
       (112683, 'L065JG', 54), -- BU 70 gets 'Account Enquiry'
       (112921, 'L066JG', 41), -- BU 68 gets 'Account Enquiry - Account Notes'
       (500001, 'L080JG', 500); -- BU 61 gets 'Collection Order'

INSERT INTO roles (role_id, version_number, opal_domain_id, role_name, is_active, application_function_list)
VALUES (1,1, 1, 'Fines_Role_1', true, ARRAY['CREATE_MANAGE_DRAFT_ACCOUNTS', 'ACCOUNT_ENQUIRY_NOTES']),
       (1,2, 1, 'Fines_Role_1', true, ARRAY['CREATE_MANAGE_DRAFT_ACCOUNTS', 'ACCOUNT_ENQUIRY']),
       (2,1, 1, 'Fines_Role_2', true, ARRAY['COLLECTION_ORDER']),
       (2,2, 1, 'Fines_Role_2', true, ARRAY['CHECK_VALIDATE_DRAFT_ACCOUNTS', 'SEARCH_AND_VIEW_ACCOUNTS']),
       (2,3, 1, 'Fines_Role_2', true, ARRAY['COLLECTION_ORDER', 'CHECK_VALIDATE_DRAFT_ACCOUNTS', 'SEARCH_AND_VIEW_ACCOUNTS']),
       (3,1, 2, 'Confiscation_Role_3', true, ARRAY['CREATE_MANAGE_DRAFT_ACCOUNTS']),
       (3,2, 2, 'Confiscation_Role_3', true, ARRAY['CREATE_MANAGE_DRAFT_ACCOUNTS', 'COLLECTION_ORDER']);

INSERT INTO business_unit_user_roles(business_unit_user_role_id, business_unit_user_id, role_id)
VALUES (1,'L065JG', 1),
       (2,'L065JG', 2);

SELECT setval(
    'business_unit_user_role_id_seq',
    COALESCE((SELECT MAX(business_unit_user_role_id) FROM business_unit_user_roles), 1),
    true
);
