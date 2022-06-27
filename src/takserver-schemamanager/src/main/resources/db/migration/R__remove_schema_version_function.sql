

-- Before April 2016, the database function marti_schema_version() was used to identify the schema version.
-- This is no longer neeed; now we're using a Flyway-based schema manager that has richer metadata about the schema.


DROP FUNCTION IF EXISTS marti_schema_version();