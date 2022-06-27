

-- Migration script to TAK Server schema version 13.
-- Updates how servertime_hour gets set in table cot_router
-- Creates tables supporting Contact API


-- *** Change how servertime_hour gets set ***
drop trigger servertime_trigger on cot_router;
alter table cot_router alter column servertime_hour set default date_trunc('hour', now()); 
alter table cot_router alter column servertime set default now(); 


-- *** Contact API changes ***
--Remove existing objects
DROP INDEX IF EXISTS client_endpoint_idx1;
DROP INDEX IF EXISTS client_endpoint_event_idx1;
DROP INDEX IF EXISTS client_endpoint_event_idx2;

DROP TABLE IF EXISTS client_endpoint_event;
DROP TABLE IF EXISTS client_endpoint;
DROP TABLE IF EXISTS connection_event_type;

DROP SEQUENCE IF EXISTS client_endpoint_event_id_seq;
DROP SEQUENCE IF EXISTS client_endpoint_id_seq;
DROP SEQUENCE IF EXISTS connection_event_type_id_seq;

--Create connection event type
CREATE SEQUENCE connection_event_type_id_seq 
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;

CREATE TABLE connection_event_type (
  id integer NOT NULL DEFAULT nextval('connection_event_type_id_seq'::regclass),
  event_name character varying(30) NOT NULL,
  CONSTRAINT connection_event_type_pk PRIMARY KEY (id),
  UNIQUE (event_name)
)
WITH (
  OIDS=FALSE
);

--Create client endpoint
CREATE SEQUENCE client_endpoint_id_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;

CREATE TABLE client_endpoint (
  id integer NOT NULL DEFAULT nextval('client_endpoint_id_seq'::regclass),
  callsign character varying(100) NOT NULL,
  uid character varying(100) NOT NULL,
  CONSTRAINT client_endpoint_pk PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);

CREATE UNIQUE INDEX client_endpoint_idx1 ON client_endpoint(callsign, uid);

COMMENT ON TABLE client_endpoint
  IS 'TAK server client endpoints.';

--Create client endpoint event
CREATE SEQUENCE client_endpoint_event_id_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;

CREATE TABLE client_endpoint_event (
  id integer NOT NULL DEFAULT nextval('client_endpoint_event_id_seq'::regclass),
  client_endpoint_id integer NOT NULL,
  connection_event_type_id integer NOT NULL,
  created_ts timestamp(3) without time zone NOT NULL,
  CONSTRAINT client_endpoint_event_pk PRIMARY KEY (id),
  CONSTRAINT connection_event_type_fk FOREIGN KEY (connection_event_type_id)
      REFERENCES connection_event_type (id) MATCH SIMPLE
      ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT client_endpoint_fk FOREIGN KEY (client_endpoint_id)
      REFERENCES client_endpoint (id) MATCH SIMPLE
      ON UPDATE RESTRICT ON DELETE RESTRICT
);

CREATE INDEX client_endpoint_event_idx1 ON client_endpoint_event(client_endpoint_id);
CREATE INDEX client_endpoint_event_idx2 ON client_endpoint_event(client_endpoint_id, connection_event_type_id);

--Insert data in connection event table
insert into connection_event_type (event_name)
select ('Connected') 
where not exists (select * from connection_event_type cet where cet.event_name = 'Connected');

insert into connection_event_type (event_name)
select ('Disconnected') 
where not exists (select * from connection_event_type cet where cet.event_name = 'Disconnected');



