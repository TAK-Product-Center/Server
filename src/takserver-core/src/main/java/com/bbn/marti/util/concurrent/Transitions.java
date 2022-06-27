

package com.bbn.marti.util.concurrent;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.bbn.marti.util.Assertion;

public class Transitions {
    /**
    * Repeatedly tries to transition the state of the given atomic reference to dest, 
    * until the transition is successfully completed, or until the given untilSet
    * contains the atomic reference's state.
    *
    * Returns the state observed prior to either a successful transition or a member
    * of untilSet
    * 
    * @note the caller should take care to all but prove convergence
    */
    public static <E extends Enum<E>> E doUntilSetTransition(AtomicReference<E> state, E dest, Set<E> untilSet) {
        Assertion.areNotNull(state, dest, untilSet);
        
        E current;
    
        do {
            current = state.get();
        } while (!untilSet.contains(current)
            && !state.compareAndSet(current, dest));

        return current;
    }

    /**
    * Repeatedly tries to transition the state of the given atomic reference to dest,
    * until the transition is successfully completed, or the given whileSet no longer
    * contains the atomic reference's state.
    *
    * Returns the state observed prior to either a successful transition or a member
    * of the complement of whileSet
    * 
    * @note the caller should take care to all but prove convergence, as the opportunities for infinite
    * looping abound
    */
    public static <E extends Enum<E>> E doWhileSetTransition(AtomicReference<E> state, E dest, Set<E> whileSet) {
        Assertion.areNotNull(state, dest, whileSet);

        E current;
    
        do {
            current = state.get();
        } while (whileSet.contains(current)
            && !state.compareAndSet(current, dest));

        return current;
    }
}
