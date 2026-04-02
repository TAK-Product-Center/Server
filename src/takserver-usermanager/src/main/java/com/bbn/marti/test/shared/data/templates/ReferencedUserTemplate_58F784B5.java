package com.bbn.marti.test.shared.data.templates;

import com.bbn.marti.test.shared.data.generated.ImmutableUsers;
import org.jetbrains.annotations.NotNull;

/**
 * Created on 10/22/15.
 */
public enum ReferencedUserTemplate_58F784B5 {
	//////////////////////////
	// Begin Generated Users
	////////////////////////// 68EBC0C6-82C3-4C2F-9779-9191B5B432EC
	dummy_user("iShouldNotExist");
	////////////////////////// BD6A4745-76B4-42DD-AE35-5C2251DD6301
	// End Generated Users
	//////////////////////////


	private final String genUsersIdentifier;
	private ImmutableUsers user;

	ReferencedUserTemplate_58F784B5(@NotNull String genUsersIdentifier) {
		this.genUsersIdentifier = genUsersIdentifier;
	}

	public ImmutableUsers getUser() {
		if (user == null) {
			user = ImmutableUsers.valueOf(genUsersIdentifier);
		}
		return user;
	}

}
