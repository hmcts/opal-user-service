-- Insert users from Flyway script V20240729_003
INSERT INTO users (user_id, token_preferred_username, password, description)
VALUES (500000000, 'opal-test@HMCTS.NET', 'password', 'User with 7 business units');

INSERT INTO users (user_id, token_preferred_username, token_subject, status, description, token_name, version_number)
VALUES (500000001, 'opal-test-2@HMCTS.NET', 'GfsHbIMt49WjQ', NULL, 'User with no business units', NULL, 0),
       (500000002, 'update-user@HMCTS.NET', 'BmMfmuTT9pEdG', 'CREATED', 'User for testing \`update\`', NULL, 0),
       (500000003, 'test-user@HMCTS.NET', 'jjqwGAERGW43', 'active', 'Test User for testing', 'Pablo', 2),
       (500000004, 'test-user@HMCTS.NET', '7324-fh42dEsr', 'active', 'Test User for testing', 'Pablo', 2),
       (500000005, 'update-user@HMCTS.NET', 'QeJjwoWnY-kBmMfm', 'active', 'Test User for testing \`update\`', 'Pablo', 7);

-- Insert business units that are referenced in the business_unit_users script
INSERT INTO business_units (business_unit_id, business_unit_name, business_unit_code, business_unit_type)
VALUES (61, 'Test BU 61', 'T61', 'TEST'),
       (67, 'Test BU 67', 'T67', 'TEST'),
       (68, 'Test BU 68', 'T68', 'TEST'),
       (69, 'Test BU 69', 'T69', 'TEST'),
       (70, 'Test BU 70', 'T70', 'TEST'),
       (71, 'Test BU 71', 'T71', 'TEST'),
       (73, 'Test BU 73', 'T73', 'TEST');


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
       ('L080JG', 61, 500000000);

-- Grant permissions to User 500000000 from Flyway script V20240730_007
-- Granting a subset for a focused test
INSERT INTO user_entitlements (user_entitlement_id, business_unit_user_id, application_function_id)
VALUES (112687, 'L065JG', 41), -- BU 70 gets 'Account Enquiry - Account Notes'
       (112683, 'L065JG', 54), -- BU 70 gets 'Account Enquiry'
       (112921, 'L066JG', 41), -- BU 68 gets 'Account Enquiry - Account Notes'
       (500001, 'L080JG', 500); -- BU 61 gets 'Collection Order'
