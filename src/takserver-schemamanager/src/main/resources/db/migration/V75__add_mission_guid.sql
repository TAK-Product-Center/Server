-- add guid to mission table
alter table mission add column guid uuid;

create index mission_guid on mission using btree (guid);


