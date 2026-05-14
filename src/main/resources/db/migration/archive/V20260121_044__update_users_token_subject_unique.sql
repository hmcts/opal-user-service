/**
* CGI OPAL Program
*
* MODULE      : update_users_token_subject_unique.sql
*
* DESCRIPTION : Update token_subject values and add a unique constraint to prevent duplicates.
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    -----------------------------------------------------------------------------
* 26/01/2026    C Cho       1.0         PO-2363 - Update token_subject values and add a unique constraint.
*
**/

UPDATE users
SET token_subject = token_preferred_username
WHERE token_subject IS NULL;

ALTER TABLE users
ADD CONSTRAINT users_token_subject_uk UNIQUE (token_subject);
