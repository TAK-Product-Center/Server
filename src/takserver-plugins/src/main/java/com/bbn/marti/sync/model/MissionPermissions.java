
package com.bbn.marti.sync.model;

import java.util.Set;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

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


