
-- MISSION_MANAGE_FEEDS
insert into permission (permission) values (6);

-- MISSION_OWNER -> MISSION_MANAGE_FEEDS
insert into role_permission (role_id, permission_id) values (1,7);

