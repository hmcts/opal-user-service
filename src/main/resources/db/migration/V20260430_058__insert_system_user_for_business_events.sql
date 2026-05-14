/**
* CGI OPAL Program
*
* MODULE      : insert_system_user_for_business_events.sql
*
* DESCRIPTION : Insert the Opal system user used as the initiator for business events auditing.
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    ----------------------------------------------------------------------------
* 28/04/2026    C Cho       1.0         PO-3796 Insert the Opal system user for business events auditing.
*
**/

INSERT INTO users
(
    user_id,
    token_preferred_username,
    password,
    description,
    created_date,
    token_subject,
    token_name,
    version_number
)
SELECT
    -1,
    'opal-system-user',
    NULL,
    'System user for business event initiator auditing',
    CURRENT_TIMESTAMP,
    'opal-system-user',
    'opal-system-user',
    0
WHERE NOT EXISTS (
    SELECT 1
      FROM users
     WHERE user_id = -1
);
