

package com.bbn.marti.nio.codec;

import java.nio.ByteBuffer;

/**
* An interface presented to a ByteCodec for allowing
* uniform communication betwen ByteCodecs and BytePipelines
*
* All mutating calls into the ByteCodecs must be sourced
* from a thread context controlled by the BytePipeline.
*
* Otherwise, awful things will happen.
*
* This interface allows the ByteCodec to schedule calls into
* it (and report exceptions to the Pipeline), allowing for 
* perfect synchronicicity.
*/
public interface PipelineContext {
    /**
    * Schedules a read/write (encode/decode) check for some
    * point in the future.
    *
    * May be accompanied by nonempty read/write
    * data
    */
	public void scheduleReadCheck();
	public void scheduleWriteCheck();

    /**
    * Allows the codec to push the given data 
    * on to the next codec, in the given 
    * direction
    */
    public void scheduleRead(ByteBuffer buffer);
    public void scheduleWrite(ByteBuffer buffer);

    /**
    * Reports an exception that the ByteCodec experienced
    * during operation
    *
    * Expected to result in a pipeline shutdown
    */
	public void reportException(Exception e);
}