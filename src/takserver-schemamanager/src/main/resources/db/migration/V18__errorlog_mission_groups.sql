
--- edit mission schema
alter table mission add column create_change_id bigint;
alter table mission drop column create_time;

--- error log
CREATE TABLE error_logs
(
  id integer NOT NULL,
  uid text,
  callsign text,
  log text,
  "time" timestamp(3) with time zone DEFAULT now(),
  filename text,
  major_version text,
  minor_version text,
  platform text
);

DROP SEQUENCE IF EXISTS error_logs_id_seq ;
CREATE SEQUENCE error_logs_id_seq 
  START WITH 1
  INCREMENT BY 1 
  NO MINVALUE 
  NO MAXVALUE 
  CACHE 1;

ALTER SEQUENCE error_logs_id_seq OWNED BY error_logs.id;
ALTER TABLE error_logs ALTER COLUMN id SET DEFAULT nextval('error_logs_id_seq'::regclass);
ALTER TABLE ONLY error_logs ADD CONSTRAINT error_logs_pkey PRIMARY KEY (id);

--- groups
-- remove these - old
drop view IF EXISTS users CASCADE;
drop table IF EXISTS user_aliases CASCADE;

-- add groups column, a bit vector per message row
alter table cot_router add column groups bit varying;

-- groups
create table groups (
  id bigserial primary key,
  name text unique not null,
  bitpos integer unique not null,
  create_ts timestamp(3) with time zone not null,
  type integer not null
);

create index groups_name_idx ON groups USING btree (name);

-- group type (picklist / static data). For later use.
create table group_type_pl (
 id serial primary key,
 type text unique not null
);


insert into group_type_pl (type) values ('LDAP');

-- This table will keep track of the last-used bit position for group bit vectors. Since the bit vector is finite resource
-- we don't want gaps in this sequence, which is why this is not a "normal" Postgres sequence.
create table group_bitpos_sequence (
  bitpos integer not null primary key
);

-- This table will always have just one row, which we will update when consuming a new bit vector position. 
insert into group_bitpos_sequence values (0);

