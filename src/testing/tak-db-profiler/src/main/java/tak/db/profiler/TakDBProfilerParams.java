package tak.db.profiler;

public class TakDBProfilerParams {
	
	private String username = "martiuser";
	private String password = "pass4marti";
	private String database = "cot";
	private String host = "127.0.0.1";
	private int port = 5432;
	private String configDir = "/opt/tak/db-utils/db-profiler";
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getDatabase() {
		return database;
	}
	public void setDatabase(String database) {
		this.database = database;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getConfigDir() {
		return configDir;
	}
	public void setConfigDir(String configDir) {
		this.configDir = configDir;
	}
	@Override
	public String toString() {
		return "TakDBProfilerParams [username=" + username + ", password=" + password + ", database=" + database
				+ ", host=" + host + ", port=" + port + ", configDir=" + configDir + "]";
	}
}
