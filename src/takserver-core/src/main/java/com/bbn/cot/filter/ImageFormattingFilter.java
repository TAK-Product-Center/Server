

package com.bbn.cot.filter;

import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.bbn.cot.filter.Images.ImageData;
import com.bbn.marti.remote.ImagePref;
import com.bbn.marti.util.Assertion;

import tak.server.cot.CotEventContainer;

public class ImageFormattingFilter implements CotFilter {
	private static final Logger log = Logger.getLogger(ImageFormattingFilter.class);
	private final ImagePref pref;

	public ImageFormattingFilter() {
		this(ImagePref.FULL_IMAGE);
	}
	
	public ImageFormattingFilter(ImagePref pref) {
		this.pref = pref;
	}
	
	/**
	* Defaults to full image preference
	*/
	public CotEventContainer filter(CotEventContainer cot) {
		return filter(cot, pref);
	}

	/**
	* Filter for a specific preference type
	*/
	public static CotEventContainer filter(CotEventContainer cot, ImagePref pref) {
		Assertion.notNull(pref);
		
		// get image list out
		List<ImageData> imageList = (List<ImageData>) cot.getContext(CotEventContainer.IMAGE_KEY);
		
		// predicate on these conditions
		if (imageList == null || 
			imageList.size() == 0) {
			return cot;
		}

		log.trace("reattaching " + imageList.size() + " to the cot, with format " + pref.toString());
		
		// get detail element out of document for attaching child images
		Element detailElem = DocumentHelper.makeElement(cot.getDocument(), Images.DETAIL_PATH);
		
		// remove any existing image elements
		List<Element> imageElems = (List<Element>) detailElem.elements(Images.IMAGE_ELEMENT_NAME);
		if (imageElems.size() > 0) {
			log.warn("Trying to format cot message for images that already has image elements present -- stripping them out");
			for (Element imageElem : imageElems) {
				imageElem.detach();
			}
		}
		
		// attach image string data for each image
		for (ImageData img : imageList) {
			Element imageElem = Images.generateElementFromData(img, pref);
			if (imageElem != null) {
				detailElem.add(imageElem);
			}
		}

		return cot;
	}
}