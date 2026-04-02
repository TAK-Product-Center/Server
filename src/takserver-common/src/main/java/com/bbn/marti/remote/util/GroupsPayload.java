package com.bbn.marti.remote.util;

import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.Group.Type;
import com.bbn.marti.remote.groups.Direction;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupsPayload {

    @JsonProperty("schema")
    public String schema = "groups.v1";

    @JsonProperty("generated_at_utc")
    public String generatedAtUtc = isoDateTimeUTC(new Date());

    @JsonProperty("groups")
    public List<GroupDTO> groups;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GroupDTO {
        public String  name;
        public String  distinguishedName;
        public String  direction;
        public String  type;
        public Integer bitpos;
        public Boolean active;
        public String  description;
        public String  created;

        public Integer neighborCount;
    }


    private static final String DATE_ONLY = "yyyy-MM-dd";

    private static String fmtDateOnly(Date d) {
        if (d == null) return null;
        return new SimpleDateFormat(DATE_ONLY).format(d);
    }

    private static Date parseDateOnly(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return new SimpleDateFormat(DATE_ONLY).parse(s);
        } catch (ParseException e) {
            return null;
        }
    }

    private static String isoDateTimeUTC(Date d) {
        if (d == null) return null;
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        return fmt.format(d);
    }

    public static GroupsPayload fromNavigableSet(NavigableSet<Group> src) {
        GroupsPayload gp = new GroupsPayload();
        gp.groups = (src == null ? Collections.<Group>emptyNavigableSet() : src).stream()
                .map(GroupsPayload::toDTO)
                .collect(Collectors.toList());
        return gp;
    }

    private static GroupDTO toDTO(Group g) {
        GroupDTO d = new GroupDTO();
        d.name              = g.getName();
        d.distinguishedName = g.getDistinguishedName();
        d.direction         = g.getDirection() == null ? null : g.getDirection().name();
        d.type              = g.getType() == null ? null : g.getType().name();
        d.bitpos            = g.getBitpos();
        d.active            = g.getActive();
        d.description       = g.getDescription();
        d.created           = fmtDateOnly(g.getCreated());
        d.neighborCount     = (g.getNeighbors() == null) ? 0 : g.getNeighbors().size();
        return d;
    }

    public NavigableSet<Group> toNavigableSet() {
        return toNavigableSet(null);
    }

    public NavigableSet<Group> toNavigableSet(java.util.function.Function<GroupDTO, Group> builder) {
        NavigableSet<Group> out = new TreeSet<>();
        if (groups == null) return out;

        for (GroupDTO d : groups) {
            Group g = (builder != null) ? builder.apply(d) : defaultFromDTO(d);
            if (g != null) out.add(g);
        }
        return out;
    }

    private static Group defaultFromDTO(GroupDTO d) {
        Direction dir = null;
        try {
            dir = (d.direction == null) ? null : Direction.valueOf(d.direction);
        } catch (IllegalArgumentException ignored) {

        }

        if (dir == null) {
            return null;
        }

        Group g = new Group(d.name, dir);

        if (d.bitpos != null)      g.setBitpos(d.bitpos);
        if (d.type != null) {
            try { g.setType(Type.valueOf(d.type)); } catch (IllegalArgumentException ignored) {}
        }
        Date created = parseDateOnly(d.created);
        if (created != null)       g.setCreated(created);
        if (d.active != null)      g.setActive(d.active);
        if (d.description != null) g.setDescription(d.description);
        if (d.distinguishedName != null) g.setDistinguishedName(d.distinguishedName);

        return g;
    }
}