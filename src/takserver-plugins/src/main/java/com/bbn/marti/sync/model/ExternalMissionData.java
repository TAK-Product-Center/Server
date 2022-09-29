


package com.bbn.marti.sync.model;

import java.io.Serializable;
import javax.persistence.*;
import javax.xml.bind.annotation.XmlElement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.GenericGenerator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.ComparisonChain;

@Entity
@Table(name = "mission_external_data")
@Cacheable
@JsonInclude(Include.NON_NULL)
public class ExternalMissionData implements Serializable, Comparable<ExternalMissionData> {

    private static final long serialVersionUID = 68934987235979L;

    protected static final Logger logger = LoggerFactory.getLogger(ExternalMissionData.class);

    protected String id; // machine-generated string UUID
    protected String name;
    protected String tool;
    protected String urlData;
    protected String urlDisplay;
    protected String notes;
    protected Mission mission;

    // no-arg constructor
    public ExternalMissionData() {
        name = "";
        tool = "";
        urlData = "";
        urlDisplay = "";
        notes = "";
    }

    public ExternalMissionData(@NotNull String name, @NotNull String tool, @NotNull String urlData, @NotNull String urlDisplay, @NotNull String notes) {
        this.name = name;
        this.tool = tool;
        this.urlData = urlData;
        this.urlDisplay = urlDisplay;
        this.notes = notes;
    }

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @JsonProperty("uid")
    @XmlElement(name = "uid")
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    @Column(name = "name", nullable = false, columnDefinition = "TEXT")
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "tool", nullable = false, columnDefinition = "TEXT")
    public String getTool() { return tool; }
    public void setTool(String tool) {
        this.tool = tool;
    }

    @Column(name = "url_data", nullable = false, columnDefinition = "TEXT")
    public String getUrlData() { return urlData; }
    public void setUrlData(String urlData) { this.urlData = urlData; }

    @Column(name = "url_display", nullable = false, columnDefinition = "TEXT")
    @JsonProperty("urlView")
    @XmlElement(name = "urlView")
    public String getUrlDisplay() { return urlDisplay; }
    public void setUrlDisplay(String urlDisplay) { this.urlDisplay = urlDisplay; }

    @Column(name = "notes", nullable = false, columnDefinition = "TEXT")
    public String getNotes() { return notes; }
    public void setNotes(String notes) {
        this.notes = notes;
    }

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name="mission_id")
    public Mission getMission() { return mission; }
    public void setMission(Mission mission) { this.mission = mission; }


    @Override
    public int compareTo(ExternalMissionData that) {
        return ComparisonChain.start()
                .compare(this.id, that.id)
                .compare(this.name, that.name)
                .compare(this.tool, that.tool)
                .compare(this.urlData, that.urlData)
                .compare(this.urlDisplay, that.urlDisplay)
                .compare(this.notes, that.notes)
                .result();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((tool == null) ? 0 : tool.hashCode());
        result = prime * result + ((urlData == null) ? 0 : urlData.hashCode());
        result = prime * result + ((urlDisplay == null) ? 0 : urlDisplay.hashCode());
        result = prime * result + ((notes == null) ? 0 : notes.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ExternalMissionData other = (ExternalMissionData) obj;

        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;

        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;

        if (tool == null) {
            if (other.tool != null)
                return false;
        } else if (!tool.equals(other.tool))
            return false;

        if (urlData == null) {
            if (other.urlData != null)
                return false;
        } else if (!urlData.equals(other.urlData))
            return false;

        if (urlDisplay == null) {
            if (other.urlDisplay != null)
                return false;
        } else if (!urlDisplay.equals(other.urlDisplay))
            return false;

        if (notes == null) {
            if (other.notes != null)
                return false;
        } else if (!notes.equals(other.notes))
            return false;

        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ExternalMissionData [id=");
        builder.append(id);

        builder.append(", name=");
        builder.append(name);

        builder.append(", tool=");
        builder.append(tool);

        builder.append(", urlData=");
        builder.append(urlData);

        builder.append(", urlDisplay=");
        builder.append(urlDisplay);

        builder.append(", notes=");
        builder.append(notes);

        builder.append("]");
        return builder.toString();
    }
}