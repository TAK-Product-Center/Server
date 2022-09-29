package com.bbn.marti.remote.groups;

import java.io.Serializable;
import java.util.Comparator;

/**
 * An enum used to contain all the possible results from manipulating an input or static subscription.
 * <p/>
 * If the provided change matches the current state, a SUCCESS is expected to be returned.
 * <p/>
 * Created on 2/26/16.
 */

public class ConnectionModifyResult implements Comparator<ConnectionModifyResult>, Serializable {

    private String displayMessage;
    private int httpStatusCode;

    public final String getDisplayMessage() {
        return displayMessage;
    }

    public final int getHttpStatusCode() {
        return httpStatusCode;
    }

    public ConnectionModifyResult(String displayMessage, int httpStatusCode) {
        this.displayMessage = displayMessage;
        this.httpStatusCode = httpStatusCode;
    }

    @Override
    public int compare(ConnectionModifyResult o1, ConnectionModifyResult o2) {
        if (o1 == null && o2 == null) {
            return 0;

        } else if (o1 == null) {
            return -1;


        } else if (o2 == null) {
            return 1;

        } else {
            if (o1.getDisplayMessage().equals(o2.getDisplayMessage())) {
                return 0;
            }

        }
        return 0;
    }

    public static final ConnectionModifyResult SUCCESS = new ConnectionModifyResult("Connection updated successfully.", 200);
    public static final ConnectionModifyResult FAIL_NONEXISTENT = new ConnectionModifyResult("ERROR! The specified connection does not exist!", 400);
    public static final ConnectionModifyResult FAIL_NOMOD_NAME = new ConnectionModifyResult("ERROR! The connection name cannot be modified after initial creation!", 400);
    public static final ConnectionModifyResult FAIL_NOMOD_AUTH_TYPE = new ConnectionModifyResult("ERROR! The connection authentication type cannot be modified after initial creation!", 400);
    public static final ConnectionModifyResult FAIL_NOMOD_PROTOCOL = new ConnectionModifyResult("ERROR! The connection protocol cannot be modified after initial creation!", 400);
    public static final ConnectionModifyResult FAIL_NOMOD_PORT = new ConnectionModifyResult("ERROR! The connection port cannot be modified after initial creation!", 400);
    public static final ConnectionModifyResult FAIL_NOMOD_GROUP = new ConnectionModifyResult("ERROR! The connection multicast group cannot be modified after initial creation!", 400);
    public static final ConnectionModifyResult FAIL_NOMOD_IFACE = new ConnectionModifyResult("ERROR! The connection interface cannot be modified after initial creation!", 400);
    public static final ConnectionModifyResult FAIL_NOMOD_DFEED = new ConnectionModifyResult("ERROR! The connection found was not a data feed but contained data feed attributes!", 400);
}
