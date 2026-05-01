/**
* CGI OPAL Program
*
* MODULE      : drop_roles_is_active_column.sql
*
* DESCRIPTION : Drop IS_ACTIVE column from the ROLES table.
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    ----------------------------------------------------------------------------
* 01/05/2026    C Cho       1.0         PO-3769 Drop IS_ACTIVE column from ROLES.
*
**/

ALTER TABLE roles
DROP COLUMN is_active;
