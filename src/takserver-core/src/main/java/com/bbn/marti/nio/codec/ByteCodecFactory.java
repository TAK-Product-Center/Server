

package com.bbn.marti.nio.codec;

import com.bbn.marti.util.concurrent.executor.OrderedExecutor;

/*
* An interface for a byte codec constructor
*
* @note the returned ByteCodec cannot be null--a null ByteCodec will generate a RuntimeException
* during BytePipeline construction
*/
public interface ByteCodecFactory {
    
    /**
    * Constructs a codec that uses the given pipeline context
    * as a view for communicating with the surrounding pipeline
    */
	ByteCodec buildCodec(PipelineContext ctx);

    /**
    * May be called by the pipeline when construction of 
    * a new codec occurs, allowing a codec to specify
    * an underlying executor that can/should be used 
    * to call into the codec
    *
    * Allows for specific threads to be dedicated to 
    * specific tasks (such as ssl)
    *
    * If the codec does not have such an execution
    * model, then it can return null, and the pipeline
    * will default to using its own executor.
    */
    OrderedExecutor codecExecutor();
}