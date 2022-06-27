-- Delete data from the TAK Server database, keeping only data from the past 1 month.

begin;
-- get max ts cot_router id
select max(id) maxid into temp tmp_crid from cot_router where servertime < now() - interval '1 month';

delete from cot_thumbnail ct using cot_router cr left outer join mission_uid mu on mu.uid=cr.uid where ct.cot_id = cr.id and cr.id <= (select maxid from tmp_crid limit 1) and mu.uid is null;
delete from cot_image ci using cot_router cr left outer join mission_uid mu on mu.uid=cr.uid where ci.cot_id = cr.id and cr.id <= (select maxid from tmp_crid limit 1) and mu.uid is null;
delete from cot_link cl using cot_router cr left outer join mission_uid mu on mu.uid=cr.uid where cl.containing_event = cr.id and cl.id <= (select maxid from tmp_crid limit 1) and mu.uid is null;
delete from cot_router using cot_router cr left outer join mission_uid mu on mu.uid=cr.uid where cot_router.id = cr.id and cr.id <= (select maxid from tmp_crid limit 1) and mu.uid is null;

-- uncomment this line to also delete enterprise sync / mission package data
--delete from resource where submissiontime < now() - interval '1 month';

drop table tmp_crid;
commit;

-- The following transaction deletes data from client_endpoint_event, which can accumlate over time.
begin;
select max(id) maxidce into temp tmp_ceid from client_endpoint_event where created_ts < now() - interval '1 month';
delete from client_endpoint_event ce where ce.id <= (select maxidce from tmp_ceid limit 1);
commit;
drop table tmp_ceid;
