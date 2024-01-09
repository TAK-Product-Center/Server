

package tak.server.util;

import java.io.Serializable;

/**
* An association for pairing a key and value, where equality and hashing are based only on the key.
*/
public class Association<K,V> implements Serializable {
	
	private static final long serialVersionUID = -58274876345L;
	
	protected K theKey;
	protected V theValue;

	public Association(K key, V value) {
		if (key == null) throw new IllegalArgumentException("Key can't be null");
		theKey = key;
		theValue = value;
	}
	
	public K getKey() {
		return theKey;
	}
	
	public V getValue() {
		return theValue;
	}

	public V setValue(V value) {
		V oldValue = theValue;
		theValue = value;
		return oldValue;
	}
	
	@Override
	public boolean equals(Object other) {
		if (this != other) {
			if (other != null && other instanceof Association) {
				Association that = (Association) other;
				return getKey().equals(that.getKey());
			} else return false;
		} else return true;
	}
	
	@Override
	public int hashCode() {
		return getKey().hashCode();
	}
	
	@Override
	public String toString() {
		return String.format("Association(%s -> %s)", getKey().toString(), getValue().toString());
	}
}