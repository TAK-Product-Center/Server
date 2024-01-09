

package com.bbn.marti.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.lang.UnsupportedOperationException;

/**
* A peeking iterator for looking at the element that will be returned by next(), without actually advancing the iterator.
* <p>
* Wraps an existing iterator.
*/
public class PeekingIterator<E> implements Iterator<E> {
  protected Iterator<E> theIter;
  protected E head; 		 // current element, will be returned by peek/next
  protected boolean valid; // lookahead flag that indicates whether the iterator's internal head refers to a valid element
  
  public PeekingIterator(Iterator<E> iterator) {
    theIter = iterator;
    advance();
  }

  protected Iterator<E> getIter() {
    return theIter;
  }

  /**
  * Returns whether the iterator has another element (ie, whether a call to next or peek will be exceptionless)
  */
  public boolean hasNext() {
    return valid;
  }
  
  /**
  * Returns the next element in the iterator, and advances it by one position.
  * <p>
  * @throws NoSuchElementException If called any time after hasNext returned false.
  */
  public E next() {
    if (hasNext()) {
      // can return element held by peek
      E result = peek();
      
      // advance iterator
      advance();

      return result;
    } else {
      throw new NoSuchElementException("No more elements left");
    }
  }
  
  /**
  * Returns the element that will be returned by a call to next, but does not modify the state of the iterator.
  * <p> 
  * @throws NoSuchElementException If called any time after hasNext returned false.
  */
  public E peek() {
    if (hasNext())
      // return head
      return head;
    else 
      throw new NoSuchElementException("No more elements left");
  }
  
  /**
  * Do not call this.
  * <p>
  * @throws UnsupportedOperationException If you call it.
  */
  public void remove() {
  	throw new UnsupportedOperationException("Who in their right mind would have remove be part of an iterator");
  }

  protected void advance() {
    // pull the element out of the front of the iterator, if it exists, or set the lookahead flag to false
    if (getIter().hasNext()) {
      head = getIter().next();
      valid = true;
    } else {
      valid = false;
    }
  }
}
