package tak.server.util;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class JAXBUtils {

    private static final Logger logger = LoggerFactory.getLogger(JAXBUtils.class);

    /**
     * Parses an XML resource stream to the specified packageName.  It assumes the packageName is valid and that is has been
     * generated from the schema
     *
     * @param xmlInputStream The input stream for the XML data to load
     * @param packageName    The getConsistentUniqueReadableIdentifier of the package that corresponds to the contents of the file
     * @param <T>            The type corresponding to the packagename
     * @return The object of type {@link T} if the file exists, null if it does not
     * @throws FileNotFoundException If the file was not found
     * @throws JAXBException         If a parsing exception occurs
     */
    public static <T> T loadJAXifiedXML(InputStream xmlInputStream, String packageName) throws FileNotFoundException, JAXBException {
        JAXBContext jc = JAXBContext.newInstance(packageName);
        Unmarshaller u = jc.createUnmarshaller();
        return (T) u.unmarshal(xmlInputStream);
    }

    /**
     * Parses an XML file to the specified packageName.  It issumes the packageName is valid and that is has been
     * generated from the schema
     *
     * @param xmlFile     The file to open
     * @param packageName The name of the package that corresponds to the contents of the file
     * @param <T>         The type corresponding to the packagename
     * @return The object of type {@link T} if the file exists, null if it does not
     * @throws FileNotFoundException If the file was not found
     * @throws JAXBException         If a parsing exception occurs
     */
    public static <T> T loadJAXifiedXML(String xmlFile, String packageName) throws FileNotFoundException, JAXBException {

        File f = new File(xmlFile);

        try {
            InputStream is = new FileInputStream(f);
            JAXBContext jc = JAXBContext.newInstance(packageName);
            Unmarshaller u = jc.createUnmarshaller();
            Object result = u.unmarshal(is);
            is.close();
            return (T) result;
        } catch (FileNotFoundException ex) {
            logger.error(xmlFile + " not found.");
            return null;
        } catch (IOException ex) {
            logger.error("Error loading XML from " + xmlFile, ex);
            return null;
        }
    }

    /**
     * Saves The object created with JAXB to an xml file
     *
     * @param xmlFile              The target file
     * @param obj                  THe object to serialize
     * @param createNewIfNecessary If true, a new file will be created if one does not exist
     * @throws IOException   If an IOException occurs. May result in a mangled file
     * @throws JAXBException If a JAXBException occurs. Will not result in a mangled file
     */
    public static void saveJAXifiedObject(String xmlFile, Object obj, boolean createNewIfNecessary) throws IOException, JAXBException {
        // First write it into a ByteArrayOutputStream so JAXBExceptions don't result in a mangled original file
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String packageName = obj.getClass().getPackage().getName();
        JAXBContext jc = JAXBContext.newInstance(packageName, obj.getClass().getClassLoader());
        Marshaller m = jc.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.marshal(obj, baos);

        // Yay, not JAXBException. Now write to the file
        File file = new File(xmlFile);

        // Create a new file if necessary and allowed
        if (!file.exists() && createNewIfNecessary) {
            file.createNewFile();
        } else {
            File copied = new File(xmlFile + ".backup");
            FileUtils.copyFile(file, copied);
        }

        OutputStream fos = new FileOutputStream(file);
        // Write to the file
        baos.writeTo(fos);
        fos.flush();
        fos.close();
    }


}
