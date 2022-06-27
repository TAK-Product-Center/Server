

-- Migration script to TAK Server schema version 14
-- Adds support for mission logs.
-- 

CREATE TABLE mission_log (
    id character varying(255) NOT NULL,
    content text NOT NULL,
    creator_uid text,
    dtg timestamp(3) with time zone,
    entry_uid text,
    servertime timestamp(3) with time zone
);

ALTER TABLE ONLY mission_log ADD CONSTRAINT mission_log_pkey PRIMARY KEY (id);

CREATE INDEX mission_log_servertime_idx ON mission_log USING btree (servertime);

CREATE TABLE mission_log_mission_name (
    mission_log_id character varying(255) NOT NULL,
    missionnames character varying(255)
);

CREATE INDEX mission_log_mission_name_idx ON mission_log_mission_name USING btree (missionnames);

ALTER TABLE ONLY mission_log_mission_name ADD CONSTRAINT mission_log_mission_name_pk PRIMARY KEY (mission_log_id, missionnames);

CREATE TABLE mission_log_hash (
    mission_log_id character varying(255) NOT NULL,
    contenthashes text
);

ALTER TABLE ONLY mission_log_hash ADD CONSTRAINT mission_log_hash_pk PRIMARY KEY (mission_log_id, contenthashes);
