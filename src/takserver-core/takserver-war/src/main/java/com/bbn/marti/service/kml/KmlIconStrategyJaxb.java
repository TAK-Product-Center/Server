

package com.bbn.marti.service.kml;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.bbn.marti.model.kml.Icon;
import com.bbn.marti.model.kml.Icon.IconParts;
import com.bbn.marti.remote.util.SecureXmlParser;

import com.google.common.base.Strings;

import tak.server.cot.CotElement;

import org.w3c.dom.Document;

/*
 * 
 * Concrete KML icon strategy, which uses JAXB to parse the detail text and obtain all of the icon-related fields.
 * 
 */

public class KmlIconStrategyJaxb extends AbstractKmlIconStrategy implements IconStrategy<CotElement> {

    private static JAXBContext jaxbContext = null;

    private static ThreadLocal<Unmarshaller> jaxbUnmarshallerThreadLocal = new ThreadLocal<>();

    public KmlIconStrategyJaxb() {
    }


    public static void ParseUserIcon(Document doc, CotElement cotElement) {

        if (doc == null) {
            return;
        }

        if (jaxbContext == null) {
            try {
                jaxbContext = JAXBContext.newInstance(Detail.class, Usericon.class, Color.class);
            } catch (JAXBException e) {
                logger.error("jaxb exception creating unmarshaller", e);
            }
        }
        if (Strings.isNullOrEmpty(cotElement.detailtext)) {
            logger.debug("empty detail field");
            return;
        }

        Unmarshaller jaxbUnmarshaller = jaxbUnmarshallerThreadLocal.get();
        if (jaxbUnmarshaller == null) {
            try {
                jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                jaxbUnmarshallerThreadLocal.set(jaxbUnmarshaller);
            } catch (JAXBException e) {
                logger.error("exception creating jaxb unmarshaller", e);
                return;
            }
        }

        Detail detail = null;

        try {
            detail = (Detail) jaxbUnmarshaller.unmarshal(doc);
            logger.debug("detail: " + detail);
        } catch (JAXBException e) {
            logger.error("exception unmarshalling detail xml", e);
        }

        String iconsetPath = "";
        String iconColorString = "";

        try {
            if (detail != null) {
                if (detail.getUsericon() != null) {
                    iconsetPath = detail.getUsericon().getIconSetPath();
                    cotElement.iconSetPath = iconsetPath;

                    // if the iconsetpath attribute was found, parse the contents
                    try {
                        IconParts iconParts = Icon.parseIconPath(iconsetPath);

                        cotElement.iconSetUid = iconParts.iconsetUid;
                        cotElement.iconGroup = iconParts.group;
                        cotElement.iconName = iconParts.name;
                    } catch (Throwable t) {
                        logger.debug("exception parsing iconsetpath: " + iconsetPath, t);
                    }
                }

                if (detail.getColor() != null) {
                    iconColorString = detail.getColor().getArgb();
                    try {
                        Long iconColor = Long.parseLong(iconColorString);
                        cotElement.iconArgbColor = iconColor;
                    } catch (Throwable t) {
                        logger.trace("exception parsing iconColorString " + iconColorString + " to long");
                    }
                }
            }
        } catch (Throwable t) {
            logger.debug("exception getting iconsetpath and color", t);
        }

        logger.debug("iconSetPath: " + iconsetPath + " iconColorString: " + iconColorString);
    }

    public static void ParseUserIcon(CotElement cotElement) {
        try {
            Document doc = SecureXmlParser.makeDocument(cotElement.detailtext);
            ParseUserIcon(doc, cotElement);
        } catch (Throwable t) {
            logger.debug("makeDocument threw exception in ParseUserIcon!", t);
        }
    }

    // get iconset path 
    @Override
    public void parseUserIcon(CotElement cotElement) {
        ParseUserIcon(cotElement);
    }

    // These model classes are only used internally in this class, by JAXB, to specify the detail XML structure.

    @XmlRootElement(name = "detail")
    public static class Detail {

        public Detail() { }

        private Usericon usericon;
        private Color color;

        @XmlElement(name = "usericon")
        public Usericon getUsericon() {
            return usericon;
        }
        public void setUsericon(Usericon usericon) {
            this.usericon = usericon;
        }

        @XmlElement(name = "color")
        public Color getColor() {
            return color;
        }
        public void setColor(Color color) {
            this.color = color;
        }   
    }

    public static class Usericon {

        private String iconSetPath;

        public Usericon() { }

        @XmlAttribute(name = "iconsetpath")
        public String getIconSetPath() {
            return iconSetPath;
        }

        public void setIconSetPath(String iconSetPath) {
            this.iconSetPath = iconSetPath;
        }
    }

    public static class Color {

        public Color() { }

        private String argb;

        @XmlAttribute(name = "argb")
        public String getArgb() {
            return argb;
        }

        public void setArgb(String argb) {
            this.argb = argb;
        }
    }
}
