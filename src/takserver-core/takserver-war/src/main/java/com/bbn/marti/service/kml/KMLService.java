

package com.bbn.marti.service.kml;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import com.bbn.marti.util.Coord;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import tak.server.cot.CotElement;

public interface KMLService {

	// Perform the Kml request, obtaining CoT data from the database, and rendering it appropriately as Kml.
    Kml process(String cotType, int secAgo, String groupVector);

	// Render the list of cot elements as Kml.
	Kml process(LinkedList<CotElement> cotElements);

	/**
	 * populates a kml document with the transforms of each query result -{@literal >} kml object.
	 * <p>
	 * also populates the kml doc with the requisite set of styles for representing each cot object.
	 */
	void buildFeatures(LinkedList<CotElement> qrs, Document doc);

	/**
	 * Parses the current ResultSet cursor position into a QueryResult, returning null if it cannot be parsed correctly.
	 */
	CotElement parseFromResultSet(ResultSet results) throws SQLException;

	/** 2384 2424
	 * Parses the detailText from a QueryResult into an ordered list of coordinates taken from the link elements in detail.
	 * Returns the empty list if if cannot be parsed
	 */
	List<Coord> parseRouteFromDetail(String detailText);

    CotElement deserializeFromResultSet(ResultSet results) throws SQLException;

    void setStyleUrlBase(Kml kml, String urlBase);
}
