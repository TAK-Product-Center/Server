

--Remove existing objects
DROP TABLE IF EXISTS certificate_private_key;
DROP TABLE IF EXISTS certificate;

DROP SEQUENCE IF EXISTS certificate_id_seq;

--Create the certificate table and related objects

--Create certificate sequence
CREATE SEQUENCE certificate_id_seq 
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;
  
--Create certificate table
CREATE TABLE certificate (
	id integer NOT NULL DEFAULT nextval('certificate_id_seq'::regclass),
	creator_dn character varying(1000),
	subject_dn character varying(1000) NOT NULL,
	user_dn character varying(1000) NOT NULL,
	issuance_date timestamp(3) with time zone NOT NULL,
	effective_date timestamp(3) with time zone NOT NULL,
	expiration_date timestamp(3) with time zone NOT NULL,
	revocation_date timestamp(3) with time zone,
	certificate text NOT NULL,
	--May want to use a different datatype below ... not sure (bytea or text)
	hash character varying(1000),
	CONSTRAINT certificate_pk PRIMARY KEY(id)
)
WITH (
	OIDS=FALSE
);

CREATE INDEX certificate_idx1 ON certificate (subject_dn);
CREATE INDEX certificate_idx2 ON certificate (hash);

--Create certificate_private_key table and related objects
  
--Create certificate_private_key table (note that structurally this table has a 1:1 cardinality with certificate; 
--we're separating the data for added security. Hence, it should be treated as a subtype table from a structural perspective
--and mapped accordingly in hibernate; i.e., the two tables share the same primary key).
CREATE TABLE certificate_private_key (
	certificate_id integer NOT NULL,
	key_format_code character varying(255) NOT NULL,
	key text NOT NULL,
	CONSTRAINT certificate_private_key_pk PRIMARY KEY(certificate_id),
	CONSTRAINT certificate_fk FOREIGN KEY (certificate_id)
		REFERENCES certificate (id) MATCH SIMPLE
		ON UPDATE RESTRICT ON DELETE CASCADE,
	UNIQUE(key)
)
WITH (
	OIDS=FALSE
);

