

package com.bbn.tak.schema;

import java.util.StringJoiner;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters
public class CommonOptions {

	// this field is parsed from the URL string. It is used mostly for logging
	String database;

	@Parameter(names={"-p", "-password"}, description = "password for database owner", password=true)
	String password;

	@Parameter(names={"-U", "-u", "-user"}, description = "user name of database owner")
	String username;

	@Parameter(names={"-url"}, description = "database url, example: jdbc:postgresql://127.0.0.1:5432/cot")
	String jdbcUrl;

	void setDatabase(String database) {
		this.database = database;
	}

	void setPassword(String password) {
		this.password = password;
	}

	void setUsername(String username) {
		this.username = username;
	}

	void setJdbcUrl(String url) {
		this.jdbcUrl = url;
	}


	@Override
	public String toString() {
		return new StringJoiner(", ", CommonOptions.class.getSimpleName() + "[", "]")
				.add("database='" + database + "'")
				.add("password='" + "******" + "'")
				.add("username='" + username + "'")
				.add("jdbcUrl='" + jdbcUrl + "'")
				.toString();
	}
}
