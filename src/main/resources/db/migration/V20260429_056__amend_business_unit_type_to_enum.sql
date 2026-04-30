/**
* CGI OPAL Program
*
* MODULE      : amend_business_units_business_unit_type_to_enum.sql
*
* DESCRIPTION : Amend the business_unit_type column within business_units to use an enumerated type
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    ----------------------------------------------------------------------------
* 29/04/2026    CL          1.0         Amend the business_unit_type column within business_units to use an enumerated type
*
**/

CREATE TYPE t_business_unit_type_enum AS ENUM ('Accounting Division', 'Area');

ALTER TABLE business_units
   ALTER COLUMN business_unit_type TYPE t_business_unit_type_enum
   USING business_unit_type::t_business_unit_type_enum;
