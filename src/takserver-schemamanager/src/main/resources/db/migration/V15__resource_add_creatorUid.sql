-- add creatorUid column to resource

alter table resource add column creatorUid character varying(256);
create index resource_creatorUid_idx on resource (creatorUid);
