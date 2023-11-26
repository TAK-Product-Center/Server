package com.bbn.marti.network;

import java.io.Serializable;

import com.bbn.marti.network.FederationApi.DirectionValue;

public class FederateCAHopsAssociation implements Serializable {

    public FederateCAHopsAssociation() {}

    public FederateCAHopsAssociation(String caId, int maxHops){
        this.caId = caId;
        this.maxHops = maxHops;
    }

    private String caId;
    private int maxHops;
    
	public String getCaId() {
		return caId;
	}

	public void setCaId(String caId) {
		this.caId = caId;
	}

	public int getMaxHops() {
		return maxHops;
	}

	public void setMaxHops(int maxHops) {
		this.maxHops = maxHops;
	}

	@Override
	public String toString() {
		return "FederateCAHopsAssociation [caId=" + caId + ", maxHops=" + maxHops + "]";
	} 
}
