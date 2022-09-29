CREATE OR REPLACE FUNCTION bitwiseAndGroups(groupVector character varying, groups bit varying) RETURNS boolean
    LANGUAGE plpgsql IMMUTABLE
    AS $$
BEGIN
	return groupVector::bit(32768) & 
		lpad(groups::character varying, 32768, '0')::bit(32768)::bit varying <> 
			0::bit(32768)::bit varying;
END;$$ RETURNS NULL ON NULL INPUT;