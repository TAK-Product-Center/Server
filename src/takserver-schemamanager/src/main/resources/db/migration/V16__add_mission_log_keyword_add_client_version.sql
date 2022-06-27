create table mission_log_keyword (mission_log_id character varying(255) not null, keyword character varying(255) not null, primary key (mission_log_id, keyword));
alter table client_endpoint_event add column client_version character varying(255);
