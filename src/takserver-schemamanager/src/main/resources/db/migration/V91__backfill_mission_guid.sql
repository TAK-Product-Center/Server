/* backfill mission table with guids. Randomly assign a guid to any row that doesn't have one (will have mission.name) */
update mission set guid = gen_random_uuid() where guid is null;

