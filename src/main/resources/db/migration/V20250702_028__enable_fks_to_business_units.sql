/**
* OPAL Program
*
* MODULE      : enable_fks_to_business_units.sql
*
* DESCRIPTION : Re-enable foreign key constraints to the business_units table in user service after data reload.
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    -----------------------------------------------------------------------------------------------------------------------------------
* 02/07/2025    C Cho       1.0         PO-1795 Re-enable foreign key constraints to business_units table in user service after data reload.
*
**/

ALTER TABLE business_unit_users ADD CONSTRAINT buu_business_unit_id_fk
FOREIGN KEY (business_unit_id) REFERENCES business_units(business_unit_id);
