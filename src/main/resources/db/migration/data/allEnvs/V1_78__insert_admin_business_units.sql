/**
* OPAL Program
*
* MODULE      : insert_admin_business_units.sql
*
* DESCRIPTION : Create one Admin business unit for each Opal domain, excluding the User (4) domain
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------
* 20/10/2026    TMc            1.0         PO-10692 - Insert records into BUSINESS_UNITS for Admin BU
*
**/

INSERT INTO business_units (
    business_unit_id, 
    business_unit_name, 
    business_unit_code, 
    business_unit_type, 
    account_number_prefix, 
    parent_business_unit_id, 
    welsh_language, 
    account_number_suffix, 
    opal_domain_id
)
SELECT -d.opal_domain_id         AS business_unit_id
     , 'Admin BU'                AS business_unit_name
     , 'ADM' || d.opal_domain_id AS business_unit_code
     , 'Admin'                   AS business_unit_type
     , NULL                      AS account_number_prefix
     , NULL                      AS parent_business_unit_id
     , FALSE                     AS welsh_language
     , NULL                      AS account_number_suffix
     , d.opal_domain_id
  FROM domain d
 WHERE d.opal_domain_name IN ('Fines', 'Confiscation', 'Maintenance', 'File-Handler')
ORDER BY d.opal_domain_id
ON CONFLICT DO NOTHING;
