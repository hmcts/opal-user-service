/**
* CGI OPAL Program
*
* MODULE      : update_test_users_activation_date.sql
*
* DESCRIPTION : Update seeded Opal test and demo users so their activation date is one second after creation date
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 30/04/2026    C Faulkner     1.0         PO-3845 - Set activation dates for seeded Opal test users
*
**/

UPDATE users
SET activation_date = created_date + INTERVAL '1 second'
WHERE user_id BETWEEN 500000000 AND 500000016
  AND created_date IS NOT NULL;
