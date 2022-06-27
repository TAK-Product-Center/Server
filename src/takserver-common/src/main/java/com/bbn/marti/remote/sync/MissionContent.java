package com.bbn.marti.remote.sync;

import java.util.ArrayList;
import java.util.List;

// local value class just to keep lists (JSON arrays) of hashes / UIDs
public final class MissionContent {
    
    public MissionContent() { }
    
    private final List<String> hashes = new ArrayList<>();
    private final List<String> uids = new ArrayList<>();
    
    public List<String> getHashes() {
        return hashes;
    }
    
    public List<String> getUids() {
        return uids;
    }

    @Override
    public String toString() {
        return "MissionContent [hashes=" + hashes + ", uids=" + uids + "]";
    }
}