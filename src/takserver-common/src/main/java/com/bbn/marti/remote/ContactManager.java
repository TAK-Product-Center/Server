

package com.bbn.marti.remote;

import java.util.Collection;

public interface ContactManager {
	Collection<RemoteFile> getFileList();
	Collection<RemoteContact> getContactList();
    void updateContact(RemoteContact contact);
    void addContact(RemoteContact contact);
    void removeContact(String uid);
}
