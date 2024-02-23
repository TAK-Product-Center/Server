

package com.bbn.cot.filter;

import java.util.ArrayList;
import java.util.List;

import com.bbn.marti.config.Configuration;
import org.apache.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Element;

import com.bbn.cot.filter.Images.ImageData;
import com.bbn.marti.config.Urladd;

import com.bbn.marti.remote.config.CoreConfigFacade;
import tak.server.cot.CotEventContainer;

/**
 * Add the urls for the image and video links like the chipper used to
 * 
 *
 * The image urls are now added to the canonical dom4j element held by the ImageData objects
 * The ImageData objects for a given message are shallow copy-constructed, and the dom element deep-copied and 
 * then annotated with the appropriate URLs
 */
public class UrlAddingFilter implements CotFilter {
	
	private static final Logger log = Logger.getLogger(UrlAddingFilter.class);

	public UrlAddingFilter() { }

	public CotEventContainer filter(CotEventContainer cot) {
		// make sure there's a database
		Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();

		// if we don't have a database, or we don't have any images
		if (!config.getRepository().isEnable() || !cot.hasContextKey(CotEventContainer.IMAGE_KEY)) {
			return cot;
		}

		// copy construct and annotate the ImageData's held element
		List<ImageData> dataList = (List<ImageData>) cot.getContext(CotEventContainer.IMAGE_KEY);
		List<ImageData> newDataList = new ArrayList(dataList.size());
		
		if (dataList != null && dataList.size() > 0) {
			for (ImageData data : dataList) {
				ImageData newData = copyImageData(data);
				
				annotateImageData(newData);
				newDataList.add(newData);
			}
			
			cot.setContext(CotEventContainer.IMAGE_KEY, newDataList);
		}

		// Add the video URL
		Element videoElem = (Element) cot.getDocument().selectSingleNode("/event/detail/__video");
		if (videoElem != null) {
			Attribute urlAttr = videoElem.attribute("url");

			// write one in if it's not there
			if (urlAttr == null) {
				videoElem.addAttribute(
					"url", config.getFilter().getUrladd().getVidscript()
						+ "?cotID="
						+ cot.getContextValue(CotEventContainer.PRIMARY_KEY));
			}
		}

		return cot;
	}
	
	/**
	* Creates a shallow copy of the image data, and then deep copies the dom4j image element (we're adding url annotations to it)
	*/
	public static ImageData copyImageData(ImageData data) {
		ImageData newData = data.copy();
		Element newImageElem = data.element().createCopy();
		newData.element(newImageElem);

		return newData;
	}

	/**
	* Adds urls for retrieving the full/thumbnail image versions from the web services side
	* by annotating (mutating) the image element
	*/
	public void annotateImageData(ImageData imageData) {
		Urladd urladd = CoreConfigFacade.getInstance().getRemoteConfiguration().getFilter().getUrladd();
		Element imageElem = imageData.element();
		
		//Construct URL
		String url = (String) urladd.getHost() + urladd.getScript() + "?imageID=" + imageData.primaryKey().toString();
		
		// overwrite url attribute if we're supposed to
		Attribute attr = imageElem.attribute("url");
		if ( (attr == null || urladd.isOverwriteurl()) && urladd.isFullurl() ) {
			imageElem.setAttributeValue("url", url + "&format=jpg");
		}

		// overwrite thumb attribute if we're supposed to
		Attribute thumbattr = imageElem.attribute("thumb");
		if ( (thumbattr == null || urladd.isOverwriteurl()) && urladd.isThumburl() ) {
			imageElem.setAttributeValue("thumb", url + "&format=thumb");
		}
	}
}
