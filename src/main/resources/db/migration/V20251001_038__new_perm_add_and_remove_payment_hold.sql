/**
* CGI OPAL Program
*
* MODULE      : new_perm_add_and_remove_payment_hold.sql
*
* DESCRIPTION : Add new permission for Add and Remove payment hold
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    --------    --------    ---------------------------------------------------------------------
* 22/09/2025    P Brumby    1.0         PO-1928 - User - Create new permission - Add and Remove payment hold.
*
**/
DO $$
BEGIN
    -- Get the next available ID and insert the new permission record
    INSERT INTO application_functions (application_function_id, function_name) 
    SELECT MAX(application_function_id) + 1, 'Add and Remove payment hold' 
    FROM application_functions; 

END $$;