package tak.server.federation.message;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * 
 * Currently, this class provides a mapping from file extensions to valid Mime Type Strings.<br>
 * We may want to extend it do offer more functionality. 
 *  
 * TODO Discuss putting in the PluginContext for use by all plugins
 *
 */
public final class MetadataUtils {
	private final ConcurrentMap<String, String> fileExtensionToMimeTypeString = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, String> mimeTypeToFileExtension = new ConcurrentHashMap<>();
	private static MetadataUtils instance = null;
	private static final String DEFAULT_FILE_NAME = "mimetypes.csv";
	//private static final Logger LOGGER = LoggerFactory.getLogger(MetadataUtils.class);
	
	
	private MetadataUtils() throws IOException{
		// file only contains "image/jpeg"
		mimeTypeToFileExtension.put("image/jpg", ".jpg");
		
		// Compatibility for ATAK. Since this is an old MIME Type, 
		// we only want to recognize it, not use it based on the file extension
		mimeTypeToFileExtension.put("application/x-zip-compressed", ".zip");
		
		InputStream inputStream = MetadataUtils.class.getClassLoader().getResourceAsStream(DEFAULT_FILE_NAME); 
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
		String fileLine = reader.readLine();
		while (fileLine != null){
			String[] mimeTypeInfo = fileLine.toLowerCase(Locale.ENGLISH).split(","); // split values of CSV
			// index 0 is description, don't need for now
			fileExtensionToMimeTypeString.put(mimeTypeInfo[2], mimeTypeInfo[1]);
			mimeTypeToFileExtension.put(mimeTypeInfo[1], mimeTypeInfo[2]);
			
			fileLine = reader.readLine();
		}
		reader.close();

	}
	@SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
	public static synchronized MetadataUtils getInstance() throws IOException{
		if (instance == null){
			instance = new MetadataUtils();
		}
		return instance;
	}
	
	public String getMimeTypeFromFileName(String fileName){
		
	    int lastPeriod = fileName.lastIndexOf('.');
	    
	    if (lastPeriod == -1){
	       return null;
	    }
	    String extension = fileName.substring(lastPeriod);
	    return getMimeType(extension);
	}
	
	/**
	 * 
	 * @param fileExtension, including period
	 * @return MIME Type in String format
	 */
	public String getMimeType(String fileExtension){
		return fileExtensionToMimeTypeString.get(fileExtension.toLowerCase(Locale.ENGLISH));
	}
	
	/**
	 * 
	 * @param mimeType Type in String format
	 * @return fileExtension, including period
	 */
	public String getFileExtension(String mimeType){
		return mimeTypeToFileExtension.get(mimeType.toLowerCase(Locale.ENGLISH));
	}
	
	/**
	 * 
	 * @param possibleMime
	 * @return true if possibleMime is the string representation of a recognized MIME Type according to ROGER
	 */
	public boolean isValidMimeType(String possibleMime){
		return mimeTypeToFileExtension.containsKey(possibleMime);
	}
	
	/**
	 * Replaces file extension, where file extension is the characters after the last period in fileName, according to the mimeType provided. 
	 * Appends file extension if none is found.  
	 * @param fileName
	 * @return
	 */
	public String changeFileExtension(String fileName, String mimeType){
		String coreName = removeFileExtension(fileName);
		return coreName + getFileExtension(mimeType);
	}
	
	   /**
     * Remove file extension, where file extension is the characters after the last period in fileName  
     * @param fileName
     * @return a new string with no extension
     */
    public String removeFileExtension(String fileName){
        int lastPeriod = lastIndexOf(fileName, '.');
        String coreName = fileName;
        if (lastPeriod != -1){
            coreName = fileName.substring(0, lastPeriod);
        }
        return coreName;
    }
	
	private int lastIndexOf(String string, char character){
		//if index = -1 right away, return -1 because the character isn't there
		int lastIndex = -1;
		int index = string.indexOf(character);
		while (index != -1){
			lastIndex = index;
			index = string.indexOf(character, index+1);
		} // now index is -1
		return lastIndex;
	}
	
	
	/**
	 * This method makes a "deep clone" of any Java object it is given. 
	 * Returns null for null object
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public static Object deepClone(Object object) throws IOException, ClassNotFoundException {
		if (object == null) {
		    return null;
		}
	    if (SetWrapper.class.isAssignableFrom(object.getClass())){
			return ((SetWrapper<?>) object).deepCopy();
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(object);
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		return ois.readObject();
	 }
	
	
	/**
     * Creates parent metadata by combining metadata from child messages (does <b>not</b> use parentMetadata from metadataConstants)
     * @param messages
     * @param metadataUtils
     * @return
     */
    public Map<String, Object> createParentMetadata(Message... messages){
        // create combinedMessage metadata
        Map<String, Object> metadata = new HashMap<>(messages[0].getMetadataReadOnly());
        // remove child filename, child Mime type
        metadata.remove(MetadataConstants.Filename);
        metadata.remove(MetadataConstants.MIME_Type);

        metadata.remove(MetadataConstants.ChildMessageIndex.toString());
        metadata.remove(MetadataConstants.NumChildrenCreated.toString());
        return metadata;
    }
}
