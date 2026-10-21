/**
* OPAL Program
*
* MODULE      : add_permission_manage_alternate_payment_references.sql
*
* DESCRIPTION : Add the Manage Alternate Payment References permission.
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 21/10/2026    T Tang.        1.0         PO-10881 - Create new permission for Manage Alternate Payment References test role
*
**/

ALTER TYPE t_permissions_enum
    ADD VALUE IF NOT EXISTS 'Manage Alternate Payment References';