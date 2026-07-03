/**
* OPAL Program
*
* MODULE      : insert_domain_for_file_handler.sql
*
* DESCRIPTION : Insert record into DOMAIN for 'File-Handler' domain service.
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 01/07/2026    T McCallion    1.0         PO-7220 - Insert record into DOMAIN for 'File-Handler' domain service.
*
**/

INSERT INTO "domain" (opal_domain_id, opal_domain_name)
VALUES(NEXTVAL('domain_id_seq'), 'File-Handler');