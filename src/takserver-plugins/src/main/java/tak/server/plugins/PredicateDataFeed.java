package tak.server.plugins;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class PredicateDataFeed implements Serializable {
	
	private static final long serialVersionUID = 7220421301147165722L;

	private String uuid;
	
	private String name;
	
	private String predicateLang;
	
	private URL dataSourceEndpoint;
	
	private String predicate;
	
	private String authType;
		
	private List<String> tags = new ArrayList<>();
	
	private boolean archive;

	private boolean sync;
	
	private List<String> filterGroups;
	
	private boolean federated;
	
	public PredicateDataFeed() { };
	
	public PredicateDataFeed(String uuid, String name, String predicateLang, URL dataSourceEndpoint, String predicate,
			String authType, List<String> tags, boolean archive, boolean sync, List<String> filterGroups,
			boolean federated) {
		super();
		this.uuid = uuid;
		this.name = name;
		this.predicateLang = predicateLang;
		this.dataSourceEndpoint = dataSourceEndpoint;
		this.predicate = predicate;
		this.authType = authType;
		this.tags = tags;
		this.archive = archive;
		this.sync = sync;
		this.filterGroups = filterGroups;
		this.federated = federated;
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
		if (tags == null) {
            tags = new ArrayList<String>();
        }
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
	
	public boolean isFederated() {
		return federated;
	}

	public void setFederated(boolean federated) {
		this.federated = federated;
	}

	public String getPredicateLang() {
		return predicateLang;
	}

	public void setPredicateLang(String predicateLang) {
		this.predicateLang = predicateLang;
	}

	public URL getDataSourceEndpoint() {
		return dataSourceEndpoint;
	}

	public void setDataSourceEndpoint(URL dataSourceEndpoint) {
		this.dataSourceEndpoint = dataSourceEndpoint;
	}

	public String getPredicate() {
		return predicate;
	}

	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}

	public String getAuthType() {
		return authType;
	}

	public void setAuthType(String authType) {
		this.authType = authType;
	}
	
	@Override
	public String toString() {
		return "PredicateDataFeed [uuid=" + uuid + ", name=" + name + ", predicateLang=" + predicateLang
				+ ", dataSourceEndpoint=" + dataSourceEndpoint + ", predicate=" + predicate + ", authType=" + authType
				+ ", tags=" + tags + ", archive=" + archive + ", sync=" + sync + ", filterGroups=" + filterGroups
				+ ", federated=" + federated + "]";
	}		
}
