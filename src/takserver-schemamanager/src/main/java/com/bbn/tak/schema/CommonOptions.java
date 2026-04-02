
package com.bbn.tak.schema;

import java.util.StringJoiner;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters
public class CommonOptions {

	// this field is parsed from the URL string. It is used mostly for logging
	String database;

	@Parameter(names = { "-p", "-password" }, description = "password for database owner", password = true)
	String password;

	@Parameter(names = { "-U", "-u", "-user" }, description = "user name of database owner")
	String username;

	@Parameter(names = { "-url" }, description = "database url, example: jdbc:postgresql://127.0.0.1:5432/cot")
	String jdbcUrl;
	
	// we can't use Boolean object for sslEnabled to track null status to determine if the argument was set or not because
	// jcommander will try to autobox Boolean object into a primitive therefore we have to track the set status manually
	private boolean sslExplicitlySet = false;

	@Parameter(names = { "-ssl" }, description = "use ssl instead of username + password")
	boolean sslEnabled = false;

	@Parameter(names = { "-sslMode" }, description = "require, verify-ca, verify-full")
	String sslMode;

	@Parameter(names = { "-sslCert" }, description = "client cert")
	String sslCert;

	@Parameter(names = { "-sslKey" }, description = "client key")
	String sslKey;

	@Parameter(names = {
			"-sslRootCert" }, description = "CA certificate that the client uses to verify the server’s certificate. (needed if using verify-ca or verify-full.)")
	String sslRootCert;

	public void setDatabase(String database) {
		this.database = database;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}

	public void setSslEnabled(boolean sslEnabled) {
	    this.sslEnabled = sslEnabled;
	}

	public void setSslExplicitlySet(boolean assigned) {
		this.sslExplicitlySet = assigned;
	}
	
	public boolean isSslExplicitlySet() {
	    return sslExplicitlySet;
	}

	public void setSslMode(String sslMode) {
		this.sslMode = sslMode;
	}

	public void setSslCert(String sslCert) {
		this.sslCert = sslCert;
	}

	public void setSslKey(String sslKey) {
		this.sslKey = sslKey;
	}

	public void setSslRootCert(String sslRootCert) {
		this.sslRootCert = sslRootCert;
	}

	@Override
	public String toString() {
	    return new StringJoiner(", ", CommonOptions.class.getSimpleName() + "[", "]")
	            .add("database='" + database + "'")
	            .add("password='" + "******" + "'")
	            .add("username='" + username + "'")
	            .add("jdbcUrl='" + jdbcUrl + "'")
	            .add("sslEnabled='" + String.valueOf(sslEnabled) + "'")
	            .add("sslMode='" + sslMode + "'")
	            .add("sslCert='" + sslCert + "'")
	            .add("sslKey='" + sslKey + "'")
	            .add("sslRootCert='" + sslRootCert + "'")
	            .toString();
	}
}
