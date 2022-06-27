DROP TABLE IF EXISTS device_profile_directory CASCADE;
DROP SEQUENCE IF EXISTS device_profile_directory_id_seq;

CREATE TABLE public.device_profile_directory
(
  id bigint NOT NULL,
  path character varying(255),
  device_profile_id bigint,
  CONSTRAINT device_profile_directory_pkey PRIMARY KEY (id),
  CONSTRAINT device_profile_directory_fkey FOREIGN KEY (device_profile_id)
      REFERENCES public.device_profile (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);

CREATE INDEX fki_device_profile_directory_fkey
  ON public.device_profile_directory
  USING btree
  (device_profile_id);
  
    
CREATE SEQUENCE device_profile_directory_id_seq
  START WITH 1
  INCREMENT BY 1 
  NO MINVALUE 
  NO MAXVALUE 
  CACHE 1;   

ALTER SEQUENCE device_profile_directory_id_seq OWNED BY device_profile_directory.id;
ALTER TABLE device_profile_directory ALTER COLUMN id SET DEFAULT nextval('device_profile_directory_id_seq'::regclass);