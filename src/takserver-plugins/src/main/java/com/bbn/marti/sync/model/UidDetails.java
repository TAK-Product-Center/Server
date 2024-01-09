package com.bbn.marti.sync.model;

import java.io.Serializable;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@XmlRootElement(name = "Details")
public class UidDetails implements Serializable {

	private static final long serialVersionUID = -2340896168483402689L;

	public UidDetails() { }

    @XmlAttribute(name = "type")
    public String type;

    @XmlAttribute(name = "callsign")
    public String callsign;

    @XmlAttribute(name = "title")
    public String title;

    @XmlAttribute(name = "iconsetPath")
    public String iconsetPath;

    @XmlAttribute(name = "color")
    public String color;

    @XmlElement(name = "attachment")
    public List<String> attachments;

    @XmlAttribute(name = "name")
    public String name;

    @XmlAttribute(name = "category")
    public String category;

	public Location location;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attachments == null) ? 0 : attachments.hashCode());
		result = prime * result + ((callsign == null) ? 0 : callsign.hashCode());
		result = prime * result + ((category == null) ? 0 : category.hashCode());
		result = prime * result + ((color == null) ? 0 : color.hashCode());
		result = prime * result + ((iconsetPath == null) ? 0 : iconsetPath.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((location == null) ? 0 : location.hashCode());
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
		UidDetails other = (UidDetails) obj;
		if (attachments == null) {
			if (other.attachments != null)
				return false;
		} else if (!attachments.equals(other.attachments))
			return false;
		if (callsign == null) {
			if (other.callsign != null)
				return false;
		} else if (!callsign.equals(other.callsign))
			return false;
		if (category == null) {
			if (other.category != null)
				return false;
		} else if (!category.equals(other.category))
			return false;
		if (color == null) {
			if (other.color != null)
				return false;
		} else if (!color.equals(other.color))
			return false;
		if (iconsetPath == null) {
			if (other.iconsetPath != null)
				return false;
		} else if (!iconsetPath.equals(other.iconsetPath))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (location == null) {
			if (other.location != null)
				return false;
		} else if (!location.equals(other.location))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "UidDetails [type=" + type + ", callsign=" + callsign + ", title=" + title + ", iconsetPath="
				+ iconsetPath + ", color=" + color + ", attachments=" + attachments + ", name=" + name + ", category="
				+ category + "]";
	}
}
