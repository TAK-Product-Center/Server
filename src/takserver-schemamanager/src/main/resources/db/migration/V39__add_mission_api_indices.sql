CREATE INDEX mission_change_uid_idx ON mission_change USING btree (uid);
CREATE INDEX mission_change_hash_idx ON mission_change USING btree (hash);