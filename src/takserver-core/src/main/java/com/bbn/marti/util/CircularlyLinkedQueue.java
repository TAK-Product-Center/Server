

package com.bbn.marti.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
* A CircularlyLinkedQueue implementation for supporting fast and potentially constant-time, zero-copy appends to other
* CircularlyLinkedQueues.
*
* A circularly linked queue is essentially a singly linked list, except that a pointer to the tail element is
* kept instead of a head reference. The last element is circularly linked to the head of the list, allowing for constant
* time reads from the head, and constant time writes to the tail.
*
* The last element is called the tail, and refers to the most recently inserted element, and can be
* addressed by logical index (size() - 1). If the list is empty, the tail field is null, and the size is 0. This
* creates a separate case which must be addressed when an element is added to an empty queue, and when the last element of
* the queue is removed.
*
* Internally, this queue relies on the Node structure to store the parameterized type and a pointer to the next element.
* The NodeIterator internal class allows for contiguous iteration over the chain of nodes - it returns a Node with each next,
* and advances an internal Node pointer to the next Node. It starts pointing at the head.
*
* The NodeValueIterator is a shallow wrapper around the NodeIterator class, and simply returns the value in each returned Node.
*
* Neither Iterator supports concurrent modification, but are not written to explicitly fail if this occurs.
*
* This queue was written (in particular) to support a fast drainTo operation: this method takes another CircularlyLinkedQueue, and an optional
* size limit, and splices at most limit nodes into the given queue. This is done without instantiating a new Node structure for
* each element (ie, zero-copy). If the size limit is larger than the size of this queue, the entire list can be spliced in constant time.
* Otherwise, limit nodes are selected in time proportional to limit (specifically, the time it takes to iterate across limit nodes), and spliced in.
* The number of elements actually spliced is returned.
*
*/
public class CircularlyLinkedQueue<E> implements List<E> {
	public int size;     // number of elements in the queue
	public Node<E> tail; // pointer to queue tail. Is null when size is 0. Otherwise, tail.next points to the queue head

	public static class Node<E> {
		public E val;
		public Node<E> next;

		public Node(E in) {
			val = in;
		}

		public E getValue() {
			return val;
		}
	}

	/**
	* Iterates over the number of nodes in the queue at instantiation, starting with the head node.
	*/
	public static class NodeIterator<E> implements Iterator<Node<E>> {
		Node<E> finger;
		int count;

		public NodeIterator(CircularlyLinkedQueue<E> queue) {
			count = queue.size();
			if (count > 0) {
				finger = queue.tail;
			} else {
				finger = null;
			}
		}

		public boolean hasNext() {
			return count > 0;
		}

		public Node<E> next() {
			if (hasNext()) {
				finger = finger.next;
				count--;
				return finger;
			} else throw new NoSuchElementException("No more elements left in the iterator");
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	* A shallow wrapper around the NodeIterator, and returns the value contained in each returned Node structure.
	*/
	public static class NodeValueIterator<E> implements Iterator<E> {
		NodeIterator<E> iter;

		public NodeValueIterator(CircularlyLinkedQueue<E> queue) {
			iter = new NodeIterator<E>(queue);
		}

		public boolean hasNext() {
			return iter.hasNext();
		}

		public E next() {
			// underlying iterator takes care of NoSuchElementException w/internal hasNext
			return iter.next().val;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public CircularlyLinkedQueue() {
		reset();
	}

	public CircularlyLinkedQueue(Collection<? extends E> coll) {
		this.tail = buildTail(coll);
		this.size = coll.size();
	}

	/**
	* Effectively reinitializes the queue
	*/
	public void reset() {
		size = 0;
		tail = null;
	}

	/**
	* Returns the current value of the head, if the size is nonzero. Otherwise, returns null.
	*/
	public E peek() {
		if (size() > 0) {
			return tail.next.val; // if size is one, tail.next points at itself
		} else {
			return null;
		}
	}

	/**
	* Returns and removes the head of the queue, if the size is nonzero. Otherwise, returns null.
	*/
	public E poll() {
		if (size() > 0) {
			return remove(0);
		} else {
			return null;
		}
	}

	public int size() {
		return size;
	}

	/**
	*	This method hooks the tails of two circularly linked queues together, such that the
	* 	tail of the second queue becomes the tail of the new queue.
	*
	*   Given
	*		Queue 1: 	0 -{@literal >} 1 -{@literal >} .... -{@literal >} n (first tail)
	*					^				  |
	*					|	{@literal <}-	{@literal <}- {@literal <}-	  V
	*
	*		Queue 2: 	0 -{@literal >} 1 -{@literal >} .... -{@literal >} (second tail)
	*					^				  |
	*					|	{@literal <}-	{@literal <}- {@literal <}-    V
	*
	*
	*	The new queue is
	* 		        	0 -{@literal >} 1 -{@literal >} .... -{@literal >} n (first tail) -{@literal >} 0 -{@literal >} 1 -{@literal >} .... -{@literal >} n (second tail)
	*					^													  |
	*					|	{@literal <}-	{@literal <}-	{@literal <}-	{@literal <}-	{@literal <}-	{@literal <}-	{@literal <}-	{@literal <}-	{@literal <}-	{@literal <}-  {@literal <}-  {@literal <}-	  V
	*
	*/
	public static <E> void hookTails(Node<E> firstTail, Node<E> secondTail) {
		Node<E> head = firstTail.next;
		firstTail.next = secondTail.next;
		secondTail.next = head;
	}

	/**
	* Builds a loop of nodes from the iterator from the given Collection, and returns the circularly linked
	* tail. Throws an exception if the collection's size() method does not return the size observed by iterator depletion.
	*
	* Because the size field is critical to this queue, we double check it.
	*/
	public static <E> Node<E> buildTail(Collection<? extends E> coll) {
		if (coll.isEmpty()) return null;

		// keep track of size for sanity checking, throw error if inaccurate
		int reportedSize = coll.size();
		Iterator<? extends E> iter = coll.iterator();
		Node<E> head = new Node<E>(iter.next());
		Node<E> finger = head;
		reportedSize--;

		while(iter.hasNext()) {
			Node<E> newFinger = new Node<E>(iter.next());
			finger.next = newFinger;
			finger = newFinger;
			reportedSize--;
		}

		if (reportedSize != 0) {
			throw new IllegalStateException("Given collection misreported size");
		}

		finger.next = head;
		return finger;
	}

	/**
	* A constant-time, append all operation that hooks this entire queue onto the end of the given one.
	*
	* If this queue is empty, nothing is done. If their queue is empty, our tail node is copied
	* into their tail field. Otherwise (both nonempty), both loops are linked together such that
	* their tail points to our head, and our tail points to their head.
	*
	* The size of this queue is returned.
	*/
	public int drainTo(CircularlyLinkedQueue<E> in) {
		// append the contents of this circularly linked queue to the given one
		if (size() == 0) return 0; // nothing to do

		// POST: our tail is valid
		if (in.size() > 0) {
			// have a valid tail to target
			hookTails(in.tail, this.tail);
		}

		in.tail = this.tail;

		// save my size for return value, increment their size
		int numAdded = size();
		in.size += numAdded;

		reset();

		return numAdded;
	}

	/**
	* A batched append operation that hooks at most maxAdd nodes onto the end of the given queue.
	*
	* If maxAdd is at least as large as this queue, we append the whole queue with the other drainTo method.
	* Otherwise, we select maxAdd Nodes, unhook them from our queue, and link them circularly. If the target
	* queue is empty, this loop becomes their new queue. Otherwise, we hook our queue fragment onto the end of theirs.
	*/
	public int drainTo(CircularlyLinkedQueue<E> in, int maxAdd) {
		if (maxAdd == 0) return 0;
		else if (maxAdd < 0) throw new IllegalArgumentException("Value to add is negative");

		// POST: maxAdd is positive
		if (maxAdd < size()) {
			// POST: size is at least 2 : we can safely batch a remove from this queue without moving the tail
			// select maxAdd nodes, link them into the given structure
			Node<E> finger = tail;
			Node<E> head = tail.next;

			for (int i = 0; i < maxAdd; i++) {
				finger = finger.next;
			}

			// POST: finger points to the new tail of in, finger.next points to our new head
			// close loop for this queue
			tail.next = finger.next;
			size -= maxAdd;

			// close loop for fragment
			finger.next = head;
			if (in.size() > 0) {
				// they have a tail -- hook in
				hookTails(in.tail, finger);
			}

			in.tail = finger;
			in.size += maxAdd;
			return maxAdd;
		} else {
			// maxAdd is at least as big as the size -- append the whole list
			return drainTo(in);
		}
	}

	/**
	* A location-preserving iterator across the values of this list.
	*/
	public Iterator<E> iterator() {
		return new NodeValueIterator<E>(this);
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public boolean contains(Object o) {
		return (indexOf(o) != -1);
	}

	@Override
	public Object[] toArray() {
		Object[] result = new Object[size()];
		int idx = 0;
		for (E elem : this) {
			result[idx++] = elem;
		}

		return result;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(Collection<?> coll) {
		for (Object elem : coll) {
			if (!this.contains(elem)) {
				return false;
			}
		}
		return true;
	}

	/**
	* Builds a circular queue from the given collection, and splices it into our queue.
	*
	* The size of the given collection is checked, and an exception is thrown if it misreports its size in iteration.
	*/
	@Override
	public boolean addAll(Collection<? extends E> coll) {
		Node<E> newTail = buildTail(coll);
		if (newTail == null) return false;

		if (size() > 0) {
			hookTails(tail, newTail);
		}
		tail = newTail;
		size += coll.size();

		return true;
	}

	/**
	* Inserts a straightline segment built from the given collection into our queue, at the given index.
	*/
	@Override
	public boolean addAll(int index, Collection<? extends E> coll) {
		if (index < 0 || index > size()) throw new IndexOutOfBoundsException();
		else if (index == size()) return addAll(coll); // defer to simple tail append

		// build new circular queue, returns tail
		Node<E> tempTail = buildTail(coll);
		if (tempTail == null) return false;

		// splice segment in
		Node<E> before = getNodeBefore(index);
		Node<E> after = before.next;

		before.next = tempTail.next;
		tempTail.next = after;

		size += coll.size();
		return true;
	}

	/**
	* Returns the node at the given index. Special-cased to return tail directly if the index is size - 1.
	* Otherwise, it relies internally on the getNodeBefore call, and calls next on that result.
	*/
	public Node<E> getNodeAt(int index) {
		// primary case, make fastest
		if (index >= 0 && index < size() - 1)
			return getNodeBefore(index).next;
		else if (index < 0 || index >= size())
			throw new IndexOutOfBoundsException();
		else // index == size - 1
			return tail;
	}

	/**
	* Instantiates an iterator and calls next index - 1 times to find the node before the given index.
	*
	* index == 0 is special cased to return the tail.
	*/
	public Node<E> getNodeBefore(int index) {
		if (index < 0 || index >= size()) throw new IndexOutOfBoundsException();
		else if (index == 0) return tail;

		// POST: index is at least 1
		NodeIterator<E> iter = new NodeIterator<E>(this);

		// advance the iterator index - 2 times
		while(--index > 0) {
			iter.next();
		}

		// advance the iterator one time
		return iter.next();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	/**
	* Tosses the tail pointer, resets the size
	*/
	@Override
	public void clear() {
		reset();
	}

	/**
	* Constant time access for head (0) and tail (size() - 1). All other indices take
	* time proportional to the given index.
	*/
	@Override
	public E get(int index) {
		return getNodeAt(index).val;
	}

	@Override
	public E set(int index, E element) {
		Node<E> node = getNodeAt(index);
		E oldVal = node.val;
		node.val = element;

		return oldVal;
	}

	/**
	* Appends the given element to the queue - adding to index size() (nonexistent until the end of the call)
	* is special cased for fast access.
	*/
	public boolean add(E inVal) {
		add(size(), inVal);
		return true;
	}

	/**
	* Constructs a new Node to hold element, and stores it at the given index. If index == size(), then we shift the
	* tail field to point to the new node. If size() was zero, we initialize the tail field.
	*/
	@Override
	public void add(int index, E element) {
		if (index < 0 || index > size()) throw new IndexOutOfBoundsException();

		Node<E> newNode = new Node<E>(element);
		if (index == size()) {
			// appending into the tail position
			if (size() > 0) {
				// have an existing tail, link to head
				newNode.next = tail.next;
				tail.next = newNode;
			} else {
				newNode.next = newNode;
			}

			tail = newNode;
		} else {
			// inserting somewhere into the list, without shifting the tail
			Node<E> before = getNodeBefore(index);
			newNode.next = before.next;
			before.next = newNode;
		}

		size++;
	}

	/**
	* Removes the node at the given index, and returns its contained value
	*
	* Finds the node before the index (see getNodeBefore(index)), and removes the node after it.
	* Removing index 0 is particularly fast, and removing the tail node shifts the tail field back one element.
	*
	* Element size() - 1 is the most expensive node to remove.
	*/
	@Override
	public E remove(int index) {
		Node<E> before = getNodeBefore(index);
		Node<E> target = before.next;
		// getNodeBefore takes care of bounds check internally
		// POST: size > 0

		if (size() > 1) {
			// don't need to void out tail ptr after removal
			before.next = target.next;
			if (index == size() - 1) {
				// removing tail, shift tail ptr back one
				tail = before;
			}
		} else {
			// removing the sole tail element
			tail = null;
		}

		size--;

		target.next = null; // void out pointer for iterator slightly faster fail.
		return target.val;
	}

	/**
	* Uses findNextDistanceTo with a new iterator to find the absolute distance to the first instance of the given object
	*
	* returns -1 if it cannot be found.
	*/
	@SuppressWarnings("unchecked")
	@Override
	public int indexOf(Object o) {
		E toFind = (E) o;
		return findNextDistanceTo(toFind, iterator()) - 1;
	}

	/**
	* Advances the iterator until the given element is found, or the iterator is depleted
	*
	* When depleted, 0 is returned. Otherwise, the count of how many iterator positions were clicked before the
	* element was found. Note that this means that the
	*/
	public int findNextDistanceTo(E elem, Iterator<E> iter) {
		int idx = 0;

		while (iter.hasNext()) {
			idx++;
			if (elem.equals(iter.next())) {
				return idx;
			}
		}

		return 0;
	}

	/**
	* This function uses a single iterator to seek (in a forwards direction) over all the instances of the given object in the list.
	* If the findNextDistanceTo function never reports an instance, then -1 is returned.
	*
	* Otherwise, the findNextDistanceTo function is called on the same iterator: each call returns the number of elements
	* it passed over before it found the given element. This count is summed into the travelled variable, which always holds the index
	* of the last instance found. Once no more instances can be found, we stop searching.
	*/
	@SuppressWarnings("unchecked")
	@Override
	public int lastIndexOf(Object o) {
		E toFind = (E) o;
		Iterator<E> iter = this.iterator();
		int travelled = -1, dist;

		// continually advance iterator
		while ((dist = findNextDistanceTo(toFind, iter)) > 0) {
			travelled += dist;
		}

		return travelled;
	}

	@Override
	public ListIterator<E> listIterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		// TODO Auto-generated method stub
		return null;
	}
}
