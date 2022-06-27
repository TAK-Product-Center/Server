ALTER TABLE public.mission ADD COLUMN parent_mission_id bigint;

ALTER TABLE public.mission
  ADD CONSTRAINT parent_mission_fkey FOREIGN KEY (parent_mission_id)
      REFERENCES public.mission (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION;