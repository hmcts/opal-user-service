/**
* CGI OPAL Program
*
* MODULE      : amend_business_events_add_event_date.sql
*
* DESCRIPTION : Add the event_date column to business_events to support audit logging.
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    -------     --------    ----------------------------------------------------------------------------
* 30/04/2026    C Cho       1.0         PO-3799 Add event_date to BUSINESS_EVENTS for audit logging.
*
**/

ALTER TABLE business_events
    ADD COLUMN IF NOT EXISTS event_date TIMESTAMP;

UPDATE business_events
SET event_date = CURRENT_TIMESTAMP
WHERE event_date IS NULL;

ALTER TABLE business_events
    ALTER COLUMN event_date SET NOT NULL;

COMMENT ON COLUMN business_events.event_date IS 'The date and time that the business event occurred';
