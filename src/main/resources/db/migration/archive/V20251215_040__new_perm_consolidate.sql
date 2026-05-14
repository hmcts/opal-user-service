/**
* CGI OPAL Program
*
* MODULE      : new_perm_consolidate.sql
*
* DESCRIPTION : Add new permission for Consolidate
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    --------    --------    ---------------------------------------------------------------------
* 15/12/2025    C Larkin    1.0         PO-2423 - User - Create new permission - Consolidate.
*
**/
DO $$
BEGIN
    -- Get the next available ID and insert the new permission record
    INSERT INTO application_functions (application_function_id, function_name) 
    SELECT MAX(application_function_id) + 1, 'Consolidate' 
    FROM application_functions; 

END $$;