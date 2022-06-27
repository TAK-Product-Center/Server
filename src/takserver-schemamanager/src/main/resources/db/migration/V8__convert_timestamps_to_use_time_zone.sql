--
-- Upgrade to TAK Server schema version 8: mostly, setting timestamps to include the time zeon
-- This schema version was originally checked in 28 April 2015



-- 
-- Drop views so we can later tables the views point to
--
DROP VIEW IF EXISTS latestcot;
DROP VIEW IF EXISTS users;
DROP VIEW IF EXISTS latestresource;

ALTER TABLE public.cot_router ALTER COLUMN "start" TYPE timestamp (3) with time zone;
ALTER TABLE public.cot_router ALTER COLUMN "time" TYPE timestamp (3) with time zone;
ALTER TABLE public.cot_router ALTER COLUMN stale TYPE timestamp (3) with time zone;
ALTER TABLE public.cot_router ALTER COLUMN servertime TYPE timestamp (3) with time zone;
ALTER TABLE public.cot_router ALTER COLUMN servertime_hour TYPE timestamp (3) with time zone;

update public.cot_router set servertime_hour = date_trunc('hour', servertime);

DROP TRIGGER IF EXISTS servertime_trigger ON cot_router;
create or replace function ts_hour_trigger() returns trigger as $$ 
                declare ts timestamp (3) with time zone := now();
                begin
                new.servertime := ts;
                new.servertime_hour := date_trunc('hour', ts); 
                return new;
                end;
 $$ LANGUAGE plpgsql;
 
 CREATE TRIGGER servertime_trigger BEFORE INSERT OR UPDATE ON public.cot_router FOR EACH ROW EXECUTE PROCEDURE ts_hour_trigger();
 
 
 ALTER TABLE public.resource ALTER COLUMN submissiontime TYPE timestamp (3) with time zone;
 ALTER TABLE resource ALTER COLUMN submissiontime SET DEFAULT now();
 
 ALTER TABLE icon ALTER COLUMN created TYPE timestamp with time zone;
 
 ALTER TABLE iconset ALTER COLUMN created TYPE timestamp with time zone;
 
 -- 
 -- Recreate the views we dropped in the beginning
 --
 CREATE OR REPLACE VIEW users AS 
         SELECT DISTINCT cot_router.uid, cot_router.uid AS name
           FROM cot_router
          WHERE NOT (EXISTS ( SELECT NULL::unknown AS unknown
                   FROM user_aliases
                  WHERE cot_router.uid::text ~~ user_aliases.mask::text))
UNION ALL 
         SELECT DISTINCT user_aliases.mask AS uid, user_aliases.alias AS name
           FROM cot_router, user_aliases
          WHERE cot_router.uid::text ~~ user_aliases.mask::text;

CREATE OR REPLACE VIEW latestcot AS 
 SELECT cot.id, cot.uid, cot.cot_type, cot.access, cot.qos, cot.opex, cot.start, cot."time", cot.stale, cot.how, cot.point_hae, cot.point_ce, cot.point_le, cot.detail, cot.servertime, cot.event_pt
   FROM cot_router cot
   JOIN ( SELECT cot_router.uid, max(cot_router.servertime) AS lastreceivetime
           FROM cot_router
          GROUP BY cot_router.uid) groupedcot ON cot.uid::text = groupedcot.uid::text AND cot.servertime = groupedcot.lastreceivetime
  ORDER BY cot.id;

CREATE OR REPLACE VIEW latestresource AS 
 SELECT resource.id, resource.altitude, resource.data, resource.filename, resource.keywords, resource.location, resource.mimetype, resource.name, resource.permissions, resource.remarks, resource.submissiontime, resource.submitter, resource.uid
   FROM resource 
   JOIN ( SELECT resource.uid, max(resource.submissiontime) AS latestupload
           FROM resource
          GROUP BY resource.uid) groupedresource ON resource.uid::text = groupedresource.uid::text AND resource.submissiontime = groupedresource.latestupload
  ORDER BY resource.id;
