/**
* CGI OPAL Program
*
* MODULE      : create_business_unit_user_roles_table.sql
*
* DESCRIPTION : Creates the BUSINESS_UNIT_USER_ROLES table to support role assignment to business unit users.
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    ----------------------------------------------------------------------------
* 23/02/2026    C Cho       1.0         PO-2824 Create BUSINESS_UNIT_USER_ROLES table, sequence, constraints, and indexes.
*
**/

CREATE TABLE business_unit_user_roles
(
 business_unit_user_role_id    bigint        NOT NULL
,business_unit_user_id         varchar(6)    NOT NULL
,role_id                       bigint        NOT NULL
,CONSTRAINT business_unit_user_roles_pk PRIMARY KEY
 (
   business_unit_user_role_id
 )
);

CREATE SEQUENCE business_unit_user_role_id_seq
    INCREMENT 1
    MINVALUE 1
    NO MAXVALUE
    START WITH 1
    CACHE 1
    OWNED BY business_unit_user_roles.business_unit_user_role_id;

COMMENT ON COLUMN business_unit_user_roles.business_unit_user_role_id IS 'Unique ID of this record';
COMMENT ON COLUMN business_unit_user_roles.business_unit_user_id IS 'ID of the business unit user being assigned a role';
COMMENT ON COLUMN business_unit_user_roles.role_id IS 'ID of the role assigned for this record';

ALTER TABLE business_unit_user_roles
    ALTER COLUMN business_unit_user_role_id SET DEFAULT nextval('business_unit_user_role_id_seq');

ALTER TABLE business_unit_user_roles
ADD CONSTRAINT bu_user_roles_business_unit_user_id_fk FOREIGN KEY
(
  business_unit_user_id
)
REFERENCES business_unit_users
(
  business_unit_user_id
);

CREATE INDEX bu_user_roles_business_unit_user_id_idx ON business_unit_user_roles (business_unit_user_id);

CREATE INDEX bu_user_roles_role_id_idx ON business_unit_user_roles (role_id);

ALTER TABLE business_unit_user_roles
ADD CONSTRAINT bu_user_roles_user_role_uk UNIQUE (business_unit_user_id, role_id);
