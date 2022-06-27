

--
-- restores groups column in latestresource
--

DROP VIEW IF EXISTS latestresource;

CREATE OR REPLACE VIEW latestresource AS 
    SELECT resource.id, resource.altitude, resource.data, resource.filename, resource.keywords,
           resource.location, resource.mimetype, resource.name, resource.permissions, resource.remarks, 
           resource.submissiontime, resource.submitter, resource.uid, resource.hash, resource.groups, resource.tool
    FROM resource 
    JOIN ( SELECT resource.uid, max(resource.submissiontime) AS latestupload 
           FROM resource GROUP BY resource.uid) groupedresource 
    ON resource.uid::text = groupedresource.uid::text 
    AND resource.submissiontime = groupedresource.latestupload 
    ORDER BY resource.id;
