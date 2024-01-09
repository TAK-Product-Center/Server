package com.bbn.marti.util;

import java.util.Arrays;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.*;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Assert;

public class RestrictedInputStreamTest {
	RestrictedInputStream stream;
	
	public void initIteratorWithLimit(int limit, int... args) {
		byte[] byte_args = toByteArray(args);
		stream = new RestrictedInputStream(new ByteArrayInputStream(byte_args), limit);
	}

	public void initIteratorWith(int... args) {
		byte[] byte_args = toByteArray(args);
		stream = new RestrictedInputStream(new ByteArrayInputStream(byte_args), args.length);
	}

	public byte[] toByteArray(int... args) {
		byte[] byte_args = new byte[args.length];
		
		for (int i=0; i<args.length; i++) {
			byte_args[i] = (byte) args[i];
		}

		return byte_args;
	}

	@Test
	public void emptyStreamShouldHaveNoneAvailable() {
		initIteratorWith();
		try {
			Assert.assertSame("Empty stream should have none available", 0, stream.available());
		} catch (IOException e) { Assert.fail(); }
	}

	@Test
	public void nonemptyStreamShouldHaveInputAvailable() {
		initIteratorWith(0,1,2,3,4,5,6);
		try {
			Assert.assertSame("Nonempty stream should have some available", 7, stream.available());
		} catch (IOException e) { Assert.fail(); }
	}

	@Test
	public void emptyStreamShouldHaveMarkSupported() {
		initIteratorWith();
		Assert.assertTrue("Mark should be supported", stream.markSupported());
	}

	@Test
	public void emptyStreamShouldHaveZeroCount() {
		initIteratorWith();
		Assert.assertSame("Should be zero -- no data fed in", 0L, stream.readCount());
	}

	@Test
	public void emptyStreamShouldHaveZeroBytesLeft() {
		initIteratorWith();
		Assert.assertSame("Should be zero -- no data fed in", 0L, stream.bytesLeft());
	}

	@Test(expected=IllegalStateException.class)
	public void emptyIteratorResetShouldThrow() {
		initIteratorWith();
		try {
			stream.reset();
		} catch (IOException e) { Assert.fail(); }
	}

	@Test
	public void emptyStreamMarkAndResetShouldBeFine() {
		initIteratorWith();
		stream.mark(0);
		
		try {	
			stream.reset();
		} catch (IOException e) { Assert.fail(); }

		Assert.assertSame("Should be none left still", 0L, stream.bytesLeft());
		Assert.assertSame("Should have read none", 0L, stream.readCount());
	}

	@Test
	public void emptyStreamShouldReturnNegSingleByte() {
		initIteratorWith();
		try {
			Assert.assertSame("Should be -1", -1, stream.read());
		} catch (IOException e) { Assert.fail(); }			
	}

	@Test
	public void emptyStreamShouldReturnNegByteArray() {
		initIteratorWith();
		try {
			Assert.assertSame("Should be -1", -1, stream.read(new byte[0]));
		} catch (IOException e) { Assert.fail(); }
	}

	@Test
	public void emptyStreamShouldReturnNegByteArray2() {
		initIteratorWith();
		try {
			Assert.assertSame("Should be -1", -1, stream.read(new byte[10]));
		} catch (IOException e) { Assert.fail(); }
	}

	@Test
	public void emptyStreamShouldReturnNegByteArrayLimit() {
		initIteratorWith();
		try {
			Assert.assertSame("Should be -1", -1, stream.read(new byte[0], 0, 0));
		} catch (IOException e) { Assert.fail(); }
	}

	@Test
	public void emptyStreamShouldReturnNegByteArrayLimit2() {
		initIteratorWith();
		try {
			Assert.assertSame("Should be -1", -1, stream.read(new byte[10], 0, 0));
		} catch (IOException e) { Assert.fail(); }			
	}	

	@Test
	public void readCountAndBytesReadShouldMirror() {
		initIteratorWith(0,1,2,3,4,5,6,7,8,9);

		for (int i=0; i<10; i++) {
			Assert.assertSame("Should have read i", (long) i, stream.readCount());
			Assert.assertSame("Should have 10 - i to go", (long) (10 - i), stream.bytesLeft());

			try {
				Assert.assertSame("Should read i at i", i, stream.read());
			} catch (IOException e) { Assert.fail(); }
		}

		Assert.assertSame("Should have read i", 10L, stream.readCount());
		Assert.assertSame("should have 0 to go", 0L, stream.bytesLeft());
	}

	@Test
	public void readCountAndBytesReadShouldMirrorGivenArray() {
		initIteratorWith(0,1,2,3,4,5,6,7,8,9);

		for (int i=0; i<10; i++) {
			Assert.assertSame("Should have read i", (long) i, stream.readCount());
			Assert.assertSame("Should have 10 - i to go", (long) (10 - i), stream.bytesLeft());

			try {
				byte[] array = new byte[1];
				Assert.assertSame("Should read 1 byte", 1, stream.read(array));
				Assert.assertSame("Should read i at i", (byte) i, array[0]);
			} catch (IOException e) { Assert.fail(); }
		}

		Assert.assertSame("Should have read i", 10L, stream.readCount());
		Assert.assertSame("should have 0 to go", 0L, stream.bytesLeft());
	}

	@Test
	public void readCountAndBytesReadShouldMirrorGivenArray2Step() {
		initIteratorWith(0,1,2,3,4,5,6,7,8,9);

		for (int i=0; i<10; i+=2) {
			Assert.assertSame("Should have read i", (long) i, stream.readCount());
			Assert.assertSame("Should have 10 - i to go", (long) (10 - i), stream.bytesLeft());

			try {
				byte[] array = new byte[2];
				Assert.assertSame("Should read 1 byte", 2, stream.read(array));
				Assert.assertSame("Should read i at i", (byte) i, array[0]);
				Assert.assertSame("Should read i+1 at i+1", (byte) (i + 1), array[1]);
			} catch (IOException e) { Assert.fail(); }
		}

		Assert.assertSame("Should have read i", 10L, stream.readCount());
		Assert.assertSame("should have 0 to go", 0L, stream.bytesLeft());
	}

	@Test
	public void readCountAndBytesReadShouldMirrorGivenBoundedArray() {
		initIteratorWith(0,1,2,3,4,5,6,7,8,9);

		for (int i=0; i<10; i++) {
			Assert.assertSame("Should have read i", (long) i, stream.readCount());
			Assert.assertSame("Should have 10 - i to go", (long) (10 - i), stream.bytesLeft());

			try {
				byte[] array = new byte[1];
				Assert.assertSame("Should read 1 byte", 1, stream.read(array, 0, 1));
				Assert.assertSame("Should read i at i", (byte) i, array[0]);
			} catch (IOException e) { Assert.fail(); }
		}

		Assert.assertSame("Should have read i", 10L, stream.readCount());
		Assert.assertSame("should have 0 to go", 0L, stream.bytesLeft());
	}

	@Test
	public void readCountAndBytesReadShouldMirrorGivenBoundedArray2Step() {
		initIteratorWith(0,1,2,3,4,5,6,7,8,9);

		for (int i=0; i<10; i+=2) {
			Assert.assertSame("Should have read i", (long) i, stream.readCount());
			Assert.assertSame("Should have 10 - i to go", (long) (10 - i), stream.bytesLeft());

			try {
				byte[] array = new byte[2];
				Assert.assertSame("Should read 2 bytes", 2, stream.read(array, 0, 2));
				Assert.assertSame("Should read i at i", (byte) i, array[0]);
				Assert.assertSame("Should read i+1 at i+1", (byte) (i + 1), array[1]);
			} catch (IOException e) { Assert.fail(); }
		}

		Assert.assertSame("Should have read i", 10L, stream.readCount());
		Assert.assertSame("should have 0 to go", 0L, stream.bytesLeft());
	}

	@Test(expected=IllegalStateException.class)
	public void singletonStreamWithZeroLimitShouldThrowOnRead() {
		stream = new RestrictedInputStream(new ByteArrayInputStream(new byte[]{0}), 0L);
		try {
			stream.read();
		} catch (IOException e) { Assert.fail(); }
	}

	@Test(expected=IllegalStateException.class)
	public void multiStreamWithZeroLimitShouldThrowOnRead() {
		initIteratorWithLimit(0, 0,1,2,3,4);
		try {
			stream.read();
		} catch (IOException e) { Assert.fail(); }
	}

	@Test(expected=IllegalStateException.class)
	public void multiStreamWithNonzeroLimitShouldThrowAtDepletionPoint() {
		initIteratorWithLimit(5, 0,1,2,3,4,5);

		try {
			for (int i=0; i<5; i++) {
				Assert.assertSame("Should read i", i, stream.read());
			}

			stream.read();
		} catch (IOException e) { Assert.fail(); }
	}

	@Test
	public void strobeIntoArrayWithLimitShouldWork() {
		initIteratorWith(0,1,2,3,4,5,6,7,8,9);

		try {
			byte[] args = new byte[10];
			
			Assert.assertSame("Should read 10", 10, stream.read(args));
			
			for (int i=0; i<10; i++) {
				Assert.assertSame("SHould read i at i", (byte) i, args[i]);
			}
		} catch (IOException e) {Assert.fail();}
	}

	@Test
	public void strobeIntoArrayWithAboveLimitShouldWork() {
		initIteratorWith(0,1,2,3,4,5,6,7,8,9);

		try {
			byte[] args = new byte[12];
			
			Assert.assertSame("SHould read 10", 10, stream.read(args));
			
			for (int i=0; i<10; i++) {
				Assert.assertSame("SHould read i at i", (byte) i, args[i]);
			}
		} catch (IOException e) {Assert.fail();}
	}

	@Test
	public void strobeIntoArrayWithAboveLimitShouldWorkTwoParts() {
		initIteratorWith(0,1,2,3,4,5,6,7,8,9);

		try {
			byte[] args = new byte[5];
			
			Assert.assertSame("SHould read 5", 5, stream.read(args));
			for (int i=0; i<5; i++) {
				Assert.assertSame("SHould read i at i", (byte) i, args[i]);
			}

			Assert.assertSame("Should read 5", 5, stream.read(args));
			for (int i=0; i<5; i++) {
				Assert.assertSame("should read i+5 at i", (byte) (i + 5), args[i]);
			}
		} catch (IOException e) {Assert.fail();}
	}

	@Test(expected=IllegalStateException.class)
	public void multiStreamStrobeShouldThrowAtDepletionPoint() {
		initIteratorWithLimit(9, 0,1,2,3,4,5,6,7,8,9);

		try {
			byte[] args = new byte[10];

			Assert.assertSame("should read 9", 9, stream.read(args, 0, 9));
			for (int i=0; i<9; i++) {
				Assert.assertSame("Should read i at i", (byte) i, args[i]);
			}

			stream.read();
		} catch (IOException e) { Assert.fail(); }
	}

	@Test(expected=IllegalStateException.class)
	public void multiStreamStrobeShouldThrowAtDepletionPoint2() {
		initIteratorWithLimit(9, 0,1,2,3,4,5,6,7,8,9);

		try {
			byte[] args = new byte[5];

			Assert.assertSame("should read 5", 5, stream.read(args));
			for (int i=0; i<5; i++) {
				Assert.assertSame("Should read i at i", (byte) i, args[i]);
			}

			stream.read(args);
		} catch (IOException e) { Assert.fail(); }
	}

	@Test(expected=IllegalStateException.class)
	public void multiStreamStrobeShouldThrowAtDepletionPoint3() {
		initIteratorWithLimit(5, 0,1,2,3,4,5,6,7,8,9);

		try {
			byte[] args = new byte[5];

			Assert.assertSame("should read 5", 5, stream.read(args));
			for (int i=0; i<5; i++) {
				Assert.assertSame("Should read i at i", (byte) i, args[i]);
			}

			stream.read(args);
		} catch (IOException e) { Assert.fail(); }
	}














}