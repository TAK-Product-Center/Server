

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

SET search_path = public, pg_catalog;

--
-- Ensure PostGIS extension is enabled
--
CREATE EXTENSION IF NOT EXISTS postgis;

--
-- Clear out unneeded extensions put in place by setup scripts for schema versions 7-12
--
DROP EXTENSION IF EXISTS fuzzystrmatch CASCADE;
DROP EXTENSION IF EXISTS postgis_topology CASCADE;

-- From here below, copied from postgis_create_cot_table.sql r2474

-- Clear out everything we are about to (re) create to eliminate any version skew and
-- other maintenance headaches
DROP VIEW IF EXISTS users CASCADE;
DROP VIEW IF EXISTS latestcot CASCADE;

DROP TABLE IF EXISTS cot_thumbnail CASCADE;
DROP TABLE IF EXISTS cot_image CASCADE;
DROP TABLE IF EXISTS cot_router CASCADE;
DROP TABLE IF EXISTS cot_link CASCADE;
DROP TABLE IF EXISTS information_types CASCADE;
DROP TABLE IF EXISTS cot_rs_imagery CASCADE;
DROP TABLE IF EXISTS cot_rs_tmp_imagery CASCADE;
DROP TABLE IF EXISTS subscriptions CASCADE;
DROP TABLE IF EXISTS user_aliases CASCADE;

DROP TABLE IF EXISTS iconset CASCADE;
DROP TABLE IF EXISTS icon CASCADE;

DROP INDEX IF EXISTS bounding_region_idx CASCADE;  
DROP INDEX IF EXISTS ingested_at_idx CASCADE;

DROP INDEX IF EXISTS icon_uid_group_name_name_idx CASCADE;
DROP INDEX IF EXISTS iconset_uid_idx CASCADE;

DROP SEQUENCE IF EXISTS cot_router_seq CASCADE;
DROP SEQUENCE IF EXISTS cot_image_seq CASCADE;
DROP SEQUENCE IF EXISTS cot_thumbnail_seq CASCADE;
DROP SEQUENCE IF EXISTS subscriptions_id_seq CASCADE;
DROP SEQUENCE IF EXISTS resource_id_seq CASCADE;

/* create sequence to generate ids for several tables */

CREATE SEQUENCE cot_router_seq START 101;
CREATE SEQUENCE cot_image_seq START 101;
CREATE SEQUENCE cot_thumbnail_seq START 101;

CREATE TABLE cot_router
(
  id SERIAL PRIMARY KEY,
  uid character varying NOT NULL,
  cot_type character varying,
  access varchar,
  qos varchar,
  opex varchar,
  "start" timestamp (3) without time zone,
  "time" timestamp (3) without time zone,
  stale timestamp (3) without time zone,
  how varchar,
  point_hae numeric,
  point_ce numeric,
  point_le numeric,
  --serialized_obj bytea 
  detail text,
  servertime timestamp (3) without time zone,
  servertime_hour timestamp (3) without time zone
)

WITH (
  OIDS=FALSE
);

SELECT AddGeometryColumn('','cot_router','event_pt',4326,'POINT',2);

create or replace function ts_hour_trigger() returns trigger as $$
      declare ts timestamp (3) without time zone := now();
      begin
      new.servertime := ts;
        new.servertime_hour := date_trunc('hour', ts);
        return new;
      end;
$$ LANGUAGE plpgsql;
CREATE TRIGGER servertime_trigger BEFORE INSERT OR UPDATE ON cot_router FOR EACH ROW EXECUTE PROCEDURE ts_hour_trigger();

CREATE TABLE cot_image
(
  id serial PRIMARY KEY,
  image bytea,
  cot_id integer NOT NULL REFERENCES cot_router(id)
);

CREATE TABLE cot_thumbnail
(
  id serial PRIMARY KEY,
  thumbnail bytea,
  cot_id integer NOT NULL REFERENCES cot_router(id),
  cot_image_id integer NOT NULL REFERENCES cot_image(id)
);

--
-- Name: cot_link; Type: TABLE; Schema: public; Owner; martiuser
--
CREATE TABLE cot_link (
   id SERIAL PRIMARY KEY,
   containing_event integer REFERENCES cot_router (id) NOT NULL, 
   target_uid character varying NOT NULL,
   target_type character varying NOT NULL,
   relation character varying NOT NULL,
   url character varying,
   remarks character varying,
   mime_type character varying,
   version character varying DEFAULT '1.7'
);

--
-- Name: subscriptions; Type: TABLE; Schema: public; Owner: martiuser; Tablespace: 
--
CREATE TABLE subscriptions (
    id integer NOT NULL,
    uid text,
    cot_msg text NOT NULL
);

--
-- Name: subscriptions_id_seq; Type: SEQUENCE; Schema: public; Owner: martiuser
--
CREATE SEQUENCE subscriptions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: subscriptions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: martiuser
--
ALTER SEQUENCE subscriptions_id_seq OWNED BY subscriptions.id;

--
-- Name: id; Type: DEFAULT; Schema: public; Owner: martiuser
--
ALTER TABLE subscriptions ALTER COLUMN id SET DEFAULT nextval('subscriptions_id_seq'::regclass);

--
-- Name: subscriptions_pkey; Type: CONSTRAINT; Schema: public; Owner: martiuser; Tablespace: 
--
ALTER TABLE ONLY subscriptions
    ADD CONSTRAINT subscriptions_pkey PRIMARY KEY (id);


/* Details for adding alias table */

-- Table: user_aliases
CREATE TABLE user_aliases
(
  alias character varying(255) NOT NULL,
  mask character varying(255) NOT NULL,
  CONSTRAINT user_aliases_pkey PRIMARY KEY (alias)
)
WITH (
  OIDS=FALSE
);

-- View: users

CREATE OR REPLACE VIEW users AS 
         SELECT DISTINCT cot_router.uid, cot_router.uid AS name
           FROM cot_router
          WHERE NOT (EXISTS ( SELECT NULL::unknown AS unknown
                   FROM user_aliases
                  WHERE cot_router.uid::text ~~ user_aliases.mask::text))
UNION ALL 
         SELECT DISTINCT user_aliases.mask AS uid, user_aliases.alias AS name
           FROM cot_router, user_aliases
          WHERE cot_router.uid::text ~~ user_aliases.mask::text;

-- Build the "resource" table for Marti enterprise synchronization
-- also known as file sharing 2.0

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;
SET search_path = public, pg_catalog;
SET default_tablespace = '';
SET default_with_oids = false;

-- create the resource table
DROP TABLE IF EXISTS resource CASCADE;
CREATE TABLE resource (
    id SERIAL PRIMARY KEY,
    altitude decimal DEFAULT NULL,
    data bytea,
    filename varchar(2048),
    keywords varchar(128)[],
    location geometry(Point, 4326),
    mimetype varchar(256),
    name varchar(128),
    permissions varchar(128)[],
    remarks varchar(2048),
    submissiontime timestamp without time zone,
    submitter varchar(128),
    uid varchar(128) NOT NULL
);
ALTER TABLE resource ALTER COLUMN submissiontime SET DEFAULT now();

CREATE OR REPLACE VIEW latestresource AS 
 SELECT resource.id, resource.altitude, resource.data, resource.filename, resource.keywords, resource.location, resource.mimetype, resource.name, resource.permissions, resource.remarks, resource.submissiontime, resource.submitter, resource.uid
   FROM resource 
   JOIN ( SELECT resource.uid, max(resource.submissiontime) AS latestupload
           FROM resource
          GROUP BY resource.uid) groupedresource ON resource.uid::text = groupedresource.uid::text AND resource.submissiontime = groupedresource.latestupload
  ORDER BY resource.id;
 
-- create icon and iconset table, for default and custom iconsets
create table icon (
    id  bigserial not null unique,
    bytes bytea, group_name varchar(255),
    iconsetUid varchar(255),
    mimeType varchar(255),
    name varchar(255),
    type2525b varchar(255),
    iconset_id int8,
    created timestamp,
    primary key (id));
    
create table iconset (
    id  bigserial not null unique,
    defaultFriendly varchar(255),
    defaultHostile varchar(255),
    defaultUnknown varchar(255),
    name varchar(255),
    skipResize boolean,
    uid varchar(255) not null unique,
    version int4,
    created timestamp,
    primary key (id));

alter table icon add constraint FK313C79784B3989 foreign key (iconset_id) references iconset;

-- Create a view called LatestCot
--

CREATE OR REPLACE VIEW latestcot AS 
 SELECT cot.id, cot.uid, cot.cot_type, cot.access, cot.qos, cot.opex, cot.start, cot."time", cot.stale, cot.how, cot.point_hae, cot.point_ce, cot.point_le, cot.detail, cot.servertime, cot.event_pt
   FROM cot_router cot
   JOIN ( SELECT cot_router.uid, max(cot_router.servertime) AS lastreceivetime
           FROM cot_router
          GROUP BY cot_router.uid) groupedcot ON cot.uid::text = groupedcot.uid::text AND cot.servertime = groupedcot.lastreceivetime
  ORDER BY cot.id;
  
  -- (Re)create indexes for table cot_router
DROP INDEX IF EXISTS cot_type_idx CASCADE;
DROP INDEX IF EXISTS time_idx CASCADE;
DROP INDEX IF EXISTS uid_idx CASCADE;
DROP INDEX IF EXISTS servertime_idx CASCADE;
DROP INDEX IF EXISTS servertime_hour_idx CASCADE;
DROP INDEX IF EXISTS event_pt_idx CASCADE;
DROP INDEX IF EXISTS uid_servertime_idx CASCADE;
DROP INDEX IF EXISTS cot_type_servertime_idx CASCADE;
DROP INDEX IF EXISTS servertime_no_bts_idx CASCADE;
DROP INDEX IF EXISTS icon_uid_group_name_idx CASCADE;
DROP INDEX IF EXISTS  iconset_uid_idx CASCADE;

CREATE INDEX cot_type_idx ON cot_router (cot_type);
CREATE INDEX time_idx ON cot_router("time");
CREATE INDEX uid_idx ON cot_router (uid);
CREATE INDEX event_pt_idx ON cot_router USING GIST (event_pt);
CREATE INDEX cot_type_servertime_idx ON cot_router(cot_type, servertime);
CREATE INDEX servertime_no_bts_idx ON cot_router(servertime) WHERE cot_type != 'b-t-f';
CREATE INDEX servertime_hour_idx ON cot_router(servertime_hour);
CREATE INDEX servertime_idx ON cot_router(servertime);
CREATE INDEX uid_servertime_idx ON cot_router(uid, servertime);

-- (Re)create indexes for table icon
CREATE INDEX icon_uid_group_name_idx ON icon (iconsetUid, group_name, name);
CREATE INDEX iconset_uid_idx ON iconset (uid);
