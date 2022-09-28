package tak.server.plugins;

import java.util.UUID;

/*
 *
 * Value class containing metadata about a plugin.
 *
 */
public class PluginInfo {

	private String name;
	private String description;
	private String className;
	private String version;
	private String tag;
	private UUID id;
	boolean isSender;
	boolean isReceiver;
	boolean isInterceptor;
	private boolean isEnabled;
	private boolean isStarted;
	private String exceptionMessage;
	private boolean archiveEnabled;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	public boolean isSender() {
		return isSender;
	}
	public void setSender(boolean isSender) {
		this.isSender = isSender;
	}
	public boolean isReceiver() {
		return isReceiver;
	}
	public void setReceiver(boolean isReceiver) {
		this.isReceiver = isReceiver;
	}
	public String getExceptionMessage() {
	    return this.exceptionMessage;
	}
	public void setExceptionMessage(String exceptionMessage) {
	    this.exceptionMessage = exceptionMessage;
	}
	public boolean isEnabled() {
		return isEnabled;
	}
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}
	public boolean isStarted() {
		return isStarted;
	}
	public void setStarted(boolean isStarted) {
		this.isStarted = isStarted;
	}
	public boolean isInterceptor() {
		return isInterceptor;
	}
	public void setInterceptor(boolean isInterceptor) {
		this.isInterceptor = isInterceptor;
	}
	public boolean isArchiveEnabled() {
		return archiveEnabled;
	}
	public void setArchiveEnabled(boolean archiveEnabled) {
		this.archiveEnabled = archiveEnabled;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((className == null) ? 0 : className.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
            return true;
        }
		if (obj == null) {
            return false;
        }
		if (getClass() != obj.getClass()) {
            return false;
        }
		PluginInfo other = (PluginInfo) obj;
		if (className == null) {
			if (other.className != null) {
                return false;
            }
		} else if (!className.equals(other.className)) {
            return false;
        }
		if (id == null) {
			if (other.id != null) {
                return false;
            }
		} else if (!id.equals(other.id)) {
            return false;
        }
		if (name == null) {
			if (other.name != null) {
                return false;
            }
		} else if (!name.equals(other.name)) {
            return false;
        }
		return true;
	}

}
