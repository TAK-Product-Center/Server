CREATE INDEX IF NOT EXISTS timeseries_idx ON cot_router (uid, id DESC);

update cot_router set groups = lpad(groups::character varying, 32768, '0')::bit(32768)::bit varying where length(groups) = 4096; 
update cot_router_chat set groups = lpad(groups::character varying, 32768, '0')::bit(32768)::bit varying where length(groups) = 4096; 
update resource set groups = lpad(groups::character varying, 32768, '0')::bit(32768)::bit varying where length(groups) = 4096; 
update mission set groups = lpad(groups::character varying, 32768, '0')::bit(32768)::bit varying where length(groups) = 4096; 
update ci_trap set groups = lpad(groups::character varying, 32768, '0')::bit(32768)::bit varying where length(groups) = 4096; 
update device_profile set groups = lpad(groups::character varying, 32768, '0')::bit(32768)::bit varying where length(groups) = 4096; 
update tak_user set groups = lpad(groups::character varying, 32768, '0')::bit(32768)::bit varying where length(groups) = 4096; 
update client_endpoint_event set groups = lpad(groups::character varying, 32768, '0')::bit(32768)::bit varying where length(groups) = 4096; 
update video_connections set groups = lpad(groups::character varying, 32768, '0')::bit(32768)::bit varying where length(groups) = 4096; 
update video_connections_v2 set groups = lpad(groups::character varying, 32768, '0')::bit(32768)::bit varying where length(groups) = 4096; 
update data_feed set groups = lpad(groups::character varying, 32768, '0')::bit(32768)::bit varying where length(groups) = 4096; 
