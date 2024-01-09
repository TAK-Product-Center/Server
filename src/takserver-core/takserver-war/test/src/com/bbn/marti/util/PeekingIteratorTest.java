package com.bbn.marti.util;

import java.util.Arrays;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.*;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Assert;

public class PeekingIteratorTest {
	public List<Integer> input;
	public PeekingIterator<Integer> iter;

	public void initIteratorWith(Integer... args) {
		iter = new PeekingIterator<Integer>(Arrays.asList(args).iterator());
	}

	@Test
	public void emptyIteratorShouldHaveFalseHasNext() {
		initIteratorWith();
		Assert.assertFalse("Should be false", iter.hasNext());
	}

	@Test
	public void emptyIteratorShouldHaveFalseHasNextRepeated() {
		initIteratorWith();
		for (int i=0; i<6; i++) {
			Assert.assertFalse("Should be false", iter.hasNext());
		}
	}

	@Test(expected=NoSuchElementException.class)
	public void emptyIteratorShouldBeEmptyPeek() {
		initIteratorWith();
		iter.peek();
	}

	@Test
	public void emptyIteratorShouldBeEmptyPeekRepeated() {
		initIteratorWith();
		
		for (int i=0; i<6; i++) {
			try {
				iter.peek();
				Assert.fail();
			} catch (NoSuchElementException e) { continue; }
		}
	}	

	@Test(expected=NoSuchElementException.class)
	public void emptyIteratorShouldBeEmptyNext() {
		initIteratorWith();
		iter.next();
	}

	@Test
	public void emptyIteratorShouldBeEmptyNextRepeated() {
		initIteratorWith();
		
		for (int i=0; i<6; i++) {
			try {
				iter.next();
				Assert.fail();
			} catch (NoSuchElementException e) { continue; }
		}
	}		

	@Test
	public void repeatedPeekShouldNotAdvance() {
		initIteratorWith(0,1,2,3,4);
		
		for (int i=0; i<6; i++) {
			Assert.assertSame("Should be zero every time", 0, iter.peek());
		}
	}
	
	@Test
	public void midstreamRepeatedPeekShouldNotAdvance() {
		initIteratorWith(0,1,2,3,4);

		iter.next();
		iter.next();
		iter.next();

		for (int i=0; i<5; i++) {
			Assert.assertSame("Should stay at 3", 3, iter.peek());
		}
	}

	@Test(expected=UnsupportedOperationException.class)
	public void removeShouldThrowException() {
		initIteratorWith(0,1,2,3,4);

		iter.remove();
	}
}