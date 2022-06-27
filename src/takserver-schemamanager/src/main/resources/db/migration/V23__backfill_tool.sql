
--
-- backfills tool column in several tables for upgrade scenarios
--

update resource set tool = 'public' where tool is null or tool = '';
update mission set tool = 'public' where tool is null or tool = '';
