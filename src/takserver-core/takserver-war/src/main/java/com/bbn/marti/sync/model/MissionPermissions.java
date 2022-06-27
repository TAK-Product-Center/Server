
package com.bbn.marti.sync.model;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "permissions")
@XmlAccessorType(XmlAccessType.FIELD)
public class MissionPermissions {

    @XmlElement(name="permission")
    private Set<MissionPermission> missionPermissions;

    public MissionPermissions() { }

    public MissionPermissions(Set<MissionPermission> missionPermissions) {
        this.missionPermissions = missionPermissions;
    }
}


