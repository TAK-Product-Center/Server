
-- MISSION_UPDATE_GROUPS
insert into permission (permission) values (5);

-- MISSION_OWNER -> MISSION_UPDATE_GROUPS
insert into role_permission (role_id, permission_id) values (1,6);

