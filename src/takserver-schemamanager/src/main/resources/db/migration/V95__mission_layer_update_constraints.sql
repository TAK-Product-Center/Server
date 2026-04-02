alter table only mission_layer drop constraint uid_unique;
alter table only mission_layer add constraint uid_mission_id_unique unique(uid, mission_id);