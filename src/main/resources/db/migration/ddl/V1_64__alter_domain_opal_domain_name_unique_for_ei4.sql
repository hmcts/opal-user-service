/**
* OPAL Program
*
* MODULE      : alter_domain_opal_domain_name_unique_for_ei4.sql
*
* DESCRIPTION : Add unique constraint to domain opal_domain_name
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------
* 01/09/2026    TMc            1.0         PO-8696 - Add unique constraint to DOMAIN.OPAL_DOMAIN_NAME
*
**/

ALTER TABLE domain
    ADD CONSTRAINT domain_name_uk UNIQUE (opal_domain_name);

