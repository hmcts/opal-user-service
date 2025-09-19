/**
* CGI OPAL Program
*
* MODULE      : new_perm_view_creditor_bacs.sql
*
* DESCRIPTION : Add new permission for View Creditor BACS
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    --------    --------    ------------------------------------------------------------
* 16/09/2025    P Brumby    1.0         PO-2003 - User - Create new permission - View Creditor BACS.
*
**/
DO $$
BEGIN
    -- Get the next available ID and insert the new permission record
    INSERT INTO application_functions (application_function_id, function_name)
    SELECT MAX(application_function_id) + 1, 'View Creditor BACS'
    FROM application_functions;  

END $$;