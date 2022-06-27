alter table mission rename column createtime to create_time;
alter table mission add column creatoruid character varying(255);
alter table mission_change add column creatoruid character varying(255);


