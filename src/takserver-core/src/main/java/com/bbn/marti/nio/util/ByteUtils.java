

package com.bbn.marti.nio.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import com.bbn.marti.util.Assertion;
import com.google.common.base.Charsets;

public class ByteUtils {
	private static final Charset charset = Charsets.UTF_8;
	private static final ByteBuffer emptyReadBuffer;
	private static final ByteBuffer emptyWriteBuffer;

	static {
		ByteBuffer buffer = ByteBuffer.allocate(0);
		((Buffer)buffer).flip();
		emptyReadBuffer = buffer.asReadOnlyBuffer();

		Assertion.post(!emptyReadBuffer.hasRemaining());
	}
	
	static {
        ByteBuffer buffer = ByteBuffer.allocate(0);
        ((Buffer)buffer).flip();
        emptyWriteBuffer = buffer.asReadOnlyBuffer();
    }
	
	public static ByteBuffer getEmptyReadBuffer() {
		return emptyReadBuffer;
	}
	
	public static ByteBuffer getEmptyWriteBuffer() {
        return emptyWriteBuffer;
    }
	
	public static byte[] trimArray(byte[] src, int offset, int len) {
		if (offset == 0 && len == src.length) {
			return src;
		} else {
			byte[] dest = new byte[len];
			System.arraycopy(src, offset, dest, 0, len);
			return dest;
		}
	}

	public static ByteBuffer copy(ByteBuffer src) {
		if (src.hasRemaining()) {
			ByteBuffer dest = ByteBuffer.allocate(src.remaining());

			((Buffer)(dest.put(src)))
				.flip();

			return dest;
		} else {
			return getEmptyReadBuffer();
		}
	}
	
	/**
	* Concatenates two byte buffers together
	* 
	* Assumes the buffers are in "read" mode. Returns a buffer, also in read mode.
    *
    * Mutates the state of the two, given buffers, such that all bytes are consumed
	*/
	public static ByteBuffer concat(ByteBuffer left, ByteBuffer right) {
		if (left.remaining() == 0) return right;
		else if (right.remaining() == 0) return left;

		int total = left.remaining() + right.remaining();
		ByteBuffer concat = ByteBuffer.allocate(total);
		
		((Buffer)concat.put(left).put(right)).flip();

		return concat;
	}
	
	public static ByteBuffer concat(ByteBuffer... buffers) {
		int total = 0;
		for (int i=0; i<buffers.length; i++) {
			total += buffers[i].remaining();
		}
		
		ByteBuffer concat = ByteBuffer.allocate(total);
		for (int i=0; i<buffers.length; i++) {
			concat.put(buffers[i]);
		}

		((Buffer)concat).flip();
		
		return concat;
	}
	
    /**
    * Returns the concatenation of all the buffers contained
    * in the given list, leaving    
    */
	public static ByteBuffer concat(List<ByteBuffer> buffers) {
        int count = buffers.size();
        if (count == 0) return getEmptyReadBuffer();
        else if (count == 1) return buffers.get(0);
    
		int total = bufferTotal(buffers);

		if (total == 0) return getEmptyReadBuffer();
			
		ByteBuffer dest = ByteBuffer.allocate(total);
		for (ByteBuffer src : buffers) {
			dest.put(src);
		}
		
		((Buffer)dest).flip();
		
		return dest;
	}
    
    /**
    * Returns whether the given iterable contains only one element
    *
    * (Some implementations of concurrent queues do not maintain 
    * a size field)
    */
    public static boolean isSingleton(Iterable<ByteBuffer> buffers) {
        Iterator iter = buffers.iterator();
        
        if (iter.hasNext()) {
            iter.next();
            return !iter.hasNext();
        } else {
            return false;
        }
    }
	
	/**
	* Concatenates the queue of byte buffers into a single byte buffer
	*
	* Mutates the queue, such that all copied buffers are removed from the queue
	*/
	public static ByteBuffer concatQueue(Queue<ByteBuffer> buffers) {
        // no data to speak of
        if (buffers.isEmpty()) return getEmptyReadBuffer();
        else if (isSingleton(buffers)) return buffers.poll();
        
        // otherwise, count the number of bytes
		int total = bufferTotal(buffers);

        // no bytes -- queue full of empty buffers
		if (total == 0) return getEmptyReadBuffer();
        
        // > 0 bytes -- alloc destination buffer, move all data over
		ByteBuffer dest = ByteBuffer.allocate(total);
		ByteBuffer src;
		
		while ((src = buffers.peek()) != null
			&& (total -= src.remaining()) >= 0) {
			// invariant: 
			// -- have a nonnull buffer at queue head and in src
			// -- have enough space to store buffer into dest
			buffers.poll();
			dest.put(src);
		}

		((Buffer)dest).flip();

        Assertion.post(dest.remaining() == dest.capacity());
                        		
		return dest;
	}
	
    public static ByteBuffer concatQueue(Queue<ByteBuffer> buffers, ByteBuffer tail) {
        if (buffers.isEmpty()) return tail;
        else if (isSingleton(buffers)) return concat(buffers.poll(), tail);
        else if (!tail.hasRemaining()) return concatQueue(buffers);
        
        int total = bufferTotal(buffers) + tail.remaining();

        // no data to speak of -- return an empty buffer
        if (total == 0) return getEmptyReadBuffer();
        
        // have meaningful data -- allocate space, copy over 
        ByteBuffer dest = ByteBuffer.allocate(total);
        ByteBuffer src;
        
        while ((src = buffers.peek()) != null
            && (total -= src.remaining()) >= 0) {
            // invariant: 
            // -- have a nonnull buffer at queue head and in src
            // -- have enough space to store buffer into dest
            buffers.poll();
            dest.put(src);
        }

        // append the tail data
        dest.put(tail);

        // flip the buffer over
        ((Buffer)dest).flip();

        //Assertion.post(total == 0);
        Assertion.post(dest.remaining() == dest.capacity());
                                
        return dest;
    }

	public static ByteBuffer enbuffer(byte[] src) {
		return enbuffer(src, 0, src.length);
	}
	
	/**
	* puts the given array/offset/len tuple into a byte buffer
	*
	* The buffer is flipped before it is returned, and is in "read" mode
	*/
	public static ByteBuffer enbuffer(byte[] src, int offset, int len) {
		ByteBuffer buffer = ByteBuffer.allocate(len);
		
		buffer.put(src, offset, len);
		((Buffer)buffer).flip();
		
		return buffer;
	}
	
	public static ByteBuffer enbuffer(List<byte[]> arrays) {
		int sum = arrayTotal(arrays);
		
		if (sum == 0) return emptyReadBuffer;
		
		ByteBuffer dst = ByteBuffer.allocate(sum);
		
		for (byte[] src : arrays) {
			dst.put(src);
		}
		
		((Buffer)dst).flip();
		
		return dst;
	}
	
	public static int bufferTotal(Iterable<ByteBuffer> iter) {
		int sum = 0;

		for (ByteBuffer buffer : iter) {
			sum += buffer.remaining();
		}
		
		return sum;
	}
	
	public static int arrayTotal(Iterable<byte[]> iter) {
		int sum = 0;
		for (byte[] array : iter) {
			sum += array.length;
		}

		return sum;
	}
	
	/**
	* Transfers the data contained in the buffer into a perfectly sized array
	* 
	* Assumes that the buffer is in "read mode". Modifies the position as a result
	* of the transfers.
	*/
	public static byte[] enarray(ByteBuffer buffer) {
		byte[] data = new byte[buffer.remaining()];
		buffer.get(data);
		
		return data;
	}
	
	public static CharBuffer decode(ByteBuffer buffer) {
		return charset.decode(buffer);
	}
	
	public static ByteBuffer encode(String string) {
		return charset.encode(string);
	}
	
	public static String getString(ByteBuffer buffer) {
        final byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return new String(bytes);
	}
	
	public static String getStringAndReposition(ByteBuffer buffer) {
        final byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes, 0, buffer.remaining());
        return new String(bytes);
    }
}