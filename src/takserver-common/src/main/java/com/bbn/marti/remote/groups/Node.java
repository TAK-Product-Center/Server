

package com.bbn.marti.remote.groups;

import java.util.Set;

/*
 * Node interface for group graphs
 * 
 * 
 */
public interface Node {

    Set<Node> getNeighbors();
    
    boolean isLeaf();
}
