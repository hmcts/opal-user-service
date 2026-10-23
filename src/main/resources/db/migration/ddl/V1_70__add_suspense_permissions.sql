/**
* CGI OPAL Program
*
* MODULE      : add_suspense_permissions.sql
*
* DESCRIPTION : Add View Suspense Items and Manage Suspense Items permissions.
*
* VERSION HISTORY:
*
* Date          Author          Version     Nature of Change
* ----------    -----------     --------    --------------------------------------------
* 09/10/2026    Imran Chandio   1.0         PO-10192 Add Suspense 1 permissions for Search and View Item.
*
**/

ALTER TYPE public.t_permissions_enum
    ADD VALUE IF NOT EXISTS 'VIEW_SUSPENSE_ITEMS';

ALTER TYPE public.t_permissions_enum
    ADD VALUE IF NOT EXISTS 'MANAGE_SUSPENSE_ITEMS';