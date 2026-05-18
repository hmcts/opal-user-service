/**
* CGI OPAL Program
*
* MODULE      : add_permissions_associated_to_role_amended_event_type.sql
*
* DESCRIPTION : Add the PERMISSIONS_ASSOCIATED_TO_ROLE_AMENDED business event type.
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    ----------------------------------------------------------------------------
* 13/04/2026    C Cho       1.0         PO-2830 Add PERMISSIONS_ASSOCIATED_TO_ROLE_AMENDED to t_event_type_enum.
*
**/

ALTER TYPE t_event_type_enum
    ADD VALUE IF NOT EXISTS 'PERMISSIONS_ASSOCIATED_TO_ROLE_AMENDED';
