
alter table mission_subscription add column username text;
alter table client_endpoint add column username text;

create unique index client_endpoint_idx2 on client_endpoint(callsign, uid, username);