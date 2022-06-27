alter table mission add column expiration bigint default -1;
update mission set expiration = -1  where expiration is null;

alter table resource add column expiration bigint default -1;
update resource set expiration = -1  where expiration is null;