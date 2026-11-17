/**
* OPAL Program
*
* MODULE      : add_permission_publish_fixed_penalties.sql
*
* DESCRIPTION : Add the Publish Fixed Penalties permission.
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 13/10/2026    T McCallion    1.0         PO-10396 - Create new permission for Publish Fixed Penalties test role
*
**/

ALTER TYPE t_permissions_enum
    ADD VALUE IF NOT EXISTS 'PUBLISH_FIXED_PENALTIES';