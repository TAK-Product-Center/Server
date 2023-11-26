-- clean up any duplicate mission subscriptions that made it in since V58 and add back a unique constraint
delete from mission_subscription where mission_id in ( select mission_id  from mission_subscription group by (mission_id, client_uid, username) having count(*) > 1 );
ALTER TABLE ONLY mission_subscription ADD CONSTRAINT mission_subscription_pkey UNIQUE (mission_id, client_uid, username);
