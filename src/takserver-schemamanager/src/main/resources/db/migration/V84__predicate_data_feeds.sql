alter table data_feed add column predicate_lang text;
alter table data_feed add column data_source_endpoint text;
alter table data_feed add column predicate text;
alter table data_feed add column auth_type text;

insert into data_feed_type_pl (feed_type) values ('Predicate');

alter table data_feed_filter_group drop constraint data_feed_filter_group_data_feed_id_fk;
alter table data_feed_filter_group add constraint data_feed_filter_group_data_feed_id_fk foreign key (data_feed_id) references data_feed(id) on delete cascade;

alter table data_feed_tag drop constraint data_feed_tag_data_feed_id_fk;
alter table data_feed_tag add constraint data_feed_tag_data_feed_id_fk foreign key (data_feed_id) references data_feed(id) on delete cascade;
