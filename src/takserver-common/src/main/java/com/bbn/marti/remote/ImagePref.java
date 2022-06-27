

package com.bbn.marti.remote;

public enum ImagePref {
	FULL_IMAGE,
	THUMBNAIL,
	URL_ONLY,
	DATABASE,
	NONE;

	/**
	* Attempts to match a string to an image preference with an equivalent string value,
	* and returns the given default if one cannot be found.
	*/
	public static ImagePref imagePrefOrElse(String modeName, final ImagePref fallback) {
        if (fallback == null) {
            throw new IllegalArgumentException("Supplied default can't be null");
        }
    
		ImagePref pref = null;
		try {
			pref = ImagePref.valueOf(modeName);
		} catch (IllegalArgumentException e) {
			pref = fallback;
		}
		
        if (pref == null) {
            throw new IllegalStateException("Should always be able to find an Image Preference with a supplied default");
        }
        
		return pref;
	}
}