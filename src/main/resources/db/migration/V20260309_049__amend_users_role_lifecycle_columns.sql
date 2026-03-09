/**
*
* OPAL Program
*
* MODULE      : add_users_role_lifecycle_columns.sql
*
* DESCRIPTION : Amend USERS table for role-based permission lifecycle for User Management Tactical
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    ------------------------------------------------------------------------------------------------
* 09/03/2026    C Cho       1.0         PO-2825 Amend USERS table for role-based permission lifecycle for User Management Tactical
*
**/

ALTER TABLE users
    ADD COLUMN created_date timestamp DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN activation_date timestamp,
    ADD COLUMN suspension_start_date timestamp,
    ADD COLUMN suspension_end_date timestamp,
    ADD COLUMN suspension_reason varchar(250),
    ADD COLUMN deactivation_date timestamp,
    ADD COLUMN last_login_date timestamp;

COMMENT ON COLUMN users.created_date IS 'Date the user account was created in Opal.';
COMMENT ON COLUMN users.activation_date IS 'Date the user account was first activated.';
COMMENT ON COLUMN users.suspension_start_date IS 'Most recent start date for the user account being suspended.';
COMMENT ON COLUMN users.suspension_end_date IS 'Most recent date on which the user account suspension ends.';
COMMENT ON COLUMN users.suspension_reason IS 'Reason for suspension of the user account.';
COMMENT ON COLUMN users.deactivation_date IS 'Date the user account was disabled.';
COMMENT ON COLUMN users.last_login_date IS 'Date the user last logged into Opal.';

ALTER TABLE users
    ALTER COLUMN created_date SET NOT NULL;

ALTER TABLE users
    ALTER COLUMN created_date DROP DEFAULT;

ALTER TABLE users
    ADD CONSTRAINT users_suspension_reason_ck
        CHECK (suspension_start_date IS NULL OR suspension_reason IS NOT NULL);

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS user_domain_fk;

ALTER TABLE users
    DROP COLUMN opal_domain_id,
    DROP COLUMN status;
