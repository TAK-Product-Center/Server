

package com.bbn.marti.util;

import org.json.simple.JSONObject;

/**
 * @version 0
 * @since 2014-09-1
 * <p>
 *	A class for representing a latitude, longitude, and altitude as doubles. Altitude is assumed to be HAE.
 */
public class Coord {
	double lon;
	double lat;
	double alt;

	public Coord(double lat, double lon, double alt) {
		// default constructor
		this.lat = lat;		
		this.lon = lon;
		this.alt = alt;
	}
	
	protected Coord(Coord that) {
		// copy constructor
		this.lat = that.lat;
		this.lon = that.lon;
		this.alt = that.alt;
	}
	
	/*				Getters			*/
	
	/**
	* Returns the stored latitude.
	*/
	public double getLat() {
		return lat;
	}

	/**
	* Returns the stored longitude.
	*/
	public double getLon() {
		return lon;
	}
	
	/**
	* Returns the stored altitude.
	*/
	public double getAlt() {
		return alt;
	}
	
	/*         							 Mutators             */

	/**
	* A mutator method to set the latitude and longitude to the given arguments.
	*/
	public void set(double lat, double lon) {
		set(lat, lon, this.alt);
	}
	
	/**
	* A mutator method to set the latitude, longitude, and altitude to the given arguments.
	*/
	public void set(double lat, double lon, double alt) {
		// modify the lon, lat, and alt fields to match the arguments
		this.lat = lat;
		this.lon = lon;
		this.alt = alt;
	}

	/**
	* A mutator method to set the latitude, longitude, and altitude to the corresponding fields
	* contained by the given coordinate.
	*/
	public void set(Coord c) {
		this.lat = c.lat;
		this.lon = c.lon;
		this.alt = c.alt;
	}

	/**
	*	A mutator method for adding the given latitude and longitude differentials
	* to the current latitude and longitude.
	*/
	public void moveInPlace(double latDiff, double lonDiff) {
		// move the coordinate by mutating the current reference
		move(latDiff, lonDiff, 0.0);
	}

	/**
	*	A mutator method for adding the given latitude, longitude, and altitude differentials
	* to the current latitude, longitude, and altitude.
	*/
	public void moveInPlace(double latDiff, double lonDiff, double altDiff) {
		// modify the lon,lat, and alt fields to match the delta between the current 
		lat += latDiff;
		lon += lonDiff;
		alt += altDiff;
	}

	/* 										Immutable members					*/
	/**
	*	An immutable method for returning a new Coordinate with the given latitude, longitude, and altitude differentials
	* added to the latitude, longitude, and altitude of this Coord. If the Coord is a NamedCoord, the name is not preserved.
	*/
	public Coord move(double latDiff, double lonDiff) {
		// move the coordinate by returning a new reference to the moved coordinate
		return move(latDiff, lonDiff, 0.0);
	}

	/**
	*	An immutable method for returning a new Coordinate with the given latitude, longitude, and altitude differentials
	* added to the latitude, longitude, and altitude of this Coord. If the Coord is a NamedCoord, the name is not preserved.
	*/
	public Coord move(double latDiff, double lonDiff, double altDiff) {
		// immutable move
		Coord c = new Coord(this);
		c.moveInPlace(latDiff, lonDiff, altDiff);

		return c;
	}

	/**
	*	A deep copy constructor for this Coord.
	*/
	public Coord copy() {
		return new Coord(this);
	}

	/**
  	* A method to convert this datatype to a Json String representation.
 	 * <p>
 	 * The Json representation contains the fields lat", "lon", and "alt". All fields are given as Json numbers.
 	 */
	public String toJson() {
		return this.toJsonObject().toString();
	}

	/**
	* A method to convert this datatype to its JSON object 
	*/
	public JSONObject toJsonObject() {
		JSONObject obj = new JSONObject();
		obj.put("lat", new Double(lat));
		obj.put("lon", new Double(lon));
		obj.put("alt", new Double(alt));
		obj.put("altReference", new Integer(1));

		return obj;
	}
}