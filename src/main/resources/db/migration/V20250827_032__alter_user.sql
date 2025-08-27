/**
* CGI OPAL Program
*
* MODULE      : alter_user.sql
*
* DESCRIPTION : This script alters the USERS table.
*
*               The following columns will be added:
*               status
*               token_subject
*               token_name
*               version_number
*
*               The username column renamed to token_preferred_username
*
*               Comments added to columns
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    --------    --------    ------------------------------------------------------
* 18/08/2025    CL          1.0         PO-1890 - Amend the structure of the USERS table,
*                                       adding new columns, renaming an existing column and
*                                       adding comments.
*
**/

-- Add new columns to the users table   
ALTER TABLE users
ADD COLUMN status VARCHAR(25),
ADD COLUMN token_subject VARCHAR(100),
ADD COLUMN token_name VARCHAR(100),
ADD COLUMN version_number BIGINT;

-- Rename existing column
ALTER TABLE users
RENAME COLUMN username TO token_preferred_username;

-- Add comments to columns
COMMENT ON COLUMN users.token_preferred_username IS 'This matches the e-mail address of the user';
COMMENT ON COLUMN users.password IS 'Password';
COMMENT ON COLUMN users.status IS 'The status of the user account';
COMMENT ON COLUMN users.token_subject IS 'The Subject claim and is by default what Spring Security OAuth2 uses to identify the authenticated user';
COMMENT ON COLUMN users.token_name IS 'The Azure Active Directory attribute - Name';
COMMENT ON COLUMN users.version_number IS 'This stops concurrent users from overwriting each other, and can be used to check that related items have not changed since it was retrieved and prior to them being amended';
