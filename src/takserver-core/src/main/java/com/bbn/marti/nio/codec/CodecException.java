

package com.bbn.marti.nio.codec;

/**
* An exception used for passing errors that emanate from
* a particular ByteCodec, which is stored
*/
public class CodecException extends Exception {
    private final ByteCodec source;
	private final String message;
	public CodecException(ByteCodec source, String message, Exception cause) {
		super(message, cause);
		
		this.source = source;
		this.message = message;
	}
	
	public final ByteCodec source() {
		return this.source;
	}
    
    public String getMessage() {
        return String.format("Source codec: %s message: %s", source, message);
    }
    
    public static CodecException convert(Exception cause, ByteCodec source) {
        return new CodecException(source, cause.getMessage(), cause);
    }
}