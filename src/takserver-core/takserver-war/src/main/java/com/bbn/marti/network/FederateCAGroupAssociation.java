package com.bbn.marti.network;

import java.io.Serializable;

import com.bbn.marti.network.FederationApi.DirectionValue;

public class FederateCAGroupAssociation implements Serializable {

    public FederateCAGroupAssociation() {}

    public FederateCAGroupAssociation(String caId, String group, DirectionValue direction){
        this.caId = caId;
        this.group = group;
        this.direction = direction;
    }

    private String caId;
    private String group;
    private DirectionValue direction;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getCaId() {
        return caId;
    }

    public void setCaId(String caId) {
        this.caId = caId;
    }

    public DirectionValue getDirection() {
        return direction;
    }

    public void setDirection(DirectionValue direction) {
        this.direction = direction;
    }
}
