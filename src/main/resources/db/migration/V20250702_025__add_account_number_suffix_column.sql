/**
* OPAL Program
*
* MODULE      : add_account_number_suffix_column_users_db.sql
*
* DESCRIPTION : Add the column BUSINESS_UNITS.ACCOUNT_NUMBER_SUFFIX to store the Accounting Division suffix used with Account Numbers in the opal-user-db database.
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    -----------------------------------------------------------------------------------------------------------------------------------
* 30/06/2025    C Cho       1.0         PO-1794 Add ACCOUNT_NUMBER_SUFFIX column to BUSINESS_UNITS table in opal-user-db to store the Accounting Division suffix.
*
**/
ALTER TABLE business_units
ADD COLUMN account_number_suffix varchar(2) NULL;

COMMENT ON COLUMN business_units.account_number_suffix IS 'The Accounting Division suffix used with Account Numbers';