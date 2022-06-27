package com.bbn.marti.util;

import java.util.Arrays;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.hamcrest.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.*;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class PeekingIteratorIOTest {
	public List<Integer> input;
	public PeekingIterator<Integer> iter;
	
	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{
				new Integer[]{}
			},
			{
				new Integer[]{0}
			},
			{
				new Integer[]{0,1}
			},
			{
				new Integer[]{0,1,2,3,4,5}
			},
			{
				new Integer[]{2,1,5,2,2134234,22132,-1,8,2}
			}
		});
	}

	public PeekingIteratorIOTest(Integer[] input) {
		this.input = Arrays.asList(input);
	}

	@Before
	public void buildIterator() {
		iter = new PeekingIterator<Integer>(input.iterator());
	}

	@Test
	public void inOrderTest() {
		for (int i=0; i<input.size(); i++) {
			Integer elemI = iter.peek();
			Assert.assertSame("Get should return first element", input.get(i), elemI);
			Assert.assertSame("Next should return same element", elemI, iter.next());
		}
	}

	@Test(expected=NoSuchElementException.class)
	public void peekDepletionTest() {
		for (int i=0; i<input.size(); i++) {
			iter.next();
		}
		iter.peek();
	}

	@Test(expected=NoSuchElementException.class)
	public void nextDepletionTest() {
		for (int i=0; i<input.size(); i++) {
			iter.next();
		}

		iter.next();
	}

	@Test
	public void falseDepletionTest() {
		for (int i=0; i<input.size(); i++) {
			iter.next();
		}

		Assert.assertFalse("Should be false after depleted", iter.hasNext());
	}
}