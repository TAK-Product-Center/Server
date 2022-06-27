
DROP TABLE IF EXISTS cot_router_chat CASCADE;

CREATE TABLE cot_router_chat
(
  id SERIAL PRIMARY KEY,
  uid character varying NOT NULL,
  cot_type character varying,
  access varchar,
  qos varchar,
  opex varchar,
  "start" timestamp (3) with time zone,
  "time" timestamp (3) with time zone,
  stale timestamp (3) with time zone,
  how varchar,
  point_hae numeric,
  point_ce numeric,
  point_le numeric,
  groups bit varying,
  detail text,
  servertime timestamp (3) with time zone,
  sender_callsign varchar,
  dest_callsign varchar,
  dest_uid varchar,
  chat_content varchar,
  chat_room varchar
)

WITH (
  OIDS=FALSE
);

SELECT AddGeometryColumn('','cot_router_chat','event_pt',4326,'POINT',2);

  
DROP INDEX IF EXISTS chat_cot_type_idx CASCADE;
DROP INDEX IF EXISTS chat_time_idx CASCADE;
DROP INDEX IF EXISTS chat_uid_idx CASCADE;
DROP INDEX IF EXISTS chat_event_pt_idx CASCADE;
DROP INDEX IF EXISTS chat_cot_type_servertime_idx CASCADE;
DROP INDEX IF EXISTS chat_servertime_idx CASCADE;
DROP INDEX IF EXISTS chat_uid_servertime_idx CASCADE;
DROP INDEX IF EXISTS chat_dest_uid_idx CASCADE;
DROP INDEX IF EXISTS chat_dest_uid_servertime_idx CASCADE;

CREATE INDEX chat_cot_type_idx ON cot_router_chat (cot_type);
CREATE INDEX chat_time_idx ON cot_router_chat("time");
CREATE INDEX chat_uid_idx ON cot_router_chat (uid);
CREATE INDEX chat_event_pt_idx ON cot_router_chat USING GIST (event_pt);
CREATE INDEX chat_cot_type_servertime_idx ON cot_router_chat(cot_type, servertime);
CREATE INDEX chat_servertime_idx ON cot_router_chat(servertime);
CREATE INDEX chat_uid_servertime_idx ON cot_router_chat(uid, servertime);
CREATE INDEX chat_dest_uid_idx ON cot_router_chat(dest_uid);
CREATE INDEX chat_dest_uid_servertime_idx ON cot_router_chat(dest_uid, servertime);


DROP INDEX IF EXISTS client_endpoint_uid_idx CASCADE;
DROP INDEX IF EXISTS client_endpoint_event_created_ts_idx CASCADE;

CREATE INDEX client_endpoint_uid_idx ON client_endpoint (uid);
CREATE INDEX client_endpoint_event_created_ts_idx ON client_endpoint_event (created_ts);

ALTER TABLE public.client_endpoint_event ALTER COLUMN created_ts TYPE timestamp (3) with time zone;