CREATE OR REPLACE FUNCTION insert_data_feed_tags(data_feed_id bigint, VARIADIC data_feed_tags character varying[])
 RETURNS void
 LANGUAGE plpgsql
AS $function$
declare 
    tag varchar;
begin
    FOREACH tag in array data_feed_tags LOOP
        INSERT INTO data_feed_tag (data_feed_id, tag) VALUES( data_feed_id, tag) ON CONFLICT DO NOTHING;
    end LOOP;
END;
$function$
;

CREATE OR REPLACE FUNCTION insert_data_feed_filter_groups(data_feed_id bigint, VARIADIC data_feed_filter_groups character varying[])
 RETURNS void
 LANGUAGE plpgsql
AS $function$
declare 
    filter_group varchar;
begin
    FOREACH filter_group in array data_feed_filter_groups LOOP
        INSERT INTO data_feed_filter_group (data_feed_id, filter_group) VALUES( data_feed_id, filter_group) ON CONFLICT DO NOTHING;
    end LOOP;
END;
$function$
;