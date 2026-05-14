/**
*
* OPAL Program
*
* MODULE      : create_base_tables.sql
*
* DESCRIPTION : Create the tables and sequences for the user authentication model.
*
* VERSION HISTORY:
*
* Date          Author       Version     Nature of Change
* ----------    --------     --------    -------------------------------------------------------------
* 26/07/2024    I Readman    1.0         PO-536 Create the tables and sequences for user authentication
*
**/

-- USERS Table
CREATE TABLE users
(
 user_id       bigint          not null
,username      varchar(100)    not null
,password      varchar(1000)
,description   varchar(100)
,CONSTRAINT users_pk PRIMARY KEY (user_id)
);

COMMENT ON COLUMN users.user_id IS 'Unique ID of this record';
COMMENT ON COLUMN users.username IS 'User Name / e-mail';
COMMENT ON COLUMN users.password IS 'Password';
COMMENT ON COLUMN users.description IS 'Description of the user';

CREATE SEQUENCE IF NOT EXISTS user_id_seq INCREMENT 1 START 1 MINVALUE 1 NO MAXVALUE CACHE 20 OWNED BY users.user_id;

-- BUSINESS_UNITS table
CREATE TABLE business_units
(
 business_unit_id smallint not null
,business_unit_name varchar(200) not null
,business_unit_code varchar(4)
,business_unit_type varchar(20) not null
,account_number_prefix varchar(2)
,parent_business_unit_id smallint
,opal_domain varchar(30)
,welsh_language boolean
,CONSTRAINT business_unit_id_pk PRIMARY KEY (business_unit_id)
);

COMMENT ON COLUMN business_units.business_unit_id IS 'Unique ID of this record';
COMMENT ON COLUMN business_units.business_unit_name IS 'Business Unit name';
COMMENT ON COLUMN business_units.business_unit_code IS 'Business unit code';
COMMENT ON COLUMN business_units.business_unit_type IS 'Area or Accounting Division';
COMMENT ON COLUMN business_units.account_number_prefix IS 'Accounting division code that appears before account numbers';
COMMENT ON COLUMN business_units.parent_business_unit_id IS 'ID of the business unit that is the parent for this one';
COMMENT ON COLUMN business_units.opal_domain IS 'When business unit type is Accounting Division, then this value is the opal domain that the business uint is owned by';
COMMENT ON COLUMN business_units.welsh_language IS 'To identify if this is a welsh language business unit in Opal. It does not exist in Legacy GoB';

CREATE SEQUENCE IF NOT EXISTS business_unit_id_seq INCREMENT 1 START 1 MINVALUE 1 NO MAXVALUE CACHE 20 OWNED BY business_units.business_unit_id;

-- BUSINESS_UNIT_USERS table
CREATE TABLE business_unit_users
(
 business_unit_user_id  varchar(6)  not null
,business_unit_id       smallint    not null
,user_id                bigint      not null
,CONSTRAINT business_unit_users_pk PRIMARY KEY (business_unit_user_id)
,CONSTRAINT buu_business_unit_id_fk FOREIGN KEY (business_unit_id) REFERENCES business_units (business_unit_id)
,CONSTRAINT buu_user_id_fk FOREIGN KEY (user_id) REFERENCES users (user_id)
);

COMMENT ON COLUMN business_unit_users.business_unit_user_id IS 'Unique ID of this record';
COMMENT ON COLUMN business_unit_users.business_unit_id IS 'ID of the business unit the user belongs to';
COMMENT ON COLUMN business_unit_users.user_id IS 'The user ID, based on AAD, and is the foreign key to the Users table';

-- APPLICATION_FUNCTIONS table
CREATE TABLE application_functions
(
 application_function_id    bigint          not null
,function_name              varchar(200)    not null
,CONSTRAINT application_functions_pk PRIMARY KEY (application_function_id)
);

COMMENT ON COLUMN application_functions.application_function_id IS 'Unique ID of this record';
COMMENT ON COLUMN application_functions.function_name IS 'Function name';

CREATE SEQUENCE IF NOT EXISTS application_function_id_seq INCREMENT 1 START 1 MINVALUE 1 NO MAXVALUE CACHE 20 OWNED BY application_functions.application_function_id;

-- USER_ENTITLEMENTS table
CREATE TABLE user_entitlements
(
 user_entitlement_id        bigint      not null
,business_unit_user_id      varchar(6)  not null
,application_function_id    bigint      not null
,CONSTRAINT user_entitlements_pk PRIMARY KEY (user_entitlement_id)
,CONSTRAINT ue_application_function_id_fk FOREIGN KEY (application_function_id) REFERENCES application_functions (application_function_id)
,CONSTRAINT ue_business_unit_user_id_fk FOREIGN KEY (business_unit_user_id) REFERENCES business_unit_users (business_unit_user_id)
);

COMMENT ON COLUMN user_entitlements.user_entitlement_id IS 'Unique ID of this record';
COMMENT ON COLUMN user_entitlements.business_unit_user_id IS 'ID of the business unit user this record is for';
COMMENT ON COLUMN user_entitlements.application_function_id IS 'ID of the application function the business unit user can access';

CREATE SEQUENCE IF NOT EXISTS user_entitlement_id_seq INCREMENT 1 START 1 MINVALUE 1 NO MAXVALUE CACHE 20 OWNED BY user_entitlements.user_entitlement_id;

-- TEMPLATES table
CREATE TABLE templates
(
 template_id    bigint          not null
,template_name  varchar(100)
,CONSTRAINT templates_pk PRIMARY KEY (template_id)
);

COMMENT ON COLUMN templates.template_id IS 'Unique ID of this record';
COMMENT ON COLUMN templates.template_name IS 'The template name or description';

CREATE SEQUENCE IF NOT EXISTS template_id_seq INCREMENT 1 START 1 MINVALUE 1 NO MAXVALUE CACHE 20 OWNED BY templates.template_id;

-- TEMPLATE_MAPPINGS table
CREATE TABLE template_mappings
(
 template_id                bigint  not null
,application_function_id    bigint  not null
,CONSTRAINT template_mappings_pk PRIMARY KEY (template_id, application_function_id)
,CONSTRAINT tm_application_function_id_fk FOREIGN KEY (application_function_id) REFERENCES application_functions (application_function_id)
,CONSTRAINT tm_template_id_fk FOREIGN KEY (template_id) REFERENCES templates (template_id)
);

COMMENT ON COLUMN template_mappings.template_id IS 'ID of the template';
COMMENT ON COLUMN template_mappings.application_function_id IS 'ID of the application function';


