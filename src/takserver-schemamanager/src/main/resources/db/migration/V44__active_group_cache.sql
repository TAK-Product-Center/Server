
DROP TABLE IF EXISTS active_group_cache CASCADE;

CREATE TABLE active_group_cache
(
  id SERIAL PRIMARY KEY,
  username character varying,
  groupname character varying,
  direction character varying,
  enabled boolean DEFAULT false
)
WITH (
  OIDS=FALSE
);

DROP INDEX IF EXISTS active_group_cache_username_idx CASCADE;
CREATE INDEX active_group_cache_username_idx ON active_group_cache(username);

