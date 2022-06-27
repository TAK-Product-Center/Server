

package com.bbn.marti.nio.util;

import javax.net.ssl.SSLEngine;

/**
* An interface that allows for different ssl engines to be 
* provided to an ssl codec
*/
public interface SslSource {
    /**
    * Returns a server instance of the ssl engine
    */
    public SSLEngine buildServerEngine();
    
    /**
    * Returns a client instance of the ssl engine
    */
    public SSLEngine buildClientEngine();

    void refresh();
}