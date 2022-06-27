

package com.bbn.marti.util;

/**
* An immutable tuple, with static instantiation/type inferral method for avoiding duplicate type signatures in constructor assignment
*
* Not hostile to nulls
 *
*/
public class Tuple<V1,V2> {
	private final V1 left;
	private final V2 right;

	public Tuple(V1 left, V2 right) {
		this.left = left;
		this.right = right;
	}
	
	public V1 left() {
		return left;
	}
	
	public V2 right() {
		return right;
	}

	public static <V1,V2> Tuple<V1,V2> create(V1 leftVal, V2 rightVal) {
		return new Tuple<V1,V2>(leftVal, rightVal);
	}
}