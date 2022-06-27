

package tak.server.cot;

import java.io.File;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.xml.sax.*;

import tak.server.cot.CotParser.ValidationErrorCallback.ErrorLevel;

public class CotParser {
    public static final String SCHEMA_FILE = "Event_modified.xsd";

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(CotParser.class.getCanonicalName());

    /////////////////////////////////////////////
    // Individual parsers

    private SAXReader reader = null;

    public interface ValidationErrorCallback {
        enum ErrorLevel {
            WARNING,
            ERROR,
            FATAL
        };
        public void onValidationError(ErrorLevel level, Exception saxParseException);
    }

    public CotParser(boolean isValidating) {
        this(null, isValidating);
    }

    private static ThreadLocal<SAXParserFactory> factory = new ThreadLocal<>();

    private CotParser(ValidationErrorCallback errback, boolean isValidating) {

        if (factory.get() == null) {
            factory.set(SAXParserFactory.newInstance());

            try {

                factory.get().setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.get().setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.get().setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                factory.get().setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                factory.get().setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                factory.get().setXIncludeAware(false);

            } catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
                logger.error("exception enabling secure processing", e);
            }

            if (isValidating) {
                if (new File(SCHEMA_FILE).canRead()) {
                    try {
                        factory.get().setSchema(
                                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(
                                        new Source[]{new StreamSource(SCHEMA_FILE)}));
                    } catch (SAXException e) {

                        // ignore, don't validate
                        isValidating = false;
                    }
                }
            }
        }

        // Setup the XML parser
        try {
            SAXParser parser = factory.get().newSAXParser();
            reader = new SAXReader(parser.getXMLReader());
            reader.setValidation(false);

            // If we are going to be validating...
            if (isValidating) {
                final ValidationErrorCallback errorCallback = errback;
                reader.setErrorHandler(new ErrorHandler() {
                    @Override
                    public void warning(SAXParseException exception) throws SAXException {
                        if(errorCallback != null)
                            errorCallback.onValidationError(ErrorLevel.WARNING, exception);
                        throw exception;
                    }

                    @Override
                    public void fatalError(SAXParseException exception) throws SAXException {
                        if(errorCallback != null)
                            errorCallback.onValidationError(ErrorLevel.FATAL, exception);
                        throw exception;
                    }

                    @Override
                    public void error(SAXParseException exception) throws SAXException {
                        if(errorCallback != null)
                            errorCallback.onValidationError(ErrorLevel.ERROR, exception);
                        throw exception;
                    }
                });
            }
        } catch (Exception e) {
            // ignore, and don't validate
        }
    }

    public Document parse(String xml) throws DocumentException {
        return reader.read(new InputSource(new StringReader(xml)));
    }

}
