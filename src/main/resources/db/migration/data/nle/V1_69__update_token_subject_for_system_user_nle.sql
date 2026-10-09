/**
* OPAL Program
*
* MODULE      : update_token_subject_for_system_user_nle.sql
*
* DESCRIPTION : Update the token_subject for the opal-system-user account so QA can test the System user (NLE only)
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 06/10/2026    T McCallion    1.0         PO-8692 - Alter INTERFACE_FILES table for EI4
*
**/
--1f817516-701c-4da4-b5d2-df9db428bf95 is for NLE only
UPDATE users 
   SET token_subject = '1f817516-701c-4da4-b5d2-df9db428bf95'
 WHERE user_id = -1;