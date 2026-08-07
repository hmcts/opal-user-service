/**
* OPAL Program
*
* MODULE      : alter_users_add_is_system_user.sql
*
* DESCRIPTION : Add is_system_user column to users table.
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    --------------------------------------------
* 07/08/2026    TMc            1.0         PO-7238 - Add IS_SYSTEM_USER column to USERS
*
**/

ALTER TABLE users
    ADD COLUMN is_system_user BOOLEAN DEFAULT FALSE NOT NULL;

UPDATE users
   SET is_system_user = TRUE
 WHERE user_id = -1;

COMMENT ON COLUMN users.is_system_user IS 'Flag for whether the user account is a system account.';
