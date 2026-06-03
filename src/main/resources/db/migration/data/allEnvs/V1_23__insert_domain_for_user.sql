/**
* OPAL Program
*
* MODULE      : V1_23__insert_domain_for_user.sql
*
* DESCRIPTION : Insert record into DOMAIN for 'User' domain service.
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 01/06/2026    T McCallion    1.0         PO-2828 - Insert record into DOMAIN for 'User' domain service.
*
**/

INSERT INTO "domain" (opal_domain_id, opal_domain_name)
VALUES(NEXTVAL('domain_id_seq'), 'User');