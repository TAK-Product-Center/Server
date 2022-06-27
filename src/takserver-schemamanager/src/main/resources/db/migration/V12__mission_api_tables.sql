

--
-- Migration script to TAK Server schema version 12
-- Adds support for Mission API
--

-- drop table if exists mission cascade;
-- drop table if exists mission_resource cascade;
-- drop table if exists mission_uid cascade;
-- drop table if exists mission_uids cascade; -- old name
-- drop table if exists mission_keyword cascade;
-- drop table if exists mission_keywords cascade; -- old name
-- drop table if exists mission_change cascade;

-- mission --

CREATE TABLE mission (
    id bigint NOT NULL,
    createtime timestamp(3) with time zone NOT NULL,
    name character varying(255) NOT NULL
);

CREATE SEQUENCE mission_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
    
ALTER SEQUENCE mission_id_seq OWNED BY mission.id;


ALTER TABLE ONLY mission ALTER COLUMN id SET DEFAULT nextval('mission_id_seq'::regclass);

ALTER TABLE ONLY mission ADD CONSTRAINT mission_name_key UNIQUE (name);
ALTER TABLE ONLY mission ADD CONSTRAINT mission_pkey PRIMARY KEY (id);

CREATE INDEX mission_name_time_idx ON mission USING btree (name, createtime);


-- mission join tables --

CREATE TABLE mission_resource (
    mission_id bigint NOT NULL,
    resource_id integer NOT NULL,
    resource_hash varchar(255) not null
);

ALTER TABLE ONLY mission_resource ADD CONSTRAINT mission_resource_pkey PRIMARY KEY (mission_id, resource_id);
ALTER TABLE ONLY mission_resource ADD CONSTRAINT mission_resource_mission_id_fk FOREIGN KEY (mission_id) REFERENCES mission(id);


CREATE TABLE mission_keyword (
    mission_id bigint NOT NULL,
    keyword character varying(255) NOT NULL
);

ALTER TABLE ONLY mission_keyword ADD CONSTRAINT mission_keyword_pkey PRIMARY KEY (mission_id, keyword);
ALTER TABLE ONLY mission_keyword ADD CONSTRAINT mission_keyword_mission_id_fk FOREIGN KEY (mission_id) REFERENCES mission(id);


CREATE TABLE mission_uid (
    mission_id bigint NOT NULL,
    uid character varying(255) NOT NULL
);

ALTER TABLE ONLY mission_uid ADD CONSTRAINT mission_uid_pkey PRIMARY KEY (mission_id, uid);
ALTER TABLE ONLY mission_uid ADD CONSTRAINT mission_uid_mission_id_fk FOREIGN KEY (mission_id) REFERENCES mission(id);


-- mission_change --

CREATE TABLE mission_change (
    id bigint NOT NULL,
    hash character varying(255),
    uid character varying(255),
    mission_name character varying(255),
    ts timestamp(3) with time zone NOT NULL,
    change_type integer NOT NULL,
    mission_id bigint
);

CREATE SEQUENCE mission_change_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE ONLY mission_change ALTER COLUMN id SET DEFAULT nextval('mission_change_id_seq'::regclass);
ALTER TABLE ONLY mission_change ADD CONSTRAINT mission_change_pkey PRIMARY KEY (id);
CREATE INDEX mission_change_type_idx ON mission_change USING btree (change_type);
CREATE INDEX mission_change_main_idx ON mission_change USING btree (mission_name, ts DESC, change_type);



-- add an index on resource hash --

CREATE INDEX resource_hash_idx ON resource USING btree (hash);

-- add an index on mission_change --

CREATE INDEX mission_change_ts_idx ON mission_change USING btree (ts);

