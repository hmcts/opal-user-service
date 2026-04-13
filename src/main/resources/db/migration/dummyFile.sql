/**
* CGI OPAL Program
*
* MODULE      : update_opal_users_to_dev_tenant.sql
*
* DESCRIPTION : Update existing Opal test and demo users to use Dev tenant credentials
*               including preferred usernames, token subjects and token names
*
* VERSION HISTORY:
*
* Date          Author         Version     Nature of Change
* ----------    -----------    --------    ----------------------------------------------------------------------------
* 20/03/2026    C Faulkner     1.0         PO-3559 - Update Opal users to use Dev tenant
*aaa
**/

-- Step 1: Move any conflicting rows out of the way so the unique token_subject constraint is not hit
UPDATE users
SET token_subject = 'legacy-' || user_id || '-' || token_subject
WHERE token_subject IN (
                        'SvS8xT7bd9MYoUd3vuscoJoMNpZZuumQhTK6bg62HDY',
                        'gqdtS4nehJ4n3VV0nKQCzWXaJT958Re9tzvhkYodfaI',
                        'xUkh7faQCpDBllPSAb5rEsl4-KzwyD9a8igUz8bSlMM',
                        'm2VXixNBn0xVkD5Fqri-PjnOphQmhvITiwabrRp67J4',
                        'QtXkickfdKjXTM5y8E3dU_jjya0cflhHIpGuM2Zx5FY',
                        'VUDVWRI0jJ-ON2HFM7RLK5xFWCyyhZko2tZTMYjViLw',
                        'W_N0JCo-K1X-M8uAwOtt4y9Czbi3T7Wqm-bZ5rAD9yM',
                        'UXeGv6zahiWfispuIc47PQVHpY8tV6MUhre98EQDyuQ',
                        'MNi_ITRk3yf1qp55ibkcNZZuE4a_6U9DVrQwkTAjfeo',
                        '3zXM-0FzwOym39pVvGEP-JlI7VW39dgxmjlWwPBMsq0',
                        'pos7jVWZf2ZvuJgeVHAsTlI5-HRvutP6lwc7NMRsAp0',
                        'I5JRHfAfM7d0rR8DoITKmM3_trQz5emmq1EX9ubC8aI',
                        'a_8Kdi2TBChB3IpvhYPLatt1MpH8ETNK5ArABeKPbtE',
                        'YAZsvaGptkqSPlhCOYdgDGpy7QmOghRm6VOGSNyZC-0',
                        'upjVwO4u09Lcb_SnknQNf5MQoylUQ71cIOw9s6XmBs8',
                        'd8TExFIuX_dqXmVOSBO7GyWbQj_QVsDVKZeAyWDIZ0k',
                        'FokoJuBMZORr31mROpz8bxtuKe8D2qaYuPu-15NVjzA'
  );

-- Step 2: Update the intended users to the Dev tenant values
UPDATE users
SET
  token_preferred_username = v.token_preferred_username,
  token_subject = v.token_subject,
  token_name = v.token_name,
  version_number = v.version_number
  FROM (
    VALUES
        (500000000, 'opal-test@dev.platform.hmcts.net',  'SvS8xT7bd9MYoUd3vuscoJoMNpZZuumQhTK6bg62HDY', 'opal-test', 0),
        (500000001, 'opal-test-2@dev.platform.hmcts.net', 'gqdtS4nehJ4n3VV0nKQCzWXaJT958Re9tzvhkYodfaI', 'opal-test-2', 0),
        (500000002, 'opal-test-3@dev.platform.hmcts.net', 'xUkh7faQCpDBllPSAb5rEsl4-KzwyD9a8igUz8bSlMM', 'opal-test-3', 0),
        (500000003, 'opal-test-4@dev.platform.hmcts.net', 'm2VXixNBn0xVkD5Fqri-PjnOphQmhvITiwabrRp67J4', 'opal-test-4', 0),
        (500000004, 'opal-test-5@dev.platform.hmcts.net', 'QtXkickfdKjXTM5y8E3dU_jjya0cflhHIpGuM2Zx5FY', 'opal-test-5', 0),
        (500000005, 'opal-test-6@dev.platform.hmcts.net', 'VUDVWRI0jJ-ON2HFM7RLK5xFWCyyhZko2tZTMYjViLw', 'opal-test-6', 0),
        (500000006, 'opal-test-7@dev.platform.hmcts.net', 'W_N0JCo-K1X-M8uAwOtt4y9Czbi3T7Wqm-bZ5rAD9yM', 'opal-test-7', 0),
        (500000007, 'opal-test-8@dev.platform.hmcts.net', 'UXeGv6zahiWfispuIc47PQVHpY8tV6MUhre98EQDyuQ', 'opal-test-8', 0),
        (500000008, 'opal-test-9@dev.platform.hmcts.net', 'MNi_ITRk3yf1qp55ibkcNZZuE4a_6U9DVrQwkTAjfeo', 'opal-test-9', 0),
        (500000009, 'opal-test-10@dev.platform.hmcts.net', '3zXM-0FzwOym39pVvGEP-JlI7VW39dgxmjlWwPBMsq0', 'opal-test-10', 0),
        (500000010, 'opal-test-11@dev.platform.hmcts.net', 'pos7jVWZf2ZvuJgeVHAsTlI5-HRvutP6lwc7NMRsAp0', 'opal-test-11', 0),
        (500000011, 'opal-test-12@dev.platform.hmcts.net', 'I5JRHfAfM7d0rR8DoITKmM3_trQz5emmq1EX9ubC8aI', 'opal-test-12', 0),
        (500000012, 'opal-test-13@dev.platform.hmcts.net', 'a_8Kdi2TBChB3IpvhYPLatt1MpH8ETNK5ArABeKPbtE', 'opal-test-13', 0),
        (500000013, 'opal-test-14@dev.platform.hmcts.net', 'YAZsvaGptkqSPlhCOYdgDGpy7QmOghRm6VOGSNyZC-0', 'opal-test-14', 0),

        (500000014, 'opal-demo-1@dev.platform.hmcts.net', 'upjVwO4u09Lcb_SnknQNf5MQoylUQ71cIOw9s6XmBs8', 'opal-demo-1', 0),
        (500000015, 'opal-demo-2@dev.platform.hmcts.net', 'd8TExFIuX_dqXmVOSBO7GyWbQj_QVsDVKZeAyWDIZ0k', 'opal-demo-2', 0),
        (500000016, 'opal-demo-3@dev.platform.hmcts.net', 'FokoJuBMZORr31mROpz8bxtuKe8D2qaYuPu-15NVjzA', 'opal-demo-3', 0)
) AS v(user_id, token_preferred_username, token_subject, token_name, version_number)
WHERE users.user_id = v.user_id;
