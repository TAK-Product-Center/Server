package tak.server.plugins;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class PluginDataFeed implements Serializable{
	
	private static final long serialVersionUID = 1336926585144753864L;

	private String uuid;
	
	private String name;
		
	private List<String> tags = new ArrayList<>();
	
	private boolean archive;

	private boolean sync;
	
	private List<String> filterGroups;

	public PluginDataFeed(String uuid, String name, List<String> tags, boolean archive, boolean sync, List<String> filterGroups) {
		this.uuid = uuid;
		this.name = name;
		this.tags.addAll(tags);
		this.archive = archive;
		this.sync = sync;
		this.filterGroups = filterGroups;
	}
	
	public PluginDataFeed(String uuid, String name, List<String> tags, boolean archive, boolean sync) {
		this(uuid, name, tags, archive, sync, new ArrayList<String>());
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}
	
	public boolean isArchive() {
		return archive;
	}

	public void setArchive(boolean archive) {
		this.archive = archive;
	}

	public boolean isSync() {
		return sync;
	}

	public void setSync(boolean sync) {
		this.sync = sync;
	}

	public List<String> getFilterGroups() {
		return filterGroups;
	}
	
	public void setFilterGroups(List<String> filterGroups) {
		this.filterGroups = filterGroups;
	}
	
	@Override
	public String toString() {
		return "PluginDataFeed [uuid=" + uuid + ", name=" + name + ", tags=" + tags + ", archive=" + archive + ", sync="
				+ sync + "]";
	}

}
