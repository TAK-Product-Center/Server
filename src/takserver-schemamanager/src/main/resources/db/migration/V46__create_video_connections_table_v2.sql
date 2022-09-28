--
-- Copyright (c) 2016 Raytheon BBN Technologies. Licensed to US Government with unlimited rights.
--

--
-- Migration script to schema version 11
-- Creates the video_connections table.
-- Schema version 11 was originally committedd on 12 February 2016

CREATE TABLE video_connections_v2 (
    id integer NOT NULL,
    uid text,
    active boolean DEFAULT true,
    alias text,
    thumbnail text,
    classification text,
    xml text,
    groups bit varying
 );
 
DROP SEQUENCE IF EXISTS video_connection_id_seq_v2 ;
CREATE SEQUENCE video_connection_id_seq_v2
  START WITH 1
  INCREMENT BY 1 
  NO MINVALUE 
  NO MAXVALUE 
  CACHE 1;
 
ALTER SEQUENCE video_connection_id_seq_v2 OWNED BY video_connections_v2.id;
ALTER TABLE video_connections_v2 ALTER COLUMN id SET DEFAULT nextval('video_connection_id_seq_v2'::regclass);
ALTER TABLE ONLY video_connections_v2 ADD CONSTRAINT video_connection_pkey_v2 PRIMARY KEY (id);