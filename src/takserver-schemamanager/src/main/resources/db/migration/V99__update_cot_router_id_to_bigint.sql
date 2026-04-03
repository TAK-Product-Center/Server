
ALTER TABLE public.cot_image DROP CONSTRAINT cot_image_cot_id_fkey;
ALTER TABLE public.cot_link DROP CONSTRAINT cot_link_containing_event_fkey;
ALTER TABLE public.cot_thumbnail DROP CONSTRAINT cot_thumbnail_cot_id_fkey;

DROP VIEW IF EXISTS public.latestcot;

ALTER TABLE public.cot_image
ALTER COLUMN cot_id TYPE BIGINT;

ALTER TABLE public.cot_link
ALTER COLUMN containing_event TYPE BIGINT;

ALTER TABLE public.cot_thumbnail
ALTER COLUMN cot_id TYPE BIGINT;

ALTER SEQUENCE public.cot_router_id_seq AS BIGINT;

ALTER TABLE public.cot_router
ALTER COLUMN id TYPE BIGINT;

ALTER TABLE public.cot_router
    ALTER COLUMN id SET DEFAULT nextval('cot_router_id_seq'::regclass);

ALTER TABLE public.cot_image
    ADD CONSTRAINT cot_image_cot_id_fkey
        FOREIGN KEY (cot_id)
            REFERENCES public.cot_router (id);

ALTER TABLE public.cot_link
    ADD CONSTRAINT cot_link_containing_event_fkey
        FOREIGN KEY (containing_event)
            REFERENCES public.cot_router (id);

ALTER TABLE public.cot_thumbnail
    ADD CONSTRAINT cot_thumbnail_cot_id_fkey
        FOREIGN KEY (cot_id)
            REFERENCES public.cot_router (id);

CREATE OR REPLACE VIEW public.latestcot AS
SELECT
    cot.id::BIGINT,
    cot.uid,
    cot.cot_type,
    cot.access,
    cot.qos,
    cot.opex,
    cot.start,
    cot."time",
    cot.stale,
    cot.how,
    cot.point_hae,
    cot.point_ce,
    cot.point_le,
    cot.detail,
    cot.servertime,
    cot.event_pt
FROM
    cot_router cot
        JOIN
    (
        SELECT
            cot_router.uid,
            max(cot_router.servertime) AS lastreceivetime
        FROM
            cot_router
        GROUP BY
            cot_router.uid
    ) groupedcot
    ON
        cot.uid::text = groupedcot.uid::text
    AND cot.servertime = groupedcot.lastreceivetime
ORDER BY
    cot.id;
