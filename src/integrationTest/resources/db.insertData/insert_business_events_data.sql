-- Insert business_events records for integration testing.
-- Prerequisite: run db.insertData/insert_user_state_data.sql first.

-- Make reruns safe for the fixed test IDs below.
DELETE FROM business_events
WHERE business_event_id IN (900000001, 900000002, 900000003);

INSERT INTO business_events (
    business_event_id,
    event_type,
    subject_user_id,
    initiator_user_id,
    event_details
)
VALUES
    (
        900000001,
        'ACCOUNT_ACTIVATION_INITIATED',
        500000000,
        500000001,
        '{"reason":"Integration test seed","source":"sql-script","ticket":"PO-2826"}'
    ),
    (
        900000002,
        'ROLE_ASSIGNED_TO_USER',
        500000002,
        500000000,
        '{"role":"Account Enquiry","scope":"BU-70","source":"sql-script"}'
    ),
    (
        900000003,
        'ROLE_UNASSIGNED_FROM_USER',
        500000003,
        500000000,
        '{"role":"Collection Order","scope":"BU-61","source":"sql-script"}'
    );
