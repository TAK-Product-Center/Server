-- Table: public.mission_layer

DROP TABLE IF EXISTS public.mission_layer;

CREATE TABLE public.mission_layer
(
  uid character varying(255),
  name text,
  type integer,
  parent_node_uid character varying(255),
  mission_id bigint
)
WITH (
  OIDS=FALSE
);

alter table mission_change add column path text;
alter table mission_change add column after text;

alter table maplayer add column path text;
alter table maplayer add column after text;

-- MISSION_MANAGE_LAYERS
insert into permission (permission) values (7);

-- MISSION_OWNER -> MISSION_MANAGE_LAYERS
insert into role_permission (role_id, permission_id) values (1,8);