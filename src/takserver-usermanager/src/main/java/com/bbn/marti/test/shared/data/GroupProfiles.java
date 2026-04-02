package com.bbn.marti.test.shared.data;

/**
 * Created on 2/5/16.
 */
public enum GroupProfiles {
	group0("0"),
	group1("1"),
	group2("2"),
	group3("3"),
	__ANON__("t");

	private final String identifier;

	GroupProfiles(String identifier) {
		this.identifier = identifier;
	}

	public String getIdentifier() {
		return identifier;
	}

	public String toString() {
		return name();
	}
}
