/**
* CGI OPAL Program
*
* MODULE      : create_business_events_table.sql
*
* DESCRIPTION : Create the business_events table and the supporting enumerated type and sequence for the business_event_id 
*               primary key column
*
* VERSION HISTORY:
*
* Date          Author      Version     Nature of Change
* ----------    --------    --------    ------------------------------------------------------------------------------------
* 19/02/2025    CL          1.0         PO-2826 - Create the business_events table along with the supporting enumerated type 
*                                       and sequence for the business_event_id primary key column
*
**/

-- Drop objects in dependency order
DROP TABLE IF EXISTS business_events;

DROP SEQUENCE IF EXISTS business_event_id_seq;

DROP TYPE IF EXISTS t_event_type_enum;

-- Create Event Type enumerated type 
CREATE TYPE t_event_type_enum AS ENUM ( 'ACCOUNT_ACTIVATION_INITIATED'
                                      , 'ACCOUNT_SUSPENSION_ATTRIBUTES_AMENDED'
                                      , 'ACCOUNT_DEACTIVATION_DATE_AMENDED'
                                      , 'ROLE_ASSIGNED_TO_USER'
                                      , 'BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED'
                                      , 'ROLE_UNASSIGNED_FROM_USER'
                                      , 'FUNCTIONS_ASSOCIATED_TO_ROLE_AMENDED'
                                );

-- Create a BUSINESS_EVENTS table to support audit logging of user and role business events
CREATE TABLE business_events
(
    business_event_id   BIGINT              NOT NULL
   ,event_type          t_event_type_enum   NOT NULL 
   ,subject_user_id     BIGINT              NOT NULL
   ,initiator_user_id   BIGINT              NOT NULL
   ,event_details       JSON                NOT NULL
   ,CONSTRAINT business_events_pk PRIMARY KEY (business_event_id)
   ,CONSTRAINT business_events_subject_user_id_fk FOREIGN KEY (subject_user_id) REFERENCES USERS(user_id) 
   ,CONSTRAINT business_events_initiator_user_id_fk FOREIGN KEY (initiator_user_id) REFERENCES USERS(user_id) 
);

COMMENT ON COLUMN business_events.business_event_id IS 'Unique ID of this record';
COMMENT ON COLUMN business_events.event_type        IS 'Enumerated type value of the event type for this record';
COMMENT ON COLUMN business_events.subject_user_id   IS 'ID of the subject user id assigned for this record';
COMMENT ON COLUMN business_events.initiator_user_id IS 'ID of the initiator user id assigned for this record';
COMMENT ON COLUMN business_events.event_details     IS 'Details logged for this event type record';

-- Create Foreign Key Indexes for subject_user_id and initiator_user_id
CREATE INDEX be_subject_user_id_idx ON business_events (subject_user_id);
CREATE INDEX be_initiator_user_id_idx ON business_events (initiator_user_id);

-- Create a sequence for the business_event_id primary key column

CREATE SEQUENCE business_event_id_seq
    START WITH 1
    INCREMENT BY 1    
    NO MAXVALUE
    CACHE 1
    OWNED BY business_events.business_event_id;

