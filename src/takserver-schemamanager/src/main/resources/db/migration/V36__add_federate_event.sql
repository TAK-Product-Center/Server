-- group type (picklist / static data). For later use.
create table fed_event_kind_pl (
 id serial primary key,
 event_kind text unique not null
);

create table fed_event (
  fed_id text not null,
  fed_name text not null,
  event_kind_id integer not null,
  event_time timestamp(3) with time zone not null,
  remote boolean not null,
  details jsonb,
  primary key(fed_id, fed_name, event_kind_id, event_time)
);

create index fed_event_details on fed_event using gin (details);

create index fed_event_time on fed_event (event_time);


-- add event kind for disconnect event
insert into fed_event_kind_pl (event_kind) values ('connect');
insert into fed_event_kind_pl (event_kind) values ('disconnect');


-- index for resource add times
create index resource_submissiontime on resource (submissiontime);