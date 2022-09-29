package com.bbn.marti.remote.sync;

/*
 * Types of mission changes: Missions, and Mission content. Content changes will specify either a hash or uid, not both.
 *
 * The ordinal of these types matters at the the database level!
 */
public enum MissionChangeType {
    CREATE_MISSION, DELETE_MISSION, ADD_CONTENT, REMOVE_CONTENT, CREATE_DATA_FEED, DELETE_DATA_FEED
}