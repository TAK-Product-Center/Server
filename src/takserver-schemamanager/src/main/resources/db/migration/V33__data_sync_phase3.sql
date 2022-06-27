

ALTER TABLE public.mission ADD COLUMN password_hash text;

ALTER TABLE public.mission_invitation ADD COLUMN token text;

ALTER TABLE public.mission_subscription ADD COLUMN uid text;
ALTER TABLE public.mission_subscription ADD COLUMN token text;


update mission_subscription set uid =  uuid_in(md5(random()::text || clock_timestamp()::text)::cstring) where uid is null;



DROP TABLE IF EXISTS role CASCADE;
DROP TABLE IF EXISTS permission CASCADE;
DROP TABLE IF EXISTS role_permission CASCADE;


CREATE TABLE role (
    id bigint NOT NULL,
	role integer NOT NULL
);
CREATE SEQUENCE role_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE role_id_seq OWNED BY role.id;
ALTER TABLE ONLY role ALTER COLUMN id SET DEFAULT nextval('role_id_seq'::regclass);
ALTER TABLE ONLY role ADD CONSTRAINT role_id_key UNIQUE (id);
ALTER TABLE ONLY role ADD CONSTRAINT role_pkey PRIMARY KEY (id);

CREATE TABLE permission (
    id bigint NOT NULL,
	permission integer NOT NULL
);
CREATE SEQUENCE permission_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE permission_id_seq OWNED BY permission.id;
ALTER TABLE ONLY permission ALTER COLUMN id SET DEFAULT nextval('permission_id_seq'::regclass);
ALTER TABLE ONLY permission ADD CONSTRAINT permission_id_key UNIQUE (id);
ALTER TABLE ONLY permission ADD CONSTRAINT permission_pkey PRIMARY KEY (id);


CREATE TABLE role_permission (
    role_id bigint NOT NULL,
    permission_id integer NOT NULL
);
ALTER TABLE ONLY role_permission ADD CONSTRAINT role_permission_pkey PRIMARY KEY (role_id, permission_id);
ALTER TABLE ONLY role_permission ADD CONSTRAINT role_permission_role_id_fk FOREIGN KEY (role_id) REFERENCES role(id);
ALTER TABLE ONLY role_permission ADD CONSTRAINT role_permission_permissions_id_fk FOREIGN KEY (permission_id) REFERENCES permission(id);



-- MISSION_READ
-- MISSION_WRITE
-- MISSION_DELETE
-- MISSION_SET_ROLE
-- MISSION_SET_PASSWORD
insert into permission (permission) values (0);
insert into permission (permission) values (1);
insert into permission (permission) values (2);
insert into permission (permission) values (3);
insert into permission (permission) values (4);

-- MISSION_OWNER
-- MISSION_SUBSCRIBER
-- MISSION_READONLY_SUBSCRIBER
insert into role (role) values (0);
insert into role (role) values (1);
insert into role (role) values (2);

-- MISSION_OWNER -> MISSION_READ, MISSION_WRITE, MISSION_DELETE, MISSION_SET_ROLE, MISSION_SET_PASSWORD
insert into role_permission (role_id, permission_id) values (1,1);
insert into role_permission (role_id, permission_id) values (1,2);
insert into role_permission (role_id, permission_id) values (1,3);
insert into role_permission (role_id, permission_id) values (1,4);
insert into role_permission (role_id, permission_id) values (1,5);

-- MISSION_SUBSCRIBER -> MISSION_READ, MISSION_WRITE
insert into role_permission (role_id, permission_id) values (2,1);
insert into role_permission (role_id, permission_id) values (2,2);

-- MISSION_READONLY_SUBSCRIBER -> MISSION_READ
insert into role_permission (role_id, permission_id) values (3,1);



ALTER TABLE public.mission ADD COLUMN default_role_id bigint;
ALTER TABLE ONLY public.mission ADD CONSTRAINT mission_default_role_id_fk FOREIGN KEY (default_role_id) REFERENCES role(id);

ALTER TABLE public.mission_subscription ADD COLUMN role_id bigint;
ALTER TABLE ONLY public.mission_subscription ADD CONSTRAINT subscription_role_id_fk FOREIGN KEY (role_id) REFERENCES role(id);

ALTER TABLE public.mission_invitation ADD COLUMN role_id bigint;
ALTER TABLE ONLY public.mission_invitation ADD CONSTRAINT invitation_role_id_fk FOREIGN KEY (role_id) REFERENCES role(id);



DROP TABLE IF EXISTS mission_uid_keyword;
DROP TABLE IF EXISTS mission_resource_keyword;


CREATE TABLE mission_uid_keyword (
    id bigint NOT NULL,
    mission_id bigint NOT NULL,
	uid character varying(255) NOT NULL,
    keyword character varying(255) NOT NULL
);

CREATE SEQUENCE mission_uid_keyword_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
	
ALTER SEQUENCE mission_uid_keyword_id_seq OWNED BY mission_uid_keyword.id;
ALTER TABLE ONLY mission_uid_keyword ALTER COLUMN id SET DEFAULT nextval('mission_uid_keyword_id_seq'::regclass);
ALTER TABLE ONLY mission_uid_keyword ADD CONSTRAINT mission_uid_keyword_id_key UNIQUE (id);
ALTER TABLE ONLY mission_uid_keyword ADD CONSTRAINT mission_uid_keyword_pkey PRIMARY KEY (id);
ALTER TABLE ONLY mission_uid_keyword ADD CONSTRAINT mission_uid_keyword_mission_id_fk FOREIGN KEY (mission_id) REFERENCES mission(id);



CREATE TABLE mission_resource_keyword (
    id bigint NOT NULL,
    mission_id bigint NOT NULL,
	hash character varying(255) NOT NULL,
    keyword character varying(255) NOT NULL
);

CREATE SEQUENCE mission_resource_keyword_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
	
ALTER SEQUENCE mission_resource_keyword_id_seq OWNED BY mission_resource_keyword.id;
ALTER TABLE ONLY mission_resource_keyword ALTER COLUMN id SET DEFAULT nextval('mission_resource_keyword_id_seq'::regclass);
ALTER TABLE ONLY mission_resource_keyword ADD CONSTRAINT mission_resource_keyword_id_key UNIQUE (id);
ALTER TABLE ONLY mission_resource_keyword ADD CONSTRAINT mission_resource_keyword_pkey PRIMARY KEY (id);
ALTER TABLE ONLY mission_resource_keyword ADD CONSTRAINT mission_resource_keyword_mission_id_fk FOREIGN KEY (mission_id) REFERENCES mission(id);


insert into mission ( name, create_time, tool, groups, creatorUid ) values ( 'exchecktemplates', now(), 'ExCheck', rpad('', 4096, '1')::bit(4096)::bit varying, 'ExCheck' ) ON CONFLICT ON CONSTRAINT mission_name_key DO NOTHING;
insert into mission ( name, create_time, tool, groups, creatorUid ) values ( 'citrap', now(), 'citrap', rpad('', 4096, '1')::bit(4096)::bit varying, 'CITrapReportService' ) ON CONFLICT ON CONSTRAINT mission_name_key DO NOTHING;
