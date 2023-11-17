
--
-- Adds support for persistent Properties 
-- Utilized by Properties API
--

-- propoerties --

CREATE TABLE properties_uid (
    id bigint NOT NULL,
    uid character varying(255) NOT NULL
);

CREATE SEQUENCE properties_uid_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
    
ALTER SEQUENCE properties_uid_id_seq OWNED BY properties_uid.id;

ALTER TABLE ONLY properties_uid ALTER COLUMN id SET DEFAULT nextval('properties_uid_id_seq'::regclass);
ALTER TABLE ONLY properties_uid ADD CONSTRAINT properties_uid_unique UNIQUE (uid);
ALTER TABLE ONLY properties_uid ADD CONSTRAINT properties_uid_id PRIMARY KEY (id);

-- properties join tables ---

CREATE TABLE properties_keys (
	id bigint NOT NULL,
    properties_uid_id bigint NOT NULL,
    key text not null
);

CREATE SEQUENCE properties_key_id_seq	
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
    
ALTER SEQUENCE properties_key_id_seq OWNED BY properties_keys.id;

ALTER TABLE ONLY properties_keys ALTER COLUMN id SET DEFAULT nextval('properties_key_id_seq'::regclass);

ALTER TABLE ONLY properties_keys ADD CONSTRAINT properties_keys_pkey PRIMARY KEY (properties_uid_id, id);
ALTER TABLE ONLY properties_keys ADD CONSTRAINT properties_keys_unique UNIQUE (id);
ALTER TABLE ONLY properties_keys ADD CONSTRAINT properties_keys_properties_id_fk FOREIGN KEY (properties_uid_id) REFERENCES properties_uid(id) ON DELETE CASCADE;


CREATE TABLE properties_value (
	id bigint NOT NULL,
    properties_key_id bigint NOT NULL,
    value text not null
);

CREATE SEQUENCE properties_value_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
    
ALTER SEQUENCE properties_value_id_seq OWNED BY properties_value.id;

ALTER TABLE ONLY properties_value ALTER COLUMN id SET DEFAULT nextval('properties_value_id_seq'::regclass);

ALTER TABLE ONLY properties_value ADD CONSTRAINT properties_value_pkey PRIMARY KEY (properties_key_id, id);
ALTER TABLE ONLY properties_value ADD CONSTRAINT properties_value_key_id_fk FOREIGN KEY (properties_key_id) REFERENCES properties_keys(id) ON DELETE CASCADE;
