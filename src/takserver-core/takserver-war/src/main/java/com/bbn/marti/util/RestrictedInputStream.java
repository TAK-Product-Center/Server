

package com.bbn.marti.util;

import java.io.InputStream;
import java.io.IOException;

/**
* An input stream wrapper that will throw an exception if more than maxBytes are read before the underlying input stream is depleted.
* Intended for verifying the size of input streams from the web into storage.
* 
* This stream wrapper passes all calls made to the underlying stream, while keeping track of how many bytes have been read.
* If a user can read more than the constructor argument "maxBytes" from the contained stream before it is depleted, then
* an IllegalStateException will be thrown.
*
* @note mark and reset are supported, but only if markSupported returns true. Behavior of mark and reset if more than mark(bytes)
* are read is especially indeterminate. This stream should never wrap another stream where mark has already been called
* and reset() is expected to function properly when called through this wrapper. Neither should mark and reset calls
* be made directly on the underlying stream.
*
* @note If an exception is thrown on a read call, the contents of the destination buffer should be considered arbitrary and meaningless.
*
*
* Internally, this class maintains a count (current) of the number of bytes that have been read. After each read call,
* the number of bytes read (assuming -1 was not returned) is added to current. If current exceeds bound, then an exception 
* is thrown.
*
* This class maintains a guard around each call with boolean flag, "valid". If close or shallowClose (which returns the contained
* input stream without closing it) are called, this flag is voided. In this state, all calls that expect a return argument
* throw an exception, and read calls return -1.
*/ 
public class RestrictedInputStream extends InputStream {
	private InputStream stream;
	public final long bound;
	private long current = 0L;
	private boolean valid = true;
	private long lastmark = -1;
	
	public RestrictedInputStream(InputStream in, long maxBytes) {
		if (maxBytes < 0) throw new IllegalArgumentException("maxBytes must be nonnegative");
		else if (in == null) throw new IllegalArgumentException("Input stream must be nonnull");
		
		stream = in;
		bound = maxBytes;
	}

	/**
	* Returns whether the stream is valid and has "toRead" bytes left before bound.
	*/		
	private boolean amValid(int toRead) {
		return valid && (current + (long) toRead <= bound);
	}

	private boolean amValid() {
		return valid && (current <= bound);
	}
	
	/**
	* Returns whether the stream was valid at the time of the call, and then invalidates the stream if it was valid.
	*/
	private boolean amAndUnsetValid() {
		boolean result = amValid();
		if (result) unsetValid();
		return result;
	}

	private void unsetValid() {
		valid = false;
	}
	
	private void add(int read) {
		this.add((long) read);
	}

	/**
	* adds the given number of bytes to the current counter, throwing an exception if the bound is exceeded in doing so.
	*/
	private void add(long read) {
		current += read;
		if (current > bound) throw new IllegalStateException("Read more than " + bound + " bytes.");
	}
	
	/**
	* Voids out this streams pointer to the input stream (hopefully encouraging garbage collection)
	*/
	private void dealloc() {
		this.stream = null;
	}
	
	@Override
	public int available() throws IOException {
		if (amValid()) {
			return stream.available();
		} else return 0;
	}

	/**
	* calls close on the input stream, voids out the input stream pointer, and unsets this input streams validity.
	*/		
	@Override
	public void close() throws IOException {
		if (amAndUnsetValid()) {
			stream.close();
			this.dealloc();
		}
	}
	
	/**
	* If valid, returns a pointer to the underlying stream and unsents this input stream's validity.
	*/
	public InputStream shallowClose() {
		if (amAndUnsetValid()) {
			InputStream in = stream;
			this.dealloc();
			return in;
		} else return null;
	}
	
	/**
	* Calls mark on the underlying stream if it claims to support mark. Sets the lastmark argument to the current # of 
	* bytes read. Lastmark is restored into current when reset is called, assuming mark was called.
	*/
	@Override
	public void mark(int readlimit) {
		if (amValid() && markSupported()) {
			stream.mark(readlimit);
			lastmark = current;
		} else throw new IllegalStateException();
	}
	
	@Override
	public boolean markSupported() {
		if (amValid()) {
			return stream.markSupported();
		} else throw new IllegalStateException();
	}
	
	@Override
	public int read() throws IOException {
		// hey, nobody ever said this method would be efficient
		byte[] single = new byte[1];
		int nread = this.read(single);
		if (nread != -1) {
			return single[0];
		} else return -1;
	}

	@Override
	public int read(byte[] buffer) throws IOException {
		return this.read(buffer, 0, buffer.length);
	}

	/**
	* calls the same read call on the underlying stream, returning the number of bytes read. If bound was exceeded with the 
	* read, then an exception is thrown.
	*/
	@Override
	public int read(byte[] buffer, int offset, int len) throws IOException {
		// guards - we're using our own buffer internally to read, so the underlying stream won't catch it
		int nread;
		if (amValid() && 
			(nread = stream.read(buffer, offset, len)) != -1) 
		{
			this.add(nread);
			return nread;
		} else return -1;
	}

	/**
	* Returns the number of bytes recorded as having been read so far by this stream.
	*/
	public long readCount() {
		if (amValid()) {
			return current;
		} else throw new IllegalStateException();
	}
	
	/**
	* Returns the number of bytes that a client can safely read without encountering an exception.
	*/
	public long bytesLeft() {
		if (amValid()) {
			return bound - current;
		} else throw new IllegalStateException();
	}

	/**
	* Calls reset on the underlying client, assuming that markSupported returns true, and mark was previously called on this stream.
	*/
	@Override
	public void reset() throws IOException {
		if (amValid() && markSupported() && lastmark != -1) {
			if (lastmark != -1) {
				stream.reset();
			}
		} else throw new IllegalStateException("Operation unsupported, or mark was never called.");
	}
	
	/**
	* Calls skip on the underyling stream. The count returned is added to the current count of bytes read.
	*/
	@Override
	public long skip(long toSkip) throws IOException {
		if (amValid()) {
			long skipped = stream.skip(toSkip);
			if (skipped != -1) {
				this.add(skipped);
			}
			return skipped;
		} else throw new IllegalStateException();
	}

	@Override
	public String toString() {
		return stream.toString();
	}
}