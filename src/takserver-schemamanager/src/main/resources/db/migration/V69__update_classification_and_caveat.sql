
ALTER TABLE ONLY classification ADD CONSTRAINT classification_level_unique UNIQUE (level);
ALTER TABLE ONLY caveat ADD CONSTRAINT caveat_name_unique UNIQUE (name);
