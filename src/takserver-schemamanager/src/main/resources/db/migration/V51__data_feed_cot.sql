DROP TABLE IF EXISTS data_feed_type_pl;
create table data_feed_type_pl (
 id serial primary key,
 feed_type text unique not null
);
insert into data_feed_type_pl (feed_type) values ('Streaming');
insert into data_feed_type_pl (feed_type) values ('API');
insert into data_feed_type_pl (feed_type) values ('Plugin');

DROP TABLE IF EXISTS data_feed;
CREATE TABLE data_feed (
    id bigint NOT NULL,
    uuid character varying(255) UNIQUE NOT NULL,
    name character varying(255) NOT NULL,
    type bigint NOT NULL,
    auth character varying(255),
    port bigint,
    auth_required boolean DEFAULT false,
    protocol character varying(255),
    feed_group character varying(255),
    iface character varying(255),
    archive boolean DEFAULT true,
    anongroup boolean DEFAULT false,
    sync boolean DEFAULT false,
    archive_only boolean DEFAULT false,
    core_version bigint,
    core_version_tls_versions character varying(255)
);

CREATE SEQUENCE data_feed_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
    
ALTER SEQUENCE data_feed_id_seq OWNED BY data_feed.id;
ALTER TABLE ONLY data_feed ALTER COLUMN id SET DEFAULT nextval('data_feed_id_seq'::regclass);
ALTER TABLE ONLY data_feed ADD CONSTRAINT data_feed_pkey PRIMARY KEY (id);

DROP TABLE IF EXISTS data_feed_cot;
CREATE TABLE data_feed_cot (
    cot_router_id int NOT NULL,
    data_feed_id int NOT NULL
);

CREATE INDEX data_feed_cot_cot_router_id_idx ON data_feed_cot(cot_router_id);
CREATE INDEX data_feed_cot_data_feed_id_idx ON data_feed_cot(data_feed_id);

CREATE TABLE data_feed_tag (
    data_feed_id bigint NOT NULL,
    tag character varying(255) NOT NULL
);

ALTER TABLE ONLY data_feed_tag ADD CONSTRAINT data_feed_tag_pkey PRIMARY KEY (data_feed_id, tag);
ALTER TABLE ONLY data_feed_tag ADD CONSTRAINT data_feed_tag_data_feed_id_fk FOREIGN KEY (data_feed_id) REFERENCES data_feed(id);

CREATE TABLE data_feed_filter_group (
    data_feed_id bigint NOT NULL,
    filter_group character varying(255) NOT NULL
);

ALTER TABLE ONLY data_feed_filter_group ADD CONSTRAINT data_feed_filter_group_pkey PRIMARY KEY (data_feed_id, filter_group);
ALTER TABLE ONLY data_feed_filter_group ADD CONSTRAINT data_feed_filter_group_data_feed_id_fk FOREIGN KEY (data_feed_id) REFERENCES data_feed(id);
