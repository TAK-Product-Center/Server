alter table maplayer add column north numeric;
alter table maplayer add column south numeric;
alter table maplayer add column east numeric;
alter table maplayer add column west numeric;
alter table maplayer add column additional_parameters character varying(255);
alter table maplayer add column coordinate_system character varying(255);
alter table maplayer add column version character varying(255);
alter table maplayer add column layers integer;
alter table maplayer add column opacity integer;
