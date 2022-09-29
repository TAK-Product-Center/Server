

package com.bbn.marti.sync;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;

import com.bbn.security.web.MartiValidator;

/**
 * Metadata describing a specific resource in the Marti Enterprise Sync feature.
 * Each resource in the Enterprise Sync DB has a unique combination of attributes
 * such as resourec name, submission time, MIME type, that fully describes it. 
 * This class encapsulates that information. 
 * 
 * The nested enum, <code>Field</code>, defines the universe of available 
 * metadata attributes. In the typical case, not all <code>Metadata</code> instances
 * will have values for these attributes.
 * 
 * The implementation is a thin wrapper around a Map<Field, String[]>.
 * 
 *
 */
public class Metadata implements Serializable {
	
	private static final long serialVersionUID = -3768137666973346597L;

	public enum Field {
		Altitude(MartiValidator.Regex.Double, DEFAULT_FIELD_LENGTH, false, false),
		DownloadPath(MartiValidator.Regex.DirectoryName, MartiValidator.LONG_STRING_CHARS, false, false),
		Keywords(MartiValidator.Regex.MartiSafeString, MartiValidator.DEFAULT_STRING_CHARS, true, false),
		Latitude(MartiValidator.Regex.Double, DEFAULT_FIELD_LENGTH, false, false),
		Longitude(MartiValidator.Regex.Double, DEFAULT_FIELD_LENGTH, false, false),
		Hash(MartiValidator.Regex.Hexidecimal, MartiValidator.DEFAULT_STRING_CHARS, false, true),
		MIMEType(MartiValidator.Regex.MartiSafeString, MartiValidator.DEFAULT_STRING_CHARS, false, false),
		Name(MartiValidator.Regex.MartiSafeString, MartiValidator.DEFAULT_STRING_CHARS, false, false),
		Permissions(MartiValidator.Regex.WordList, MartiValidator.DEFAULT_STRING_CHARS, true, false),
		Size(MartiValidator.Regex.NonNegativeInteger, MartiValidator.SHORT_STRING_CHARS, false, true),
		Remarks(MartiValidator.Regex.MartiSafeString, MartiValidator.LONG_STRING_CHARS, false, false),
		SubmissionUser(MartiValidator.Regex.MartiSafeString, MartiValidator.DEFAULT_STRING_CHARS, false, false),
		PrimaryKey(MartiValidator.Regex.NonNegativeInteger, MartiValidator.DEFAULT_STRING_CHARS, false, true),
		SubmissionDateTime(MartiValidator.Regex.Timestamp, MartiValidator.DEFAULT_STRING_CHARS, false, true),
		UID(MartiValidator.Regex.MartiSafeString, MartiValidator.DEFAULT_STRING_CHARS, false, false),
		Contacts(MartiValidator.Regex.MartiSafeString, MartiValidator.DEFAULT_STRING_CHARS, true, false),
		CreatorUid(MartiValidator.Regex.MartiSafeString, MartiValidator.DEFAULT_STRING_CHARS, false, false),
		Tool(MartiValidator.Regex.MartiSafeString, MartiValidator.DEFAULT_STRING_CHARS, false, false),
		EXPIRATION(MartiValidator.Regex.Double, MartiValidator.SHORT_STRING_CHARS, false, false);
		
		/**
		 * Validation pattern to be used by ESAPI for input validation.
		 * The pattern is a regex described in the sytem configuration file, 
		 * <code>validation.properties</code>.
		 */
		public final MartiValidator.Regex validationType;
		/**
		 * Maximum length of the value of this attribute.
		 * Some fields are array-valued, in which case this refers to the maximum length of 
		 * each array element.
		 */
		public final int maximumLength;
		/**
		 * True if the attribute's value is a string array; false if the value is a single string.
		 */
		public final boolean isArray;
		/**
		 * True if this field is generated automatically by the persistence layer
		 * and should not be provided when storing the object.
		 * @see <code>PersistenceStore.insertResource</code>
		 */
		public final boolean isMachineGenerated;

		private Field(MartiValidator.Regex pattern, int maxLength, boolean isArray, boolean isMachineGenerated) {
			this.validationType = pattern;
			this.maximumLength = maxLength;
			this.isArray = isArray;
			this.isMachineGenerated = isMachineGenerated;
		}
		
		/**
		 * Gets the equivalent Field instance matching a given String.
		 * The string matching is case-insensitive.
		 * One Field, <code>MIMEType</code>, has an alias: it will match
		 * the string "MIME" as well as "MIMEType".
		 * 
		 * @param given String to convert to a Field
		 * @return the matching Field, or <code>null</code> if the String didn't match any Field
		 */
		public static Field fromString(String given) {
			if (given.compareToIgnoreCase("MIME") == 0) {
				return Metadata.Field.MIMEType;
			}
			
			for (Field field : Field.values()) {
				if (field.toString().compareToIgnoreCase(given) == 0) {
					return field;
				} 
			}
			
			return null;
		}
	}
	
	public static final int DEFAULT_FIELD_LENGTH = 1024;
	
	private static final Logger log = Logger.getLogger(Metadata.class.getCanonicalName());
	
	/**
	 * Makes a deep copy of a given Metadata instance.
	 * This is an alternative to <code>clone</code>
	 * @param original the object to be copied
	 * @return a deep copy of <code>original</code>
	 */
	public static Metadata copy(Metadata original) {
		Metadata copy = new Metadata();
		for (Field field : Field.values()) {
			String[] originalValues = original.getAll(field);
			if (originalValues != null) {
				String[] copyValues = new String[originalValues.length];
				for (int index = 0; index < originalValues.length; index++) {
					if (originalValues[index] != null) {
						copyValues[index] = new String(originalValues[index]);
					}
				}
				copy.set(field, copyValues);
			}
		}
		return copy;
	}
	
	/**
	 * Creates a metadata instance from a JSONObject
	 */
	public static Metadata fromJSON(JSONObject jsonObj) {
		Map<Field, String[]> parameters = new HashMap<Field, String[]>();
		for (Metadata.Field field : Metadata.Field.values()) {
			Object jsonValue = jsonObj.get(field.toString());
			if (jsonValue instanceof String) {
				parameters.put(field, new String[]{(String)jsonValue});
			} else if (jsonValue instanceof String[]) {
				parameters.put(field, (String[])jsonValue);
			} else if (jsonValue != null) {
				parameters.put(field, new String[]{jsonValue.toString()});
			}
		}
		return Metadata.fromMap(parameters);
	}

	public static Metadata fromSingleStringMap(Map<Field,String> map) {
		Metadata newObject = new Metadata();
		for (Field key : map.keySet()) {
			newObject.set(key, map.get(key));
		}

		return newObject;
	}
	
	public static Metadata fromMap(Map<Field, String[]> map)  {
		Metadata newObject = new Metadata();
		for (Field key : map.keySet()) {
			newObject.set(key, map.get(key));
		}
		return newObject;
	}
	
	/**
	 * Parses a JSON-encoded collection of Metadata instances
	 * @param text an encoded JSONObject or JSONArray of metadata
	 * @return an array containing each separate Metadata instance; will be empty if no valid metadata was parsed
	 */
	public static Metadata[] fromJSON(String text) throws IntrusionException, ValidationException {
		JSONParser parser = new JSONParser();
		
		ArrayList<Metadata> results = new ArrayList<Metadata>();
		try {
			Object parsedObj = parser.parse(text);
			if (parsedObj instanceof JSONObject) {
				JSONObject parsedJSON = ((JSONObject)parsedObj);
				Long count = (Long)parsedJSON.get(SearchServlet.RESULT_COUNT_KEY);
				JSONArray array = (JSONArray)parsedJSON.get(SearchServlet.RESULT_KEY);
				if (count == null && parsedObj instanceof JSONArray) {
					array = (JSONArray)parsedObj;
				}
				if (array != null) {
					for (Object jsonObj : array) {
						results.add(Metadata.fromJSON((JSONObject)jsonObj));
					}
					if (count != null && results.size() != count) {
						log.warning(SearchServlet.RESULT_COUNT_KEY  + "=" + count + " but JSON contains "
								+ results.size() + " results");
					}
				} else {
					results.add(Metadata.fromJSON(parsedJSON));
				}
			} else {
				log.severe("Unexpected type parsing JSON: " + parsedObj.getClass().getCanonicalName());
			}
		} catch (ParseException e) {
			log.severe("Error parsing JSON-encoded metadata: " + e.getMessage());
		}
		return results.toArray(new Metadata[results.size()]);
	}
		
	private Map<Field, String[]> fields =  new HashMap<Field, String[]>();
	
	/**
	 * Two Metadata instances are equal if all their attributes match in a case-sensitive lexical comparison.
	 */
	@Override
	public boolean equals(Object other) {
		return this.matches(Arrays.asList(Metadata.Field.values()), other);
	}
	
	/**
	 * Gets all values of the given metadata field.
	 * @param attribute name of the attribute to get
	 * @return Array of all values for the attribute, or <code>null</code> if the attribute is not defined.
	 */
	public String[] getAll(Field attribute) {
		return fields.get(attribute);
	}
	
	/**
	 * Gets the first value of a metadata attribute. Intended to be used on single-valued attrributes
	 * such as file name. 
	 * @param attribute 
	 * @return the first string associated with that attribute, or null if that attribute is not defined.
	 */
	public String getFirst(Field attribute) {
		String first = null;
		String[] all = this.getAll(attribute);
		if (all != null && all.length > 0) {
			first=all[0];
		}
		return first;
	}
	
	/**
	 * Gets the first value of a metadata attribute. Returns an empty string if attribute not defined. Never returns 
	 * <code>null</code>.
	 * @param attribute
	 * @return the first string associated with that attribute, or empty string if that attribute is not defined.
	 */
	public String getFirstSafely(Field attribute) {
		String value = getFirst(attribute);
		if (value == null) {
			value = "";
		}
		return value;
	}
	
	
	/**
	 * Gets the numeric value of the altitude field, if it is defined
	 * @return the altitude value, or <code>Double.NaN</code> if altitude is undefined
	 */
	public double getAltitude() {
		Double altitude = Double.NaN;
		String altitudeString = this.getFirst(Field.Altitude);
		if (altitudeString != null && !altitudeString.isEmpty()) {
			try {
				altitude = Double.parseDouble(altitudeString);
			} catch (NumberFormatException ex) {
				log.warning("Failed to parse " + Field.Altitude.toString() + ": " + ex.getMessage());
			}
		}
		return altitude; 
	}
	
	
	/**
	 * Gets the keywords for this Metadata instance as a string array.
	 * @return Array containing each keyword as a separate element, or null if keywords are undefined.
	 */
	public String[] getKeywords() {
		return this.getAll(Field.Keywords);
	}
	
	/**
	 * Gets the numeric value of the latitude field, if it is defined
	 * @return the latitude value, or <code>Double.NaN</code> if latitude is undefined
	 */
	public Double getLatitude() {
		Double latitude = Double.NaN;
		String latitudeString = this.getFirst(Field.Latitude);
		if (latitudeString != null && !latitudeString.isEmpty()) {
		try {
			latitude = Double.parseDouble(latitudeString);
		} catch (NumberFormatException ex) {
			log.warning("Failed to parse " + Field.Latitude + ": " + ex.getMessage());
		}
		}
		return latitude;
	}
		
	/**
	 * Gets the numeric value of the longitude field, if it is defined
	 * @return the longitude value, or <code>Double.NaN</code> if longitude is undefined.
	 */
	public Double getLongitude() {
		Double longitude = Double.NaN;
		String longitudeString = this.getFirst(Field.Longitude);
		if (longitudeString != null && !longitudeString.isEmpty()) {
		try {
			longitude = Double.parseDouble(this.getAll(Field.Longitude)[0]);
		} catch (NullPointerException | NumberFormatException ex) {
			log.warning("Failed to parse " + Field.Longitude + " : " + ex.getMessage());
		}
		}
		return longitude;
	}
	
	/**
	 * Gets the hash of the data object.
	 * @return a 
	 */
	public String getHash() {
		String hash = this.getFirst(Field.Hash);
		if (hash == null) {
			hash = "";
		}
		return hash;
	}
	
	/**
	 * @return the number of non-null metadata fields
	 */
	public int getNumberOfKeys() {
		int count = 0;
		for (Metadata.Field field : Metadata.Field.values()) {
			if (fields.containsKey(field) && fields.get(field) != null) {
				count++;
			}
		}
		return count;
	}
	
	/**
	 * @return the coordinates of the resource's location as a String: POINT(lon lat), 
	 * or <code>null</code> if either coordinate is undefined
	 */
	public String getPointString() {
		Double lon = this.getLongitude();
		Double lat = this.getLatitude();
		if (lat == null || lon == null || lat.isNaN() || lon.isNaN()) {
			return null;
		}
		return "POINT(" + this.getLongitude() + " " + this.getLatitude() + ")";
	}
	
	/**
	 * 
	 * @return the database primary key of the instance, or <code>null</code> if the key is not defined
	 */
	public Integer getPrimaryKey() {
		Integer key = null;
		String[] primaryKeyStrings = this.getAll(Metadata.Field.PrimaryKey);
		if (primaryKeyStrings != null && primaryKeyStrings.length > 0) {
		try {	
			key = Integer.parseInt(primaryKeyStrings[0]);
		} catch (NullPointerException | NumberFormatException ex) {
			log.warning("Failed to parse " + Metadata.Field.PrimaryKey.toString() + ": " + ex.getMessage());
		}
		}
		return key;
	}
	
	/**
	 * Gets the size of the content in bytes.
	 * @return the size of the content in bytes, or <code>null</code> if the metadata has no associated content.
	 */
	public Integer getSize() {
		Integer sizeInBytes = null;
		String size = this.getFirst(Field.Size);
		if (size != null && !size.isEmpty()) {
			try {
				sizeInBytes = Integer.parseInt(size);
			} catch (Exception ex) {
				log.severe("Error parsing size of Enerprise Sync object. Primary key " + this.getPrimaryKey()
						+ ": " + ex.getMessage());
			}
		}
		
		return sizeInBytes;
	}
	
	public Long getExpiration() {
	    String expiration_string = this.getFirst(Field.EXPIRATION);
	    if (expiration_string == null) {
	        return null;
	    }
	    Long expiration = Long.parseLong(expiration_string);
	    return expiration;
	}

	public String getUid() {
		return this.getFirst(Field.UID);
	}
		
	/**
	 * @return true if this object has all the same fields and values of <code>other</code>.
	 */
	public boolean isSupersetOf(Metadata other) {
		return this.matches(other.fields.keySet(), other);
	}
	
	/**
	 * Returns true if a collection of fields have the same value as in another Metadata instance.
	 * This is like a selective version of <code>equals</code> that only compares specified fields.
	 * It is useful when you want to compare two <code>Metadata</code> instances but ignore
	 * automatically-populated fields such as submission time or primary key.
	 * 
	 * @param fields Collection of fields to compare
	 * @param other instance to compare to
	 * @return true if the contents of all members of <code>fields</code> lexically match
	 */
	public boolean matches(Collection<Metadata.Field> fields, Object other ) {
		if (!(other instanceof Metadata)) {
			return false;
		}
		Iterator<Metadata.Field> itr = fields.iterator();
		while (itr.hasNext()) {
			Field field = itr.next();
			log.finer("Comparing field " + field.toString());
			String[] myValue = this.getAll(field);
			String[] otherValue = ((Metadata)other).getAll(field);
	
			if (myValue == null && otherValue == null) {
				// Both null; that's a match
				continue;
			} else if (myValue != null && otherValue == null) {
				return false;
			} else if (myValue.length != otherValue.length) {
				return false;
			} else {
				for (int index = 0; index < myValue.length; index++) {
					if (myValue[index].compareTo(otherValue[index]) != 0) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	public void set(Field field, String value) {
		fields.put(field, new String[] {value});
	}
	
	public void set(Field field, String[] values) {
			fields.put(field, values);
			if (values != null && values.length > 1 && !field.isArray) {
				log.warning("Too many values provided for " + field.toString() + "; ignoring all but the first.");
			}
	}

	public void set(Field field, Double value) {
		if (value != null && !Double.isNaN(value)) {
			set(field, Double.toString(value));
		}
	}
	
	public void set(Field field, Long value) {
		if (value != null) {
			set(field, Long.toString(value));
		}
	}
	
	/**
	 * Converts the Metadata instance to a <code>JSONObject</code> for generating JSON output.
	 * 
	 * @return a JSONObject that can be converted to a String using <code>JSONObject.toJSONString()</code>
	 */
	@SuppressWarnings("unchecked")
	public JSONObject toJSONObject() {
		JSONObject json = new JSONObject();
		for (Metadata.Field field : Metadata.Field.values()) {
			String[] values = this.getAll(field);
			if (!field.isArray && values != null && values.length == 1) {
				json.put(field.toString(), values[0]);
			} else if (values != null) {
				JSONArray array = new JSONArray();
				for (String value : values) {
					array.add(value);
				}
				json.put(field.toString(), array);
			}
		}
		return json;
	}
	
	public void validate(Validator validator) throws ValidationException, IntrusionException {
		if (validator != null) {
			for (Field field : fields.keySet()) {
				String[] values = fields.get(field);
				if (values != null) {
					for (String value : values) {
						validator.getValidInput("Metadata validation (" + field.toString() + ")",
								value, field.validationType.name(), field.maximumLength, true);
						log.finer("Validated " + field);
					}
				}
			}
 		}
	}

	@Override
	public String toString() {
		return "Metadata [fields=" + fields + "] json: " + toJSONObject();
	}
}
