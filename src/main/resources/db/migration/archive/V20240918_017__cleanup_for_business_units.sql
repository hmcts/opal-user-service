/**
* OPAL Program
*
* MODULE      : cleanup_for_business_units.sql
*
* DESCRIPTION : Clean up the data in the BUSINESS_UNITS table in order to be able to load Business Units Reference Data from the Legacy GoB environment. 
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    -----------------------------------------------------------------------------------------------------------------------------------------------
* 18/09/2024    A Dennis    1.0         PO-760 Clean up the data in the BUSINESS_UNITS table in order to be able to load Business Units Reference Data from the Legacy GoB environment.  
*
**/

ALTER TABLE business_unit_users
DROP constraint IF EXISTS buu_business_unit_id_fk;

DELETE FROM business_units;
