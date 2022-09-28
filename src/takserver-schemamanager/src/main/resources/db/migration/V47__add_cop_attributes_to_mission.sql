-- Table: public.mission_feed

DROP TABLE IF EXISTS public.mission_feed;

CREATE TABLE public.mission_feed
(
  uid character varying(255),
  data_feed_uid text,
  filter_bbox text,
  filter_type text,
  filter_callsign text,
  mission_id bigint
)
WITH (
  OIDS=FALSE
);

alter table mission add column base_layer text;
alter table mission add column bbox text;