

package com.bbn.marti.cot.search.model;

/*
 * 
 * model states of CoT search queries
 * 
 */
public enum CotSearchStatus {
    NEW,
    SUBMITTED,
    PROCESSING,
    SENDING,
    REPLAYING,
    DONE,
    ERROR
}
