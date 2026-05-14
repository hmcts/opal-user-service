/**
*
* OPAL Program
*
* MODULE      : amend_business_units_add_opal_domain_id.sql
*
* DESCRIPTION : Amend BUSINESS_UNITS to introduce Opal domain relationship for User Management Tactical
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    ------------------------------------------------------------------------------------------------
* 04/03/2026    C Cho       1.0         PO-2822 Amend BUSINESS_UNITS to introduce Opal domain relationship for User Management Tactical
*
**/

--Default to 1 (Fines domain ID)
ALTER TABLE business_units ADD COLUMN opal_domain_id smallint DEFAULT 1;

COMMENT ON COLUMN business_units.opal_domain_id IS 'ID of the Opal domain to which the business unit belongs.';

-- Map existing domain values to domain IDs where possible.
UPDATE business_units bu
SET opal_domain_id = d.opal_domain_id
FROM domain d
WHERE bu.opal_domain IS NOT NULL
  AND (
      UPPER(TRIM(bu.opal_domain)) = UPPER(d.opal_domain_name)
      OR (UPPER(TRIM(bu.opal_domain)) = 'RM' AND d.opal_domain_name = 'Maintenance')
  );

ALTER TABLE business_units
    ALTER COLUMN opal_domain_id SET NOT NULL;

ALTER TABLE business_units
    ALTER COLUMN opal_domain_id DROP DEFAULT;

CREATE INDEX business_units_opal_domain_id_idx ON business_units (opal_domain_id);

ALTER TABLE business_units
    ADD CONSTRAINT business_units_opal_domain_id_fk
        FOREIGN KEY (opal_domain_id) REFERENCES domain (opal_domain_id);

ALTER TABLE business_units
    DROP COLUMN opal_domain;
