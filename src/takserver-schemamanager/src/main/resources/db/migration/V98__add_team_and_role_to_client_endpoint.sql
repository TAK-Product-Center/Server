alter table client_endpoint add column team text;
alter table client_endpoint add column role text;

drop index client_endpoint_idx2;
create unique index client_endpoint_idx3 on client_endpoint(callsign, uid, username, team, role);
