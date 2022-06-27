
DROP TABLE IF EXISTS public.mission_invitation;
DROP SEQUENCE IF EXISTS public.mission_invitation_id_seq;  

CREATE SEQUENCE public.mission_invitation_id_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 7602
  CACHE 1;

CREATE TABLE public.mission_invitation
(
  id bigint NOT NULL DEFAULT nextval('mission_invitation_id_seq'::regclass),  
  mission_name text NOT NULL,
  invitee text NOT NULL,
  type text NOT null,
  creator_uid text,
  create_time timestamp with time zone,
  
  CONSTRAINT mission_invitation_pkey PRIMARY KEY (id),
  CONSTRAINT mission_invitation_mission_name_invitee_key UNIQUE (mission_name, invitee)
)
WITH (
  OIDS=FALSE
);	
  
  
ALTER TABLE public.mission_change ADD COLUMN servertime timestamp(3) with time zone; 
update mission_change set servertime = ts where servertime is null;


ALTER TABLE public.mission_log ADD COLUMN created timestamp(3) with time zone; 
update mission_log set created = servertime where created is null;