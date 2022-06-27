

package com.bbn.marti.util;

/**
* A mutable tuple, with static instantiation/type inferral method for avoiding duplicate type signatures in constructor assignment
*
* Not hostile to nulls
*
*/
public class Muple<V1,V2> {
	private V1 theLeft;
	private V2 theRight;

	public Muple(V1 left, V2 right) {
		left(left);
		right(right);
	}
	
	public V1 left() {
		return theLeft;
	}
	
	public V2 right() {
		return theRight;
	}

	public V1 left(V1 left) {
		V1 oldLeft = theLeft;
		theLeft = left;
		
		return oldLeft;
	}
	
	public V2 right(V2 right) {
		V2 oldRight = theRight;
		theRight = right;
		
		return oldRight;
	}

	public Tuple<V1,V2> tuple() {
		return Tuple.create(theLeft, theRight);
	}

	public static <V1,V2> Muple<V1,V2> create(V1 leftVal, V2 rightVal) {
		return new Muple<V1,V2>(leftVal, rightVal);
	}
}