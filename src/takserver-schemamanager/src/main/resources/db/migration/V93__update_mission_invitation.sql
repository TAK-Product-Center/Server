alter table mission_invitation drop column mission_guid;
alter table mission_invitation add column mission_guid uuid;