/**
* CGI OPAL Program
*
* MODULE      : drop_obsolete_entitlement_tables.sql
*
* DESCRIPTION : Drop obsolete entitlement and template tables from the Users database.
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    ----------------------------------------------------------------------------
* 22/04/2026    C Cho       1.0         PO-2828 Drop obsolete USER_ENTITLEMENTS, TEMPLATE_MAPPINGS, TEMPLATES, and APPLICATION_FUNCTIONS tables.
*
**/

DROP TABLE IF EXISTS user_entitlements;

DROP TABLE IF EXISTS template_mappings;

DROP TABLE IF EXISTS templates;

DROP TABLE IF EXISTS application_functions;
