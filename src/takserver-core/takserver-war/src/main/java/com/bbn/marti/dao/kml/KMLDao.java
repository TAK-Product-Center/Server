

package com.bbn.marti.dao.kml;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.bbn.marti.remote.UIDResult;

import tak.server.cot.CotElement;
import tak.server.util.Association;

import org.jetbrains.annotations.NotNull;

/*
 * Data Access Object to obtain KML CoT data from database
 * 
 */
public interface KMLDao {

	List<CotElement> getCotElements(String cotType, int secAgo, String groupVector);

	List<CotElement> getCotElements(String cotType, String groupVector);

	CotElement parseFromResultSet(ResultSet results) throws SQLException;

	void parseDetailText(CotElement cotElement);

    List<Association<Date, Long>> getCotEventCountByHour(String groupVector, boolean useCache, boolean userGroupsOnly);

    CotElement deserialize(ResultSet results) throws SQLException;

    CotElement parse(CotElement cotElement) throws SQLException;

    List<Integer> getImageCotIdsByCallsign(String callsign, int limit);
    
    List<Integer> getImageCotIdsByUid(String uid, int limit);
    
    byte[] getImageBytesByCotId(Integer cotId);

    Set<String> getUidsHavingImages(Date start, Date end, String groupsBitVector);
    
    List<UIDResult> searchUIDs(Date startDateTime, Date endDateTime);

    @NotNull
    String latestCallsign(@NotNull String uid);
}
