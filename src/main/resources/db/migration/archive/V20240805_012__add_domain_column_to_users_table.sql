/**
*
* OPAL Program
*
* MODULE      : add_domain_column.sql
*
* DESCRIPTION : Add the opal_domain_id column to the USERS table. 
*
* VERSION HISTORY:
*
* Date          Author       Version     Nature of Change 
* ----------    --------     --------    ---------------------------------------------------
* 30/07/2024    I Readman    1.0         PO-540 Add OPAL_DOMAIN_ID column to the USERS table 
*
**/
ALTER TABLE USERS
ADD COLUMN opal_domain_id     smallint;

ALTER TABLE USERS
ADD CONSTRAINT user_domain_fk
FOREIGN KEY (opal_domain_id) REFERENCES domain(opal_domain_id);   

COMMENT ON COLUMN users.opal_domain_id IS 'ID of the domain';      
