package tak.server.federation.hub.plugin;

import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class FederationHubPluginMetadata implements Serializable {
    @JsonProperty
    private String name;
    @JsonProperty
    private String startCommand;
    @JsonProperty
    private String version;
    @JsonProperty
    private String description;
    @JsonProperty
    private String author;
    @JsonProperty
    private FederationHubPluginType type;
    @JsonProperty
    private String startTime;
    @JsonProperty
    private long startTimeMillis;
    
    private FederationHubPluginMetadata(String name, String description, String author, String version) {
        Instant now = Instant.now();
        this.startTime = DateTimeFormatter.ISO_INSTANT.format(now);
        this.startTimeMillis = now.toEpochMilli();
        this.name = Objects.requireNonNull(name);
        this.version = Objects.requireNonNull(version);
        this.description = description;
        this.author = author;
    }

    // Public factory method
    public static FederationHubPluginMetadata create(String name, String description, String author, String version) {
        return new FederationHubPluginMetadata(name, description, author, version);
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStartCommand() {
		return startCommand;
	}

	public void setStartCommand(String startCommand) {
		this.startCommand = startCommand;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public FederationHubPluginType getType() {
		return type;
	}

	public void setType(FederationHubPluginType type) {
		this.type = type;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public long getStartTimeMillis() {
		return startTimeMillis;
	}

	public void setStartTimeMillis(long startTimeMillis) {
		this.startTimeMillis = startTimeMillis;
	}

	@Override
	public String toString() {
		return "FederationHubPluginMetadata [name=" + name + ", startCommand=" + startCommand + ", version=" + version
				+ ", description=" + description + ", author=" + author + ", type=" + type + ", startTime=" + startTime
				+ ", startTimeMillis=" + startTimeMillis + "]";
	}
}
