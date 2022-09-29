package com.bbn.security.web;

public class MartiValidatorConstants {
	
	public static final int LONG_STRING_CHARS = 2047;
	public static final int DEFAULT_STRING_CHARS = 255;
	public static final int SHORT_STRING_CHARS = 128;
	
	/**
	 * Members of this enum map to regexes defined in apache-tomcat/lib/validation.properties 
	 * 
	 *
	 */
	public enum Regex {
	
	    CertCommonName,     		// allow only word characters, whitespace, ',' and '='
		Coordinates, 				// decimal latitude or longitude
		ConfigAttribute, 			// attribute name for Core Config
		CotType,					// CoT type such as "a-f-.-u"
		Double,						// signed or unsigned decimal
		DirectoryName,     		 	// POSIX directory name
		Hexidecimal,				// hexidecimal numbers
		KmlGeometry,				// <Point><coordinates>  tag in KML
		MartiSafeString, 			// Alphanumeric plus certain special characters such _, -, :, /
		NonNegativeInteger, 		// Digits only
		URL,						// http, https, ftp, and ftps URLs
		SafeString,					// Alphanumeric plus space character
		RestrictedRegex,			// Simple regex patterns (no grouping) for pattern matches
		SupportedProtocol,  		// STCP, TCP, or UDP (case insensitive)
		Timestamp,					// CoT timestamp,
		WordList,					// comma-separated list of alphanumeric words
		XmlBlackList,				// disallowed strings for XML
		XmlBlackListWordOnly, 		// disallowed strings for XML, relaxed to allow 'script' as substring, ex <description>
		XpathBlackList,				// disallowed string for XPath expressions
		VideoURL,					// similar to URL, but includes addition protocols for video streaming
		Filename,					// valid filenames
		PreventDirectoryTraversal	// disallow attempts at directory traversal by not allowing .. in paths
	}
	
}
