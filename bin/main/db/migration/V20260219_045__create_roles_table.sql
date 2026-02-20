/**
* CGI OPAL Program
*
* MODULE      : create_roles_table.sql
*
* DESCRIPTION : Creates the ROLES table to support role-based permission assignment.
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    ----------------------------------------------------------------------------
* 19/02/2026    C Cho       1.0         PO-2823 Create ROLES table, sequence, constraints, and indexes.
*
**/

CREATE TABLE roles
(
 role_id                       bigint          NOT NULL
,version_number                bigint          NOT NULL
,opal_domain_id                smallint        NOT NULL
,role_name                     varchar(100)    NOT NULL
,is_active                     boolean         NOT NULL
,application_function_list     varchar(200)[]  DEFAULT ARRAY[]::varchar(200)[]
,CONSTRAINT roles_pk PRIMARY KEY
 (
   role_id
  ,version_number
 )
);

CREATE SEQUENCE role_id_seq
    INCREMENT 1
    MINVALUE 1
    NO MAXVALUE
    START WITH 1
    CACHE 20
    OWNED BY roles.role_id;

ALTER TABLE roles
    ALTER COLUMN role_id SET DEFAULT nextval('role_id_seq');

ALTER TABLE roles
ADD CONSTRAINT roles_opal_domain_id_fk FOREIGN KEY
(
  opal_domain_id
)
REFERENCES domain
(
  opal_domain_id
);

CREATE INDEX roles_opal_domain_id_idx ON roles (opal_domain_id);

CREATE UNIQUE INDEX roles_name_domain_version_uk_idx
    ON roles (role_name, opal_domain_id, version_number);

ALTER TABLE roles
ADD CONSTRAINT roles_name_domain_version_uk UNIQUE USING INDEX roles_name_domain_version_uk_idx;

CREATE INDEX roles_application_function_list_gin_idx
    ON roles USING GIN (application_function_list);
