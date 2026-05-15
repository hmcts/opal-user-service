/**
* OPAL Program
*
* MODULE      : restore_for_business_units.sql
*
* DESCRIPTION : Enable the foreign key relationship that was disabled to allow loading of Business Units Reference Data from the Legacy GoB environment. 
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    -------------------------------------------------------------------------------------------------------------------------------------------------
* 18/09/2024    A Dennis    1.0         PO-760 Enable the foreign key relationship that was disabled to allow loading of Business Units Reference Data from the Legacy GoB environment.  
*
**/

-- Put back constraint
ALTER TABLE business_unit_users
ADD CONSTRAINT buu_business_unit_id_fk FOREIGN KEY
(
  business_unit_id
)
REFERENCES business_units
(
  business_unit_id 
);
