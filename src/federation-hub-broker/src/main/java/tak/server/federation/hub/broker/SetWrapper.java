package tak.server.federation.hub.broker;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A wrapper that enables Avro serialization by explicitly capturing the element type of a set
 *
 * @param <T> The type of the elements of the list
 */
@SuppressWarnings("PMD.TooManyMethods")
public class SetWrapper<T> implements Set<T>{

    private final Set<T> set;
    private final String elementType;

    public SetWrapper(String className, Set<T> set) {
        if (set == null) {
            throw new NullPointerException("Set wrapper doesn't take a null set");
        }
        this.set = (Set<T>) set;
        this.elementType = className;
    }

    @Override
    public int size() {
        return set.size();
    }
    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }
    @Override
    public boolean contains(Object object) {
        return set.contains(object);
    }
    @Override
    public Iterator<T> iterator() {
        return set.iterator();
    }
    @Override
    public Object[] toArray() {
        return set.toArray();
    }
    @Override
    public <T> T[] toArray(T[] array) {
        return set.toArray(array);
    }
    @Override
    public boolean add(T element) {
        return set.add(element);
    }
    @Override
    public boolean remove(Object object) {
        return set.remove(object);
    }
    @Override
    public boolean containsAll(Collection<?> collection) {
        return set.containsAll(collection);
    }
    @Override
    public boolean addAll(Collection<? extends T> collection) {
         return set.addAll(collection);
    }
    @Override
    public boolean removeAll(Collection<?> collection) {
        return set.removeAll(collection);
    }
    @Override
    public boolean retainAll(Collection<?> collection) {
        return set.retainAll(collection);
    }
    @Override
    public void clear() {
        set.clear();
    }

    public SetWrapper<T> deepCopy(){
        Set<T> copySet = new HashSet<>();
        for (T object : this.set){
            copySet.add(object);
        }

        return new SetWrapper<T>(this.elementType, copySet);

    }

    public String getElementType() {
        return elementType;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {return true;}
        if (object == null || getClass() != object.getClass()) {return false;}

        SetWrapper<?> that = (SetWrapper<?>) object;

        if (!set.equals(that.set)) {return false;}
        return elementType.equals(that.elementType);

    }

    @Override
    public int hashCode() {
        int result = set.hashCode();
        result = 31 * result + elementType.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(35);
        builder.append("SetWrapper [set=").append(set).append(", elementType=").append(elementType).append(']');
        return builder.toString();
    }
}
