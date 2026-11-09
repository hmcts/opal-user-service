/**
* OPAL Program
*
* MODULE      : add_admin_to_business_unit_type_enum.sql
*
* DESCRIPTION : Add Admin to t_business_unit_type_enum
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------
* 20/10/2026    TMc            1.0         PO-10692 - Insert records into BUSINESS_UNITS for Admin BU
*
**/

ALTER TYPE t_business_unit_type_enum
    ADD VALUE IF NOT EXISTS 'Admin';
