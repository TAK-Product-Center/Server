package com.bbn.marti.remote.sync;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// local value class just to keep lists (JSON arrays) of hashes / UIDs
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class MissionContent {

    public MissionContent() { }
    
    private final List<String> hashes = new ArrayList<>();
    private final List<String> uids = new ArrayList<>();
    private Map<String, List<MissionContent>> paths;
    private String after;

    public List<String> getHashes() {
        return hashes;
    }
    
    public List<String> getUids() {
        return uids;
    }

    public Map<String, List<MissionContent>> getPaths() {
        return paths;
    }

    @JsonIgnore
    public Map<String, List<MissionContent>> getOrCreatePaths() {
        if (paths == null) {
            paths = new HashMap<>();
        }
        return paths;
    }

    public String getAfter() {
        return after;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    @Override
    public String toString() {
        return "MissionContent [hashes=" + hashes + ", uids=" + uids + ", paths=" + paths + ", after=" + after + "]";
    }
}