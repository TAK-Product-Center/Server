

package com.bbn.marti.service.kml;

/*
 * 
 * Strategy pattern interface to assign icons
 * 
 */
@FunctionalInterface
public interface IconStrategy<E> {
    void assignIcon(E data);
}
