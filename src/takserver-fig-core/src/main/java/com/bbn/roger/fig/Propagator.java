package com.bbn.roger.fig;

/*
 * 
 * Simple interface for passing state around generically
 * 
 */
public interface Propagator<State> {

    State propogate(State state);
    
}
