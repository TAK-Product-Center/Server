

package com.bbn.marti;

import java.io.IOException;
import java.util.logging.Logger;

import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.util.EGM96;

/**
 * Class that converts HAE altitude to MSL. This is basically a thin wrapper for the NASA World Wind
 * EGM96 class.
 * 
 * @see gov.nasa.worldwind.util.EMG96
 *
 */
public class AltitudeConverter {
	
	private static final Logger log = Logger.getLogger(AltitudeConverter.class.getCanonicalName());
	
	public AltitudeConverter (String filename) {
		try {
			this.egm96 = new EGM96(filename);
			log.fine("Initialzed from " + filename);
		} catch (IOException ex) {
			log.warning("Failed to load geoid data from " + filename + ": " + ex.getMessage());
		} catch (IllegalArgumentException ex) {
			log.warning("Failed to load geoid data: " + ex.getMessage());
		} catch(WWRuntimeException ex) {
			log.warning("Failed to load geoid data: " + ex.getMessage());
		}
	};
	
	private EGM96 egm96 = null;

	/**
	 * Converts altitude from HAE (Height Above Ellipsoid) to MSL (Mean Sea Level)
	 * @param hae HAE in meters
	 * @param latitude degrees
	 * @param longitude degrees
	 * @return MSL altitude in meters, or Double.NaN if geoid data is not available.
	 */
	public Double haeToMsl(double hae, double latitude, double longitude) {
		double msl = hae - getOffset(latitude, longitude);  // All arithmetic involving NaN yields NaN as a result
		log.finer("hae=" + hae + ", msl=" + msl);
		return msl;
	}
	
	/**
	 * Convenience method that tells you whether the instance has been initialized.
	 * @return <code>true</code> if the EMG96 data has been successfully loaded, <code>false</code> otherwise
	 */
	public boolean isInitialized() {
		return egm96 != null;
	}

	/**
	 * Gets the EMG96 offset in meters for the given coordinates.
	 * @param latitude
	 * @param longitude
	 * @return Vertical offset in meters, or <code>Double.NaN</code> where offset data is not available.
	 */
	private Double getOffset(double latitude, double longitude) {
		if (!this.isInitialized()) {
			return Double.NaN;
		}
		return egm96.getOffset(Angle.fromDegrees(latitude), Angle.fromDegrees(longitude));
	}
		
}
