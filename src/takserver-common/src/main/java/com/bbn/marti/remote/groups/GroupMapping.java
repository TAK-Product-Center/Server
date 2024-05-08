package com.bbn.marti.remote.groups;

import java.io.Serializable;

public class GroupMapping implements Serializable {

    private static final long serialVersionUID = 2580226646710074521L;
    private String remoteSourceGroup;
    private String localInboundGroup;

    public GroupMapping(String remoteSourceGroup, String localInboundGroup) {
        this.remoteSourceGroup = remoteSourceGroup;
        this.localInboundGroup = localInboundGroup;
    }

    public String getRemoteSourceGroup() {
        return remoteSourceGroup;
    }

    public void setRemoteSourceGroup(String remoteSourceGroup) {
        this.remoteSourceGroup = remoteSourceGroup;
    }

    public String getLocalInboundGroup() {
        return localInboundGroup;
    }

    public void setLocalInboundGroup(String localInboundGroup) {
        this.localInboundGroup = localInboundGroup;
    }

    @Override
    public String toString() {
        return "GroupMapping{" +
                "remoteSourceGroup='" + remoteSourceGroup + '\'' +
                ", localInboundGroup='" + localInboundGroup + '\'' +
                '}';
    }
}