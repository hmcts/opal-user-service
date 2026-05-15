--
-- PostgreSQL database dump
--

-- Dumped from database version 17.9 (Debian 17.9-1.pgdg13+1)
-- Dumped by pg_dump version 17.9 (Homebrew)

-- Started on 2026-05-13 10:09:17 BST

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA public IS 'standard public schema';

--
-- Name: t_business_unit_type_enum; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.t_business_unit_type_enum AS ENUM (
    'Accounting Division',
    'Area'
);

--
-- Name: t_event_type_enum; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.t_event_type_enum AS ENUM (
    'ACCOUNT_ACTIVATION_INITIATED',
    'ACCOUNT_SUSPENSION_ATTRIBUTES_AMENDED',
    'ACCOUNT_DEACTIVATION_DATE_AMENDED',
    'ROLE_ASSIGNED_TO_USER',
    'BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED',
    'ROLE_UNASSIGNED_FROM_USER',
    'FUNCTIONS_ASSOCIATED_TO_ROLE_AMENDED'
);

--
-- Name: application_functions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.application_functions (
    application_function_id bigint NOT NULL,
    function_name character varying(200) NOT NULL
);

--
-- Name: COLUMN application_functions.application_function_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.application_functions.application_function_id IS 'Unique ID of this record';

--
-- Name: COLUMN application_functions.function_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.application_functions.function_name IS 'Function name';

--
-- Name: application_function_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.application_function_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: application_function_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.application_function_id_seq OWNED BY public.application_functions.application_function_id;

--
-- Name: business_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.business_events (
    business_event_id bigint NOT NULL,
    event_type public.t_event_type_enum NOT NULL,
    subject_user_id bigint NOT NULL,
    initiator_user_id bigint NOT NULL,
    event_details json NOT NULL
);

--
-- Name: COLUMN business_events.business_event_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.business_events.business_event_id IS 'Unique ID of this record';

--
-- Name: COLUMN business_events.event_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.business_events.event_type IS 'Enumerated type value of the event type for this record';

--
-- Name: COLUMN business_events.subject_user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.business_events.subject_user_id IS 'ID of the subject user id assigned for this record';

--
-- Name: COLUMN business_events.initiator_user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.business_events.initiator_user_id IS 'ID of the initiator user id assigned for this record';

--
-- Name: COLUMN business_events.event_details; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.business_events.event_details IS 'Details logged for this event type record';

--
-- Name: business_event_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.business_event_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: business_event_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.business_event_id_seq OWNED BY public.business_events.business_event_id;

--
-- Name: business_units; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.business_units (
    business_unit_id smallint NOT NULL,
    business_unit_name character varying(200) NOT NULL,
    business_unit_code character varying(4),
    business_unit_type public.t_business_unit_type_enum NOT NULL,
    account_number_prefix character varying(2),
    parent_business_unit_id smallint,
    welsh_language boolean,
    account_number_suffix character varying(2),
    opal_domain_id smallint NOT NULL
);

--
-- Name: COLUMN business_units.business_unit_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.business_units.business_unit_id IS 'Unique ID of this record';

--
-- Name: COLUMN business_units.business_unit_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.business_units.business_unit_name IS 'Business Unit name';

--
-- Name: COLUMN business_units.business_unit_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.business_units.business_unit_code IS 'Business unit code';

--
-- Name: COLUMN business_units.business_unit_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.business_units.business_unit_type IS 'Area or Accounting Division';

--
-- Name: COLUMN business_units.account_number_prefix; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.business_units.account_number_prefix IS 'Accounting division code that appears before account numbers';

--
-- Name: COLUMN business_units.parent_business_unit_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.business_units.parent_business_unit_id IS 'ID of the business unit that is the parent for this one';

--
-- Name: COLUMN business_units.welsh_language; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.business_units.welsh_language IS 'To identify if this is a welsh language business unit in Opal. It does not exist in Legacy GoB';

--
-- Name: COLUMN business_units.account_number_suffix; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.business_units.account_number_suffix IS 'The Accounting Division suffix used with Account Numbers';

--
-- Name: COLUMN business_units.opal_domain_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.business_units.opal_domain_id IS 'ID of the Opal domain to which the business unit belongs.';

--
-- Name: business_unit_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.business_unit_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: business_unit_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.business_unit_id_seq OWNED BY public.business_units.business_unit_id;

--
-- Name: business_unit_user_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.business_unit_user_roles (
    business_unit_user_role_id bigint NOT NULL,
    business_unit_user_id character varying(6) NOT NULL,
    role_id bigint NOT NULL
);

--
-- Name: COLUMN business_unit_user_roles.business_unit_user_role_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.business_unit_user_roles.business_unit_user_role_id IS 'Unique ID of this record';

--
-- Name: COLUMN business_unit_user_roles.business_unit_user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.business_unit_user_roles.business_unit_user_id IS 'ID of the business unit user being assigned a role';

--
-- Name: COLUMN business_unit_user_roles.role_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.business_unit_user_roles.role_id IS 'ID of the role assigned for this record';

--
-- Name: business_unit_user_role_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.business_unit_user_role_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: business_unit_user_role_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.business_unit_user_role_id_seq OWNED BY public.business_unit_user_roles.business_unit_user_role_id;

--
-- Name: business_unit_users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.business_unit_users (
    business_unit_user_id character varying(6) NOT NULL,
    business_unit_id smallint NOT NULL,
    user_id bigint NOT NULL
);

--
-- Name: COLUMN business_unit_users.business_unit_user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.business_unit_users.business_unit_user_id IS 'Unique ID of this record';

--
-- Name: COLUMN business_unit_users.business_unit_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.business_unit_users.business_unit_id IS 'ID of the business unit the user belongs to';

--
-- Name: COLUMN business_unit_users.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.business_unit_users.user_id IS 'The user ID, based on AAD, and is the foreign key to the Users table';

--
-- Name: domain; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.domain (
    opal_domain_id smallint NOT NULL,
    opal_domain_name character varying(30) NOT NULL
);

--
-- Name: COLUMN domain.opal_domain_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.domain.opal_domain_id IS 'Unique ID of this record';

--
-- Name: COLUMN domain.opal_domain_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.domain.opal_domain_name IS 'Name of the domain';

--
-- Name: domain_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.domain_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: domain_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.domain_id_seq OWNED BY public.domain.opal_domain_id;

--
-- Name: roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.roles (
    role_id bigint NOT NULL,
    version_number bigint NOT NULL,
    opal_domain_id smallint NOT NULL,
    role_name character varying(100) NOT NULL,
    is_active boolean NOT NULL,
    application_function_list character varying(200)[] DEFAULT (ARRAY[]::character varying[])::character varying(200)[]
);

--
-- Name: COLUMN roles.role_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.roles.role_id IS 'ID of the role for this record.';

--
-- Name: COLUMN roles.version_number; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.roles.version_number IS 'Version of the application functions (permissions) for this record.';

--
-- Name: COLUMN roles.opal_domain_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.roles.opal_domain_id IS 'ID of the Opal domain to which the role belongs.';

--
-- Name: COLUMN roles.role_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.roles.role_name IS 'Role name, unique within an Opal domain.';

--
-- Name: COLUMN roles.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.roles.is_active IS 'Flag to indicate if this record is the active (current) version of a role.';

--
-- Name: COLUMN roles.application_function_list; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.roles.application_function_list IS 'Array of application functions (permissions set) applicable for this role version.';

--
-- Name: role_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.role_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: role_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.role_id_seq OWNED BY public.roles.role_id;

--
-- Name: templates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.templates (
    template_id bigint NOT NULL,
    template_name character varying(100)
);

--
-- Name: COLUMN templates.template_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.templates.template_id IS 'Unique ID of this record';

--
-- Name: COLUMN templates.template_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.templates.template_name IS 'The template name or description';

--
-- Name: template_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.template_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: template_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.template_id_seq OWNED BY public.templates.template_id;

--
-- Name: template_mappings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.template_mappings (
    template_id bigint NOT NULL,
    application_function_id bigint NOT NULL
);

--
-- Name: COLUMN template_mappings.template_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.template_mappings.template_id IS 'ID of the template';

--
-- Name: COLUMN template_mappings.application_function_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.template_mappings.application_function_id IS 'ID of the application function';

--
-- Name: user_entitlements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_entitlements (
    user_entitlement_id bigint NOT NULL,
    business_unit_user_id character varying(6) NOT NULL,
    application_function_id bigint NOT NULL
);

--
-- Name: COLUMN user_entitlements.user_entitlement_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_entitlements.user_entitlement_id IS 'Unique ID of this record';

--
-- Name: COLUMN user_entitlements.business_unit_user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_entitlements.business_unit_user_id IS 'ID of the business unit user this record is for';

--
-- Name: COLUMN user_entitlements.application_function_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_entitlements.application_function_id IS 'ID of the application function the business unit user can access';

--
-- Name: user_entitlement_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_entitlement_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: user_entitlement_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_entitlement_id_seq OWNED BY public.user_entitlements.user_entitlement_id;

--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    user_id bigint NOT NULL,
    token_preferred_username character varying(100) NOT NULL,
    password character varying(1000),
    description character varying(300),
    token_subject character varying(100),
    token_name character varying(100),
    version_number bigint,
    created_date timestamp without time zone NOT NULL,
    activation_date timestamp without time zone,
    suspension_start_date timestamp without time zone,
    suspension_end_date timestamp without time zone,
    suspension_reason character varying(250),
    deactivation_date timestamp without time zone,
    last_login_date timestamp without time zone,
    CONSTRAINT users_suspension_reason_cc CHECK (((suspension_start_date IS NULL) OR (suspension_reason IS NOT NULL)))
);

--
-- Name: COLUMN users.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.user_id IS 'Unique ID of this record';

--
-- Name: COLUMN users.token_preferred_username; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.token_preferred_username IS 'This matches the e-mail address of the user';

--
-- Name: COLUMN users.password; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.password IS 'Password';

--
-- Name: COLUMN users.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.description IS 'Description of the user';

--
-- Name: COLUMN users.token_subject; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.token_subject IS 'The Subject claim and is by default what Spring Security OAuth2 uses to identify the authenticated user';

--
-- Name: COLUMN users.token_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.token_name IS 'The Azure Active Directory attribute - Name';

--
-- Name: COLUMN users.version_number; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.version_number IS 'This stops concurrent users from overwriting each other, and can be used to check that related items have not changed since it was retrieved and prior to them being amended';

--
-- Name: COLUMN users.created_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.created_date IS 'Date the user account was created in Opal.';

--
-- Name: COLUMN users.activation_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.activation_date IS 'Date the user account was first activated.';

--
-- Name: COLUMN users.suspension_start_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.suspension_start_date IS 'Most recent start date for the user account being suspended.';

--
-- Name: COLUMN users.suspension_end_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.suspension_end_date IS 'Most recent date on which the user account suspension ends.';

--
-- Name: COLUMN users.suspension_reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.suspension_reason IS 'Reason for suspension of the user account.';

--
-- Name: COLUMN users.deactivation_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.deactivation_date IS 'Date the user account was disabled.';

--
-- Name: COLUMN users.last_login_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.last_login_date IS 'Date the user last logged into Opal.';

--
-- Name: user_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_id_seq OWNED BY public.users.user_id;

--
-- Name: v_current_roles; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.v_current_roles AS
 SELECT r.role_id,
    r.version_number,
    r.opal_domain_id,
    r.role_name,
    r.is_active,
    r.application_function_list
   FROM (public.roles r
     JOIN ( SELECT roles.role_id,
            max(roles.version_number) AS max_version_number
           FROM public.roles
          GROUP BY roles.role_id) roles_agg ON (((r.role_id = roles_agg.role_id) AND (r.version_number = roles_agg.max_version_number))));

--
-- Name: business_unit_user_roles business_unit_user_role_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business_unit_user_roles ALTER COLUMN business_unit_user_role_id SET DEFAULT nextval('public.business_unit_user_role_id_seq'::regclass);

--
-- Name: roles role_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles ALTER COLUMN role_id SET DEFAULT nextval('public.role_id_seq'::regclass);

--
-- Name: application_functions application_functions_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.application_functions
    ADD CONSTRAINT application_functions_pk PRIMARY KEY (application_function_id);

--
-- Name: business_unit_user_roles bu_user_roles_user_role_uk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business_unit_user_roles
    ADD CONSTRAINT bu_user_roles_user_role_uk UNIQUE (business_unit_user_id, role_id);

--
-- Name: business_events business_events_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business_events
    ADD CONSTRAINT business_events_pk PRIMARY KEY (business_event_id);

--
-- Name: business_units business_unit_id_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business_units
    ADD CONSTRAINT business_unit_id_pk PRIMARY KEY (business_unit_id);

--
-- Name: business_unit_user_roles business_unit_user_roles_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business_unit_user_roles
    ADD CONSTRAINT business_unit_user_roles_pk PRIMARY KEY (business_unit_user_role_id);

--
-- Name: business_unit_users business_unit_users_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business_unit_users
    ADD CONSTRAINT business_unit_users_pk PRIMARY KEY (business_unit_user_id);

--
-- Name: domain domain_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.domain
    ADD CONSTRAINT domain_pk PRIMARY KEY (opal_domain_id);

--
-- Name: roles roles_name_domain_version_uk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_name_domain_version_uk UNIQUE (role_name, opal_domain_id, version_number);

--
-- Name: roles roles_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pk PRIMARY KEY (role_id, version_number);

--
-- Name: template_mappings template_mappings_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.template_mappings
    ADD CONSTRAINT template_mappings_pk PRIMARY KEY (template_id, application_function_id);

--
-- Name: templates templates_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.templates
    ADD CONSTRAINT templates_pk PRIMARY KEY (template_id);

--
-- Name: user_entitlements user_entitlements_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_entitlements
    ADD CONSTRAINT user_entitlements_pk PRIMARY KEY (user_entitlement_id);

--
-- Name: users users_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pk PRIMARY KEY (user_id);

--
-- Name: users users_token_subject_uk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_token_subject_uk UNIQUE (token_subject);

--
-- Name: be_initiator_user_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX be_initiator_user_id_idx ON public.business_events USING btree (initiator_user_id);

--
-- Name: be_subject_user_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX be_subject_user_id_idx ON public.business_events USING btree (subject_user_id);

--
-- Name: bu_user_roles_business_unit_user_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX bu_user_roles_business_unit_user_id_idx ON public.business_unit_user_roles USING btree (business_unit_user_id);

--
-- Name: bu_user_roles_role_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX bu_user_roles_role_id_idx ON public.business_unit_user_roles USING btree (role_id);

--
-- Name: business_units_opal_domain_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX business_units_opal_domain_id_idx ON public.business_units USING btree (opal_domain_id);

--
-- Name: roles_application_function_list_gin_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX roles_application_function_list_gin_idx ON public.roles USING gin (application_function_list);

--
-- Name: roles_opal_domain_id_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX roles_opal_domain_id_idx ON public.roles USING btree (opal_domain_id);

--
-- Name: business_unit_user_roles bu_user_roles_business_unit_user_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business_unit_user_roles
    ADD CONSTRAINT bu_user_roles_business_unit_user_id_fk FOREIGN KEY (business_unit_user_id) REFERENCES public.business_unit_users(business_unit_user_id);

--
-- Name: business_events business_events_initiator_user_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business_events
    ADD CONSTRAINT business_events_initiator_user_id_fk FOREIGN KEY (initiator_user_id) REFERENCES public.users(user_id);

--
-- Name: business_events business_events_subject_user_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business_events
    ADD CONSTRAINT business_events_subject_user_id_fk FOREIGN KEY (subject_user_id) REFERENCES public.users(user_id);

--
-- Name: business_units business_units_opal_domain_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business_units
    ADD CONSTRAINT business_units_opal_domain_id_fk FOREIGN KEY (opal_domain_id) REFERENCES public.domain(opal_domain_id);

--
-- Name: business_unit_users buu_business_unit_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business_unit_users
    ADD CONSTRAINT buu_business_unit_id_fk FOREIGN KEY (business_unit_id) REFERENCES public.business_units(business_unit_id);

--
-- Name: business_unit_users buu_user_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.business_unit_users
    ADD CONSTRAINT buu_user_id_fk FOREIGN KEY (user_id) REFERENCES public.users(user_id);

--
-- Name: roles roles_opal_domain_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_opal_domain_id_fk FOREIGN KEY (opal_domain_id) REFERENCES public.domain(opal_domain_id);

--
-- Name: template_mappings tm_application_function_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.template_mappings
    ADD CONSTRAINT tm_application_function_id_fk FOREIGN KEY (application_function_id) REFERENCES public.application_functions(application_function_id);

--
-- Name: template_mappings tm_template_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.template_mappings
    ADD CONSTRAINT tm_template_id_fk FOREIGN KEY (template_id) REFERENCES public.templates(template_id);

--
-- Name: user_entitlements ue_application_function_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_entitlements
    ADD CONSTRAINT ue_application_function_id_fk FOREIGN KEY (application_function_id) REFERENCES public.application_functions(application_function_id);

--
-- Name: user_entitlements ue_business_unit_user_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_entitlements
    ADD CONSTRAINT ue_business_unit_user_id_fk FOREIGN KEY (business_unit_user_id) REFERENCES public.business_unit_users(business_unit_user_id);

-- Completed on 2026-05-13 10:09:18 BST

--
-- PostgreSQL database dump complete
--

