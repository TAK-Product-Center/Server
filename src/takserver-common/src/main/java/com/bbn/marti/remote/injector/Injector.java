package com.bbn.marti.remote.injector;

import java.rmi.RemoteException;

/*
 * 
 * Generic message injector interface.
 * 
 */

public interface Injector<Context, Message> {
    
    /*
     * Process a message, using information obtained from the context, or return the original message if injection was not triggered by implementation logic.
     */
    Message process(Context context, Message message);
    
    /*
     * @return a name for this injector
     */
    String getName();
}
