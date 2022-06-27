

--
-- Migration script to schema version 11
-- Creates the video_connections table.
-- Schema version 11 was originally committedd on 12 February 2016

CREATE TABLE video_connections (
    id integer NOT NULL,
    created timestamp(3) with time zone DEFAULT now(),
    deleted boolean DEFAULT false,
    owner text,
    uuid text,
    url text,
    alias text,
    latitude text,
    longitude text,
    heading text,
    fov text,
    range text,
    type text,
    xml text               
 );
 
DROP SEQUENCE IF EXISTS video_connection_id_seq ;
CREATE SEQUENCE video_connection_id_seq 
  START WITH 1
  INCREMENT BY 1 
  NO MINVALUE 
  NO MAXVALUE 
  CACHE 1;
 
ALTER SEQUENCE video_connection_id_seq OWNED BY video_connections.id;
ALTER TABLE video_connections ALTER COLUMN id SET DEFAULT nextval('video_connection_id_seq'::regclass);
ALTER TABLE ONLY video_connections ADD CONSTRAINT video_connection_pkey PRIMARY KEY (id);