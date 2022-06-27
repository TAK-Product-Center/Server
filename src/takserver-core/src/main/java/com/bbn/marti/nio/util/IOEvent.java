

package com.bbn.marti.nio.util;

import java.nio.channels.SelectionKey;
import java.util.EnumSet;
import java.util.Set;

import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.Assertion.AssertionException;
import com.google.common.base.Joiner;

/**
* An enumeration class for protecting Selection Key interest ops with a layer of type safety.
*
* The bit fields for OP_{Read,Write,Connect,Accept} are paired with an enumeration. Static and member methods are defined
* for modifying bit flags for each event enumeration. 
*/
public enum IOEvent {
    NONE    (0),
	READ    (SelectionKey.OP_READ),
	WRITE   (SelectionKey.OP_WRITE),
	CONNECT (SelectionKey.OP_CONNECT),
	ACCEPT  (SelectionKey.OP_ACCEPT);

	// single bit field for the selection key interest op associated with this event
	private final int flag;
	
	IOEvent(int flag) {
		this.flag = flag;
	}
    
    public int flag() {
        return this.flag;
    }

	/**
	* Adds the flag for the given enumeration to the given bit vector, returning the new bit vector
	*
	* If the given enumeration is already present, the output is the same as the input
	*/
	public static int addInterest(final int existingInterest, final int toAdd) {
        return existingInterest | toAdd;
	}
	
    public static int addInterest(final int existingInterest, final IOEvent toAdd) {
        return addInterest(existingInterest, toAdd.flag());
    }	

	/**
	* Removes the flag for the given enumeration from the given bit vector, returning the new bit vector
	*
	* IF the given enumeration is not present, the output is the same as the input
	*/
	public static int removeInterest(final int existingInterest, final int toRemove) {
        return existingInterest & ( ~toRemove );
	}
	
    public static int removeInterest(final int existingInterest, final IOEvent toRemove) {
        return removeInterest(existingInterest, toRemove.flag());
    }	
	
	public static boolean presentIn(final int interest, IOEvent test) {
		return ( (interest & test.flag()) != 0 );
	}

    /**
    * Returns the bit vector corresponding to the OR of 
    * the given set of IOEvents
    */
	public static int generateFlags(Set<IOEvent> events) {
		int flags = 0;
		
		for (IOEvent event : events) {
			flags |= event.flag();
		}
		
		return flags;
	}

    /**
    * The inverse of the generateFlags function. Removes all present
    * IOEvents, returning the Set of IOEvents present
    *
    * @throws AssertionException if there are bits in the given vector 
    * that don't correspond to a SelectionKey
    */
	public static Set<IOEvent> recoverFlags(int flags) {
        Set<IOEvent> events = EnumSet.noneOf(IOEvent.class);
		
		for (IOEvent event : IOEvent.values()) {
            if (presentIn(flags, event)) {
				events.add(event);
                flags = removeInterest(flags, event.flag());
			}
		}

        Assertion.post(flags == 0, "Bit vector contains unidentifiable crud");
		
        return events;
	}
    
    /**
    * Returns a boolean indicating whether the given bit vector 
    * represents a valid set of SelectionKeys
    *
    * (Removes all IOEvents, and checks to see if it's 0 at the end)
    */
    public static boolean validate(int flags) {
        for (IOEvent event : IOEvent.values()) {
            flags = removeInterest(flags, event.flag());
        }
        
        return (flags == 0);
    }

    public static String eventString(int flags) {
        return "{" + Joiner.on(", ").join(recoverFlags(flags)) + "}";
    }
}