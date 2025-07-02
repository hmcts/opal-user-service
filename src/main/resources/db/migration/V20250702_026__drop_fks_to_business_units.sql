/**
* OPAL Program
*
* MODULE      : drop_fks_to_business_units.sql
*
* DESCRIPTION : Drop foreign key constraints to the business_units table in user service to allow for data reload.
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    -----------------------------------------------------------------------------------------------------------------------------------
* 02/07/2025    C Cho       1.0         PO-1795 Drop foreign key constraints to business_units table in user service to prepare for data reload.
*
**/

ALTER TABLE business_unit_users 
DROP CONSTRAINT IF EXISTS buu_business_unit_id_fk;
