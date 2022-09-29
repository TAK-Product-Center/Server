
DROP TABLE IF EXISTS classification CASCADE;
DROP TABLE IF EXISTS caveat CASCADE;
DROP TABLE IF EXISTS classification_caveat CASCADE;


CREATE TABLE classification (
                      id bigint NOT NULL,
                      level text NOT NULL
);
CREATE SEQUENCE classification_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE classification_id_seq OWNED BY classification.id;
ALTER TABLE ONLY classification ALTER COLUMN id SET DEFAULT nextval('classification_id_seq'::regclass);
ALTER TABLE ONLY classification ADD CONSTRAINT classification_id_key UNIQUE (id);
ALTER TABLE ONLY classification ADD CONSTRAINT classification_pkey PRIMARY KEY (id);

CREATE TABLE caveat (
                            id bigint NOT NULL,
                            name text NOT NULL
);
CREATE SEQUENCE caveat_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE caveat_id_seq OWNED BY caveat.id;
ALTER TABLE ONLY caveat ALTER COLUMN id SET DEFAULT nextval('caveat_id_seq'::regclass);
ALTER TABLE ONLY caveat ADD CONSTRAINT caveat_id_key UNIQUE (id);
ALTER TABLE ONLY caveat ADD CONSTRAINT caveat_pkey PRIMARY KEY (id);


CREATE TABLE classification_caveat (
                                 classification_id bigint NOT NULL,
                                 caveat_id integer NOT NULL
);
ALTER TABLE ONLY classification_caveat ADD CONSTRAINT classification_caveat_pkey PRIMARY KEY (classification_id, caveat_id);
ALTER TABLE ONLY classification_caveat ADD CONSTRAINT classification_caveat_clssification_id_fk FOREIGN KEY (classification_id) REFERENCES classification(id);
ALTER TABLE ONLY classification_caveat ADD CONSTRAINT classification_caveat_caveat_id_fk FOREIGN KEY (caveat_id) REFERENCES caveat(id);


/*
insert into classification (level) values ('UNCLASSIFIED');
insert into classification (level) values ('CONFIDENTIAL');
insert into classification (level) values ('SECRET');
insert into classification (level) values ('TOP SECRET');

insert into caveat (name) values ('FOUO');
insert into caveat (name) values ('NOFORN');
insert into caveat (name) values ('HCS');
insert into caveat (name) values ('SI');
insert into caveat (name) values ('TK');

insert into classification_caveat (classification_id, caveat_id) values (1,1);

insert into classification_caveat (classification_id, caveat_id) values (3,2);

insert into classification_caveat (classification_id, caveat_id) values (4,3);
insert into classification_caveat (classification_id, caveat_id) values (4,4);
insert into classification_caveat (classification_id, caveat_id) values (4,5);
*/