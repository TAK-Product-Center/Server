package com.bbn.marti.sync.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.oxm.Marshaller;


@XmlRootElement(name = "MissionChanges")
@XmlAccessorType(XmlAccessType.FIELD)
public class MissionChanges {

    protected static final Logger logger = LoggerFactory.getLogger(MissionChanges.class);

    @XmlElement(name="MissionChange")
    private List<MissionChange> missionChanges = new ArrayList<MissionChange>();

    public List<MissionChange> getMissionChanges() {
        return missionChanges;
    }

    public void setMissionChanges(List<MissionChange> missionChanges) {
        this.missionChanges = missionChanges;
    }

    public void add(MissionChange missionChange) {
        missionChanges.add(missionChange);
    }

	@Override
	public String toString() {
		return "MissionChanges [missionChanges=" + missionChanges + "]";
	}
}


