/**
* CGI OPAL Program
*
* MODULE      : update_users_add_data_to_new_columns.sql
*
* DESCRIPTION : Update the users table to populate the new status, token_subject and token_name columns.
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
* 21/08/2025    C Larkin   1.0         PO-1891 - Update the users table to populate the new status, token_subject and token_name columns.
*
**/

UPDATE users 
SET status = 'active',
    token_name = SUBSTR(token_preferred_username , 1, GREATEST(CHAR_LENGTH(token_preferred_username) - 10, 0))
WHERE token_preferred_username like 'opal-test%';

UPDATE users
SET token_subject = CASE
                       WHEN token_preferred_username = 'opal-test@HMCTS.NET' THEN 'MGfsHbIMt49WjQeJjwoWnY-kBmMfmuTT9pN4T9ThYKU'
                       WHEN token_preferred_username = 'opal-test-2@HMCTS.NET' THEN 'LsPZItN_-rg7DZz9yfcTt8x2BDwky9IxgraJOyK4aoA'
                       WHEN token_preferred_username = 'opal-test-3@HMCTS.NET' THEN '9fBzWGRxBHQAzljlFqQ9RdzTBd0tkO0WsP86aZxIMDc'
                       WHEN token_preferred_username = 'opal-test-4@HMCTS.NET' THEN 'eLuoqqm1zn-40Ou89CuSlsziQxFfh8aqxLy2vEUqXgA'
                       WHEN token_preferred_username = 'opal-test-5@HMCTS.NET' THEN 'S9RaYbZYMEvI1ew3CG2X5YNOqXR2IJBVAPxpGdpxvKI'                       
                       WHEN token_preferred_username = 'opal-test-6@HMCTS.NET' THEN 'ji01VsgT5VyRCs-umztY_Pkah9__wDxvsVGtnxHofHM'
                       WHEN token_preferred_username = 'opal-test-7@HMCTS.NET' THEN 'FP52judGP5nD1UPC3262YGunmMvb1uvYVE4JaSpDqLA'
                       WHEN token_preferred_username = 'opal-test-8@HMCTS.NET' THEN 'U3BsBg3gNIPA1vXr6HtRgKeEF3CHZURH3VrszvPjGlI'
                       WHEN token_preferred_username = 'opal-test-9@HMCTS.NET' THEN 'VEjxiklPOZt62vUQ5342Ylfa9obDPTRDayu-rh0nWao'
                       WHEN token_preferred_username = 'opal-test-10@HMCTS.NET' THEN 'MNO1_ZqtRCir9Lu0A__Kaw-Bpp0-whJl5eKibo3vxkw'
                       WHEN token_preferred_username = 'opal-test-11@HMCTS.NET' THEN 'CMtE33nL3gdxrnJg1D2DxCVueY8BDIhku0csH3E7OwE'
                       WHEN token_preferred_username = 'opal-test-12@HMCTS.NET' THEN 'adwJfoaNbitMEfM4WoJRFb3LvFEEHk4MKxKT8Kw7cOE'
                       WHEN token_preferred_username = 'opal-test-13@HMCTS.NET' THEN '6gvYzo3C8XsOdXfzz4xxBOImqgdt9j2CBGBbilCpyjw'
                       WHEN token_preferred_username = 'opal-test-14@HMCTS.NET' THEN 'z56VGaXIvDOX3Ro86dkMw20iZHmaQKWKAYTsJON9-bI'
                    END
WHERE token_preferred_username LIKE 'opal-test%';

