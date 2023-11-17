-- add mission_id column to mission_invitation
-- backport mission_id based on mission name

alter table mission_invitation add column mission_id bigint;

create index mission_invitation_mission_id on mission_invitation using btree (mission_id);

update mission_invitation set mission_id = s.mission_mi from (select m.id as mission_mi, mi.mission_name as invite_mission_name from mission m inner join mission_invitation mi on lower(m.name) = lower(mi.mission_name)) s where mission_id is null and lower(mission_name) = lower(s.invite_mission_name);
