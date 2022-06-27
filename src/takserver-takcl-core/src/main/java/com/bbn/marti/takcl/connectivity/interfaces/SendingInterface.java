package com.bbn.marti.takcl.connectivity.interfaces;

import com.bbn.marti.test.shared.data.users.AbstractUser;
import org.dom4j.Document;
import org.jetbrains.annotations.NotNull;

/**
 * Created on 11/13/15.
 */
public interface SendingInterface {

	boolean sendMessage(Document doc);

	boolean sendMessageWithoutXmlDeclaration(@NotNull Document doc);

	void cleanup();

	AbstractUser getProfile();
}
