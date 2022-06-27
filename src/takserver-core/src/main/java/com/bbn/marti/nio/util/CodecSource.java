

package com.bbn.marti.nio.util;

import java.util.List;

import com.bbn.marti.nio.codec.ByteCodecFactory;
import com.bbn.marti.nio.codec.Codec;

/**
* An interface that allows for customization of the 
* codec factories used in different pipelines
* 
* Intended for allowing different ssl configurations
* (drawing on different trust stores) to be swapped
* in
*/
public interface CodecSource {
    /**
    * Returns a server factory for the given codec
    */
    public ByteCodecFactory serverFactory(Codec codec);

    /**
    * Returns a client factory for the given codec
    */
    public ByteCodecFactory clientFactory(Codec codec);
    
    /*
     * get a list of codecs to bind
     */
    public List<Codec> getCodecs();
}