/**
*
* OPAL Program
*
* MODULE      : create_domain_table.sql
*
* DESCRIPTION : Create the DOMAIN table. 
*
* VERSION HISTORY:
*
* Date          Author       Version     Nature of Change 
* ----------    --------     --------    -------------------------------------------------------------
* 05/08/2024    I Readman    1.0         PO-538 Create the DOMAIN table
*
**/     
CREATE TABLE domain
(
 opal_domain_id      smallint      not null
,opal_domain_name    varchar(30)   not null
,CONSTRAINT domain_pk PRIMARY KEY (opal_domain_id)
);

COMMENT ON COLUMN domain.opal_domain_id IS 'Unique ID of this record';
COMMENT ON COLUMN domain.opal_domain_name IS 'Name of the domain';

CREATE SEQUENCE IF NOT EXISTS domain_id_seq INCREMENT 1 START 1 MINVALUE 1 NO MAXVALUE CACHE 20 OWNED BY domain.opal_domain_id;
