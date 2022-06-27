
--
-- Adds support for mission external data


ALTER TABLE public.mission_change ADD COLUMN external_data_token character varying(255);
ALTER TABLE public.mission_change ADD COLUMN external_data_name character varying(255);
ALTER TABLE public.mission_change ADD COLUMN external_data_tool character varying(255);
ALTER TABLE public.mission_change ADD COLUMN external_data_uid character varying(255);

-- Table: public.mission_external_data

DROP TABLE IF EXISTS public.mission_external_data;

CREATE TABLE public.mission_external_data
(
  id character varying(255),
  name text,
  tool text,
  url_data text,
  url_display text,
  mission_id bigint
)
WITH (
  OIDS=FALSE
);

