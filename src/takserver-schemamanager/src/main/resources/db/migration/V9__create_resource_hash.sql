

--
-- Migration script to schema version 9: adds 'hash' column to table 'resource'
-- Schema version 9 was originally committed on 20 July 2015
--

DROP VIEW IF EXISTS latestresource;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE OR REPLACE FUNCTION sha256(bytea) returns text AS $$ 
   SELECT encode(digest($1, 'sha256'), 'hex') 
 $$ LANGUAGE SQL STRICT IMMUTABLE;
 
DROP TRIGGER IF EXISTS hash_trigger ON resource;

ALTER TABLE resource ADD COLUMN hash text;
ALTER TABLE resource ALTER COLUMN submissiontime SET DATA TYPE TIMESTAMP (3) WITH TIME ZONE;
ALTER TABLE resource ALTER COLUMN hash SET NOT NULL;

CREATE OR REPLACE VIEW latestresource AS 
    SELECT resource.id, resource.altitude, resource.data, resource.filename, resource.keywords,
           resource.location, resource.mimetype, resource.name, resource.permissions, resource.remarks, 
           resource.submissiontime, resource.submitter, resource.uid, resource.hash
    FROM resource 
    JOIN ( SELECT resource.uid, max(resource.submissiontime) AS latestupload 
           FROM resource GROUP BY resource.uid) groupedresource 
    ON resource.uid::text = groupedresource.uid::text 
    AND resource.submissiontime = groupedresource.latestupload 
    ORDER BY resource.id;