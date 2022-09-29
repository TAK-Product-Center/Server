alter table maplayer add column min_zoom integer;
alter table maplayer add column max_zoom integer;
alter table maplayer add column tile_type character varying(255);
alter table maplayer add column server_parts character varying(255);
alter table maplayer add column background_color character varying(255);
alter table maplayer add column tile_update character varying(255);
alter table maplayer add column ignore_errors boolean default false;
alter table maplayer add column invert_y_coordinate boolean default false;

alter table maplayer add column mission_id bigint;
