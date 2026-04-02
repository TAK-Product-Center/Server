package com.bbn.marti.sync.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import com.bbn.marti.maplayer.model.MapLayer;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Entity
@Table(name = "mission_layer")
@Cacheable
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@XmlRootElement()
public class MissionLayer {

    private static final Logger logger = LoggerFactory.getLogger(MissionLayer.class);

    public enum Type {
        GROUP,
        UID,
        CONTENTS,
        MAPLAYER,
        ITEM
    }


    protected String uid;
    protected String name;
    protected Type type;
    protected String after;

    protected MissionLayer prev;
    protected MissionLayer parent;
    protected List<MissionLayer> children = new CopyOnWriteArrayList<>();

    protected List<Mission.MissionAdd<String>> uidAdds = new ArrayList<>();
    protected List<Mission.MissionAdd<Resource>> resourceAdds = new ArrayList<>();
    protected List<Mission.MissionAdd<MapLayer>> maplayerAdds = new ArrayList<>();

    public MissionLayer() {}

    public MissionLayer(MissionLayer copy) {
        uid = copy.uid;
        name = copy.name;
        type = copy.type;
        after = copy.after;
        parent = copy.parent;
    }

    public MissionLayer(String uid) {
        if (uid == null) {
            uid = UUID.randomUUID().toString();
        }
        this.uid = uid;
    }

    @Id
    @Column(name = "uid", unique = true, nullable = false)
    @JsonProperty("uid")
    @XmlElement(name = "uid")
    public String getUid() {
        return uid;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }

    @Column(name = "name", nullable = true, columnDefinition = "TEXT")
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "type", nullable = false, columnDefinition = "LONG")
    public Type getType() {
        return type;
    }
    public void setType(Type type) {
        this.type = type;
    }

    @JsonIgnore
    @Column(name = "after", nullable = true, columnDefinition = "TEXT")
    public String getAfter() {
        return after;
    }
    public void setAfter(String after) {
        this.after = after;
    }

    @Transient
    @XmlElement
    public String getParentUid() {
        return parent == null ? null : parent.getUid();
    }

    @JsonIgnore
    @XmlTransient
    @ManyToOne(cascade={CascadeType.ALL}, fetch = FetchType.LAZY)
    @JoinColumn(name="parent_node_uid")
    public MissionLayer getParent() { return parent; }
    public void setParent(MissionLayer parent) { this.parent = parent; }

    @XmlTransient
    @JsonProperty("mission_layers")
    @OneToMany(mappedBy="parent", fetch = FetchType.LAZY)
    public List<MissionLayer> getChildren() { return children; }
    public void setChildren(List<MissionLayer> children) { this.children = children; }

    @Transient
    @JsonProperty("uids")
    public List<Mission.MissionAdd<String>> getUidAdds() {
        return uidAdds;
    }

    public void setUidAdds(List<Mission.MissionAdd<String>> uidAdds) {
        this.uidAdds = uidAdds;
    }

    @Transient
    @JsonProperty("contents")
    public List<Mission.MissionAdd<Resource>> getResourceAdds() {
        return resourceAdds;
    }

    public void setResourceAdds(List<Mission.MissionAdd<Resource>> resourceAdds) {
        this.resourceAdds = resourceAdds;
    }

    @Transient
    @JsonProperty("maplayers")
    public List<Mission.MissionAdd<MapLayer>> getMaplayerAdds() {
        return maplayerAdds;
    }

    public void setMaplayerAdds(List<Mission.MissionAdd<MapLayer>> maplayerAdds) {
        this.maplayerAdds = maplayerAdds;
    }

    @Transient
    @JsonIgnore
    @XmlTransient
    public String getAbsolutePath() {
        String basePath = getParent() != null ? getParent().getAbsolutePath() : "";
        return basePath + "/" + getName();
    }

    private static MissionLayer getPrevious(MissionLayer data, List<MissionLayer> layers) {

        if (data.prev != null) {
            return data.prev;
        }

        for (MissionLayer t : layers) {
            if (t.getUid().equals(data.getAfter())) {
                data.prev = t;
                return t;
            }
        }
        return null;
    }

    private static boolean hasCycle(String missionName, MissionLayer check, List<MissionLayer> layers) {
        MissionLayer temp = check;
        Set<MissionLayer> processed = new HashSet<MissionLayer>();
        processed.add(temp);
        while ((temp = getPrevious(temp, layers)) != null) {
            if (processed.contains(temp)) {
                logger.error("found cycle for " + temp.getUid() + " in " + missionName);
                return true;
            }
            processed.add(temp);
        }
        return false;
    }

    //
    // Sorts a list of layer objects
    //
    private static List<MissionLayer> sort(String missionName, List<MissionLayer> unsorted) {

        // make sure that we dont have two items pointing to the same after
        Set<String> afters = new HashSet<>();
        Set<String> uids = new HashSet<>();
        for (MissionLayer t : unsorted) {
            if (afters.contains(t.getAfter())) {
                logger.error("found duplicate after " + t.getAfter() + " in " + missionName);
                return unsorted;
            } else if (uids.contains(t.getUid())) {
                logger.error("found duplicate uid " + t.getUid() + " in " + missionName);
                return unsorted;
            } else if (hasCycle(missionName, t, unsorted)) {
                return unsorted;
            }
            afters.add(t.getAfter());
            uids.add(t.getUid());
        }

        afters.remove(null);
        Set<String> difference = Sets.difference(afters, uids);
        if (difference.size() != 0) {
            for (String after : difference) {
                if (logger.isDebugEnabled()) {
                    logger.debug("found unknown after " + after + " in " + missionName);
                }
            }
            return unsorted;
        }

        List<MissionLayer> sorted = new LinkedList<MissionLayer>(unsorted);
        boolean moved;
        do {
            moved = false;
            for (int outer = 0; outer < sorted.size() && !moved; outer++) {
                for (int inner = 0; inner < sorted.size() && !moved; inner++) {
                    if (inner == outer) {
                        continue;
                    }

                    // is inner after outer?
                    if (sorted.get(inner).getAfter() != null && sorted.get(inner).getAfter().equals(
                            sorted.get(outer).getUid())) {
                        // do the indices check out
                        if (inner != outer + 1) {
                            if (outer + 1 == sorted.size()) {
                                // move inner to the end
                                if (logger.isDebugEnabled()) {
                                    logger.debug("moving " + sorted.get(inner).getUid() + " to the end");
                                }
                                sorted.add(sorted.remove(inner));
                            } else {
                                // swap inner with whatever is currently after outer
                                if (logger.isDebugEnabled()) {
                                    logger.debug("swapping " + sorted.get(inner).getUid()
                                            + " and " + sorted.get(outer + 1).getUid());
                                }
                                Collections.swap(sorted, inner, outer + 1);
                            }
                            moved = true;
                        }
                    }
                }
            }
        } while (moved);
        return sorted;
    }

    public static List<MissionLayer> sortMissionLayers(String missionName, List<MissionLayer> missionLayers) {
        // sort the list of mission layers
        List<MissionLayer> sorted = sort(missionName, missionLayers);

        // iterate over the sorted mission layers
        for (MissionLayer layer : sorted) {

            // sort child mission layers
            layer.setChildren(sortMissionLayers(missionName, layer.getChildren()));
        }
        return sorted;
    }
}




