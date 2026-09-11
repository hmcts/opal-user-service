/**
* CGI OPAL Program
*
* MODULE      : add_create_manage_draft_casefiles_permission.sql
*
* DESCRIPTION : Add the Create and Manage Draft Casefiles database permission.
**/

ALTER TYPE public.t_permissions_enum
    ADD VALUE IF NOT EXISTS 'CREATE_MANAGE_DRAFT_CASEFILES';
