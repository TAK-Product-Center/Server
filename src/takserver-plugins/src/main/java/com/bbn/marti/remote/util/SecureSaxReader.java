package com.bbn.marti.remote.util;

import java.io.StringReader;

import javax.xml.XMLConstants;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Factory for dom4j {@link SAXReader}s that are hardened against XXE, external-entity, and
 * DTD-expansion attacks. A plain {@code new SAXReader()} (and {@code DocumentHelper.parseText})
 * resolves DOCTYPE declarations and external entities by default, so any XML coming from a
 * client, a federate, or a mission package must be parsed through this helper instead.
 *
 * <p>This is the dom4j counterpart of {@link SecureXmlParser} (which produces w3c DOM
 * documents). Disabling DOCTYPE declarations alone is sufficient to prevent entity-based
 * attacks; the remaining features are defense-in-depth.
 */
public final class SecureSaxReader {

    private static final Logger logger = LoggerFactory.getLogger(SecureSaxReader.class);

    private SecureSaxReader() {}

    /** A new dom4j {@link SAXReader} with DOCTYPE declarations and external entities disabled. */
    public static SAXReader newReader() {
        SAXReader reader = new SAXReader();
        try {
            // The decisive control: any DOCTYPE in the input becomes a fatal parse error,
            // which removes both external-entity (XXE/SSRF) and entity-expansion (billion
            // laughs) vectors.
            reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            reader.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (SAXException e) {
            // If hardening cannot be applied, fail closed rather than parse untrusted XML insecurely.
            logger.error("unable to harden SAXReader against XXE", e);
            throw new IllegalStateException("unable to configure secure SAXReader", e);
        }
        reader.setIncludeExternalDTDDeclarations(false);
        reader.setIncludeInternalDTDDeclarations(false);
        return reader;
    }

    /** Parse untrusted XML text into a dom4j {@link Document} with XXE protections enabled. */
    public static Document parseText(String xml) throws DocumentException {
        return newReader().read(new StringReader(xml));
    }
}
