package tak.server.federation.message;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A wrapper that enables Avro serialization by explicitly capturing the element type of a list
 *
 * @param <T> The type of the elements of the list
 */
@SuppressWarnings("PMD.TooManyMethods")
public class ListWrapper<T> implements List<T>{

	private final List<T> list;
	private final String elementType;

	public ListWrapper(String className, List<T> list) {
		if (list == null) {
			throw new NullPointerException("List wrapper doesn't take a null list");
		}
		this.list = list;
		this.elementType = className;
	}

	@Override
	public int size() {
		return list.size();
	}
	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}
	@Override
	public boolean contains(Object object) {
		return list.contains(object);
	}
	@Override
	public Iterator<T> iterator() {
		return list.iterator();
	}
	@Override
	public Object[] toArray() {
		return list.toArray();
	}
	@Override
	public <T> T[] toArray(T[] array) {
		return list.toArray(array);
	}
	@Override
	public boolean add(T element) {
		return list.add(element);
	}
	@Override
	public boolean remove(Object object) {
		return list.remove(object);
	}
	@Override
	public boolean containsAll(Collection<?> collection) {
		return list.containsAll(collection);
	}
	@Override
	public boolean addAll(Collection<? extends T> collection) {
		 return list.addAll(collection);
	}
	@Override
	public boolean addAll(int index, Collection<? extends T> collection) {
		return list.addAll(index, collection);
	}
	@Override
	public boolean removeAll(Collection<?> collection) {
		return list.removeAll(collection);
	}
	@Override
	public boolean retainAll(Collection<?> collection) {
		return list.retainAll(collection);
	}
	@Override
	public void clear() {
		list.clear();
		
	}
	@Override
	public T get(int index) {
		return list.get(index);
	}
	@Override
	public T set(int index, T element) {
		return list.set(index, element);
	}
	@Override
	public void add(int index, T element) {
		list.add(index, element);
		
	}
	@Override
	public T remove(int index) {
		return list.remove(index);
	}
	@Override
	public int indexOf(Object object) {
		return list.indexOf(object);
	}
	@Override
	public int lastIndexOf(Object object) {
		return list.lastIndexOf(object);
	}
	@Override
	public ListIterator<T> listIterator() {
		return list.listIterator();
	}
	@Override
	public ListIterator<T> listIterator(int index) {
		return list.listIterator(index);
	}
	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		return list.subList(fromIndex, toIndex);
	}
	public List<T> getList() {
		return list;
	}
	public String getElementType() {
		return elementType;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {return true;}
		if (object == null || getClass() != object.getClass()) {return false;}

		ListWrapper<?> that = (ListWrapper<?>) object;

		if (!list.equals(that.list)) {return false;}
		return elementType.equals(that.elementType);

	}

	@Override
	public int hashCode() {
		int result = list.hashCode();
		result = 31 * result + elementType.hashCode();
		return result;
	}

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(35);
        builder.append("ListWrapper [list=").append(list).append(", elementType=").append(elementType).append(']');
        return builder.toString();
    }
}
