

-- Add device profile schema

DROP TABLE IF EXISTS public.device_profile_file;
DROP TABLE IF EXISTS public.device_profile;

DROP SEQUENCE IF EXISTS device_profile_file_id_seq;
DROP SEQUENCE IF EXISTS device_profile_id_seq;

CREATE TABLE public.device_profile
(
  id bigint NOT NULL,
  name character varying(255),
  groups bit varying,
  apply_on_enrollment boolean DEFAULT true,
  apply_on_connect boolean DEFAULT false,
  active boolean DEFAULT true,
  updated timestamp(3) with time zone NOT NULL
);
  
CREATE SEQUENCE device_profile_id_seq
  START WITH 1
  INCREMENT BY 1 
  NO MINVALUE 
  NO MAXVALUE 
  CACHE 1;   

ALTER SEQUENCE device_profile_id_seq OWNED BY device_profile.id;
ALTER TABLE device_profile ALTER COLUMN id SET DEFAULT nextval('device_profile_id_seq'::regclass);
ALTER TABLE ONLY device_profile ADD CONSTRAINT device_profile_pkey PRIMARY KEY (id);  
ALTER TABLE ONLY device_profile ADD CONSTRAINT device_profile_name_unique UNIQUE (name);  
   
   
CREATE TABLE public.device_profile_file
(
  id bigint NOT NULL,
  name character varying(255),
  data bytea,
  device_profile_id bigint
);

CREATE SEQUENCE device_profile_file_id_seq
  START WITH 1
  INCREMENT BY 1 
  NO MINVALUE 
  NO MAXVALUE 
  CACHE 1;   

ALTER SEQUENCE device_profile_file_id_seq OWNED BY device_profile_file.id;
ALTER TABLE device_profile_file ALTER COLUMN id SET DEFAULT nextval('device_profile_file_id_seq'::regclass);
ALTER TABLE ONLY device_profile_file ADD CONSTRAINT device_profile_file_pkey PRIMARY KEY (id);  

ALTER TABLE ONLY device_profile_file ADD CONSTRAINT device_profile_fkey FOREIGN KEY (device_profile_id)
	REFERENCES public.device_profile (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
CREATE INDEX fki_device_profile_fkey ON public.device_profile_file USING btree (device_profile_id);
