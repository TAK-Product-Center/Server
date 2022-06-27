
DROP TABLE IF EXISTS tak_user CASCADE;

CREATE TABLE tak_user (
    id bigint NOT NULL,
    token character varying(255) NOT NULL,
    user_name character varying(255) NOT NULL,
    email_address character varying(255) NOT NULL,
    first_name character varying(255),
    last_name character varying(255),
    phone_number character varying(255),
    organization character varying(255),
    state character varying(255),
    activated boolean NOT NULL DEFAULT false
);

CREATE SEQUENCE tak_user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE tak_user_id_seq OWNED BY tak_user.id;
ALTER TABLE ONLY tak_user ALTER COLUMN id SET DEFAULT nextval('tak_user_id_seq'::regclass);
ALTER TABLE ONLY tak_user ADD CONSTRAINT tak_user_id_key UNIQUE (id);
ALTER TABLE ONLY tak_user ADD CONSTRAINT tak_user_id_pkey PRIMARY KEY (id);
ALTER TABLE ONLY tak_user ADD CONSTRAINT tak_user_user_name_key UNIQUE (user_name);




