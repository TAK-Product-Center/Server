

package com.bbn.marti.service.kml;

import java.util.Locale;

import com.google.common.base.Strings;

import tak.server.cot.CotElement;
import tak.server.util.Association;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Persister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.dao.kml.IconRepository;
import com.bbn.marti.util.KmlUtils;

/*
 * Encapsulates the logic for icon assignment based on a CoT event. Concrete implementation classes will extend this class, in particular with regard to XPath or XML deserialization. 
 * 
 */
public abstract class AbstractKmlIconStrategy {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractKmlIconStrategy.class);

    protected static final String ICON_RESOURCE_PATH = "api/icon";
    
    protected static final String COT_MAPPING_2525B_UID = "COT_MAPPING_2525B";
    
    @Autowired
    protected IconRepository iconRepository;
    
    public IconRepository getIconRepository() {
        return iconRepository;
    }

    public void setIconRepository(IconRepository iconRepository) {
        this.iconRepository = iconRepository;
    }

    public AbstractKmlIconStrategy() {
        super();
    }

    public void assignIcon(CotElement cotElement) {

        // parse the detail field of the CoT event to assign values as appropriate to the usericon field of the CotElement
        try {
            parseUserIcon(cotElement);
        } catch (Throwable t) {
            logger.debug("exception parsing user icon data from CoT detail text", t);
        }

        Association<String,String> urls = getStyleAndIconUrls(cotElement);
        cotElement.urls = urls;
        cotElement.styleUrl = urls.getKey();
        cotElement.iconUrl = urls.getValue();
    }

    abstract void parseUserIcon(CotElement cotElement);

    public String getStyleUrl(CotElement qr) {
        return getStyleAndIconUrls(qr).getKey();
    }

    public String getIconUrl(CotElement qr) {
        return getStyleAndIconUrls(qr).getValue();
    }

    protected Association<String,String> getStyleAndIconUrls(CotElement qr) {

        if (Strings.isNullOrEmpty(qr.iconSetUid)) {
            logger.trace("iconsetUid not present in cot, assigning 2525B/team/medevac etc icon");
            return assignIcon2525B(qr);
        }
        
        // if the iconset tag specifies 2525B mapping, fall back to the standard method for that
        if (qr.iconSetUid.toLowerCase(Locale.ENGLISH).equals(COT_MAPPING_2525B_UID.toLowerCase(Locale.ENGLISH))) {
            logger.trace(COT_MAPPING_2525B_UID + " tag detected");
            return assignIcon2525B(qr);
        }

        logger.trace("custom icon iconsetUid detected");
        
        // only use the custom icon url and style if it exists in the database
        boolean iconExists = false;
        
        try {
            iconExists = (iconRepository.findIconByIconsetUidAndGroupAndName(qr.iconSetUid, qr.iconGroup, qr.iconName) != null);
        } catch (Exception e) {
            logger.warn("exception checking existence of icon " + qr.iconSetUid + " " + qr.iconGroup + " " + qr.iconName, e);
        }
        
        if (!iconExists) {
            logger.trace("icon " + qr.iconSetUid + " " + qr.iconGroup + " " + qr.iconName + " does not exist in database - using 2525B icon assignment instead");
            return assignIcon2525B(qr);
        }
        
        logger.trace("icon " + qr.iconSetUid + " " + qr.iconGroup + " " + qr.iconName + " found in database - using custom icon");
        
        // base the style identifier and url on the {iconsetUid, group, name} tuple 
        String styleUrl = qr.iconSetUid + "_" + (Strings.isNullOrEmpty(qr.iconGroup) ? "" : qr.iconGroup + "_") + qr.iconName;
        String iconUrl  = ICON_RESOURCE_PATH + "/" + qr.iconSetUid + "/" + (Strings.isNullOrEmpty(qr.iconGroup) ? "" : qr.iconGroup + "/") + qr.iconName;

        return new Association<String, String>(styleUrl, iconUrl);
    }

    //This is what a typical announcement message looks like as of Mar 17, 2015
    //<detail>
    //  <contact endpoint="192.168.3.114:4242:tcp" callsign="BARB"/>
    //  <uid Droid="BARB"/>
    //  <__group name="Cyan" role="Team Member"/>
    //  <status battery="100"/>
    //  <track speed="0.0" course="75.20256421034934"/>
    //  <precisionlocation geopointsrc="???" altsrc="???"/>
    //</detail>
    @Root(name="detail", strict=false)
    public static class DetailElement {
       @Element(name="__group", required=false)
       GroupElement group;
    }

    public static class GroupElement {
       @Attribute(required=false)
       String name;
       @Attribute(required=false)
       String role;
    }

    /*
     * assign the icon based on the CoT type to 2525B mapping, medevac, or team color
     * 
     */
    protected Association<String, String> assignIcon2525B(CotElement qr) {

        String styleUrl = "";
        String iconUrl  = "";
        
        boolean medevac = false;
        
        String group = "";

        String detailText = qr.detailtext;

        if (detailText != null && detailText.contains("_medevac_")) {
            styleUrl = "medevac";
            
            medevac = true;
        } else if (detailText != null && detailText.contains("__group")) {
           try {
            DetailElement detailElem = new Persister().read(DetailElement.class, detailText);
            if (detailElem != null && detailElem.group != null && detailElem.group.name != null) {
                
               styleUrl = detailElem.group.name.toLowerCase();
               
               // remove spaces in case the team name contains a space
               if (styleUrl != null) {
                  styleUrl = styleUrl.replaceAll(" ", "");
               }
               
               group = styleUrl;
               
            } else {
               logger.warn("No group name in XML detail: " + detailText);
            }
           } catch (Exception e) {
              logger.warn("Error parsing XML detail text: " + detailText, e);
           }
        } else {
            iconUrl = KmlUtils.get2525BIconUrl(qr.cottype, medevac, group);
            
            styleUrl = qr.cottype;
        }
        
        logger.debug("styleUrl: " + styleUrl + " iconUrl: " + iconUrl);

        return new Association<String,String>(styleUrl, iconUrl);
    }
    
}
