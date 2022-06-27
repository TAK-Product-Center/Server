

--
-- Migration script to schema version 10: 
-- Schema version 9 was originally committed on 20 July 2015
-- 
--
UPDATE resource SET hash = uid;