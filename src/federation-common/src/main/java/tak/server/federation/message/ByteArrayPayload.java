package tak.server.federation.message;


import java.nio.charset.Charset;
import java.util.Arrays;

/*
 * Simple payload implementation that wraps a byte array.
 * 
 * For this implementation, the parameterized type of this class is the same as the type of getBytes() -- this would not usually be the case, the content type 
 * is expected to be a POJO. https://en.wikipedia.org/wiki/Plain_Old_Java_Object
 * 
 * 
 */
public class ByteArrayPayload implements Payload<byte[]> {
	
	// The contents of the message
	private byte[] bytes;

	public ByteArrayPayload(byte[] bytes) {
		
		if (bytes == null) {
			this.bytes = new byte[0];
		} else {
			this.bytes = Arrays.copyOf(bytes, bytes.length);
		}
	}
	
	public ByteArrayPayload() {
		this.bytes = new byte[0];
	}

	@Override
	public byte[] getBytes() {
		return Arrays.copyOf(bytes, bytes.length);
	}

	@Override
	public void setBytes(byte[] bytes) {

		if (bytes == null) {
			this.bytes = new byte[0];
		} else {
			this.bytes = Arrays.copyOf(bytes, bytes.length);
		}
	}

	@Override
	public byte[] getContent() {
		return getBytes();
	}

	@Override
	public void setContent(byte[] content) {
		setBytes(content);
	}

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(30);
        builder.append("ByteArrayPayload - contents: ").append(new String(bytes, Charset.forName("UTF-8")));
        return builder.toString();
    }
}
