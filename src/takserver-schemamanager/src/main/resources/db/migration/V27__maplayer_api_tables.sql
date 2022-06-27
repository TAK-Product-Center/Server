
--
-- Adds support for MapLayer API


-- maplayer --

CREATE TABLE maplayer (
    id bigint NOT NULL,
    create_time timestamp(3) with time zone NOT NULL,
    modified_time timestamp(3) with time zone NOT NULL,
    uid character varying(255) NOT NULL,
    creator_uid character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    type character varying(255) NOT NULL,
    url character varying(255) NOT NULL,
    default_layer boolean DEFAULT false,
    enabled boolean DEFAULT false
);

DROP SEQUENCE IF EXISTS maplayer_id_seq;
CREATE SEQUENCE maplayer_id_seq
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;

ALTER SEQUENCE maplayer_id_seq OWNED BY maplayer.id;
ALTER TABLE maplayer ALTER COLUMN id SET DEFAULT nextval('maplayer_id_seq'::regclass);
ALTER TABLE ONLY maplayer ADD CONSTRAINT maplayer_pkey PRIMARY KEY (id);

CREATE INDEX maplayer_uid_idx ON maplayer USING btree (uid);
CREATE INDEX maplayer_default_layer_idx ON maplayer USING btree (default_layer);




