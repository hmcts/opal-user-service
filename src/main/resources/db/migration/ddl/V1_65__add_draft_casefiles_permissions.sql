/**
* CGI OPAL Program
*
* MODULE      : add_draft_casefiles_permissions.sql
*
* DESCRIPTION : Add the creator and checker Draft Casefiles permissions.
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 29/09/2026    Chris Larkin   1.0         PO-10300 / PO-10661 Add the creator and checker Draft Casefiles permissions.
*
**/

ALTER TYPE public.t_permissions_enum
    ADD VALUE IF NOT EXISTS 'CREATE_MANAGE_DRAFT_CASEFILES';

ALTER TYPE public.t_permissions_enum
    ADD VALUE IF NOT EXISTS 'CHECK_VALIDATE_DRAFT_CASEFILES';
