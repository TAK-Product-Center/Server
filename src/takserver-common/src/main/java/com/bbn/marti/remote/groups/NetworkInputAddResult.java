

package com.bbn.marti.remote.groups;

import java.io.Serializable;

/**
 * Created on 10/23/15.
 */
public enum NetworkInputAddResult implements Serializable {
    SUCCESS("Input added successfully."),
    FAIL_TCP_PORT_ALREADY_IN_USE("ERROR! The specified TCP port is already in use!"),
    FAIL_UDP_PORT_ALREADY_IN_USE("ERROR! The specified UDP port is already in use!"),
    FAIL_MCAST_PORT_ALREADY_IN_USE("ERROR! THe specified multicast port is already in use!"),
    FAIL_MCAST_GROUP_UNSET("ERROR! Cannot add a multicast input without a group!"),
    FAIL_LDAP_AUTH_NOT_ENABLED("ERROR! Cannot add an input with LDAP authentication if LDAP authentication is not enabled!"),
    FAIL_FILE_AUTH_NOT_ENABLED("ERROR! Cannot add an input with file authentication if file authentication is not enabled!"),
    FAIL_INPUT_NAME_EXISTS("ERROR! Input name already exists!");

    private final String displayMessage;

    public final String getDisplayMessage() {
        return displayMessage;
    }

    NetworkInputAddResult(String displayMessage) {
        this.displayMessage = displayMessage;
    }
}
