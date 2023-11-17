

package com.bbn.cot.filter;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.dom4j.Element;

import com.bbn.marti.remote.ImagePref;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.util.Iterables;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.bbn.marti.util.Tuple;

/**
 * This has been changed innto a static class of
 * non-mutating processing utilies for images. See the ImageProcessingFilter
 * and ImageFormattingFilter for detachment of the xml-encoded image from the
 * cot message, and then reattachment for different formats.
 *
 * The main change here is that ImageData is now tightly coupled with
 * the element that generates it. Upon construction, the ImageData object holds
 * canonical versions of the byte arrays (full and thumb), as well as the XML
 * element that generated those. The XML element is copy constructed from the
 * provided one. For presentation, the processing methods will generate a new
 * Image Element from the ImageData object, according to the given format. For
 * URL annotations, the ImageData object is partially copied, with the byte
 * array references remaining the same, and the XML element copied to avoid
 * issues associated with the URL of the marti server changing over time (ie,
 * we don't want the URL annotations to go into the database or any other long-lived
 * store, as they are only valid for the current marti session). For Database
 * presentation, the primary key assigned to the image is stored in the DOM element
 * text field (where the image was) to avoid XML ordering issues on reconstruction.
 * (the image elements point to the Cot message primary key, and each image element
 * of the Cot message points to the image primary key).
 *
 */
public class Images {
	public static final ImagePref DEFAULT_IMAGE_PREF = ImagePref.THUMBNAIL;

	// XPath values
	public static final String DETAIL_PATH = "/event/detail";
	public static final String IMAGE_PATH = "/event/detail/image";
	public static final String IMAGE_ELEMENT_NAME = "image";
	public static final String IMAGE_FORMAT = "jpg";

	private static final Logger log = Logger.getLogger(Images.class);

	/**
	* Image data structure for storing the data associated with an image
	* - full/thumbnail, decoded byte arrays
	* - generating element (private, canonical reference)
	* - primary key (database entry)
	*/
	public static class ImageData {
		private String encodedImage = null;
		private byte[] fullResImg = null;
		private byte[] thumbnail = null;
		private Element imageElem = null;
		private Integer primaryKey = null;

		// Fluent setter
		public ImageData encodedImage(String text) {
			this.encodedImage = text;
			return this;
		}

		public String encodedImage() {
			return this.encodedImage;
		}

		// fluent setter
		public ImageData fullBytes(byte[] fullRes) {
			this.fullResImg = fullRes;
			return this;
		}

		// get full bytes
		public byte[] fullBytes() {
			return this.fullResImg;
		}

		// fluent setter
		public ImageData thumbBytes(byte[] thumbnail) {
			this.thumbnail = thumbnail;
			return this;
		}

		// get thumb bytes
		public byte[] thumbBytes() {
			return this.thumbnail;
		}

		// fluent setter
		public ImageData element(Element imageElement) {
			this.imageElem = imageElement;
			return this;
		}

		// returns a reference to the mutable element held by this data wrapper
		// users should copy before modifying, as changes will be reflected internally
		public Element element() {
			return this.imageElem;
		}

		// fluent setter for the db primary key
		public ImageData primaryKey(Integer primaryKey) {
			this.primaryKey = primaryKey;
			return this;
		}

		// get primary key
		public Integer primaryKey() {
			return this.primaryKey;
		}

		// creates shallow copy of this object
		public ImageData copy() {
			return new ImageData()
				.encodedImage(this.encodedImage)
				.fullBytes(this.fullBytes())
				.thumbBytes(this.thumbBytes())
				.element(this.element())
				.primaryKey(this.primaryKey());
		}

		// preexisting code
		public byte[] getFullResImage() { return fullResImg; }
		public byte[] getThumbnail() { return thumbnail; }
		public Element getElement() {
			Assertion.notNull(imageElem);
			return imageElem;
		}
		public Integer getPrimaryKey() {
			Assertion.notNull(primaryKey);
			return primaryKey;
		}
	}

	/*
	* Pure (side effect free) conversion from the given element to an ImageData wrapper.
	*
	* The image text is decoded into a byte[], which is transformed into a thumb byte[] as well.
	* A copy of the given element is stored with the data into an ImageData object. The given element
	* becomes the canonical version of that element, generating all later formats of the image.
	*/
	public static ImageData imageDataFromElement(Element imageElem) {
		ImageData imageData = null;
		try {
			// decode data from element text, clear string
			String rawDataString = imageElem.getText();
			byte[] rawData = decodeImageData(rawDataString);

			// This read doubles a quick check to ensure the image is well-formed. -AG
			// We use rawData downstream to avoid re-encoding the image unnecessarily, which would strip exif headers.
			BufferedImage fullImg = ImageIO.read(new ByteArrayInputStream(rawData));
			// creating the thumbnail does involve re-encoding and stripping the exif headers. -AG
			BufferedImage thumbImg = createThumbNail(fullImg);

			// write out to an array
			ByteArrayOutputStream thumbStream = new ByteArrayOutputStream();
			ImageIO.write(thumbImg, IMAGE_FORMAT, thumbStream);

			// create copy (obtuse logic with temporarily stripping out the text, we want to avoid potentially allocating
			// another text node with the encoded image data copied, though this might not be the case)
			imageElem.setText("");
			Element clonedElem = imageElem.createCopy();
			imageElem.setText(rawDataString);

			// create ImageData wrapper
			imageData = new ImageData()
				.encodedImage(rawDataString)
				.fullBytes(rawData)
				.thumbBytes(thumbStream.toByteArray())
				.element(clonedElem);

		} catch (Exception e) {
			log.warn("  Image Conversion error: " + e.getMessage(), e);
		}

		return imageData;
	}

	/**
	* Generates an element formatted for the given Image Preference
	*/
	public static Element generateElementFromData(ImageData imageData, ImagePref pref) {
		Assertion.notNull(pref);
		Element newImageElem = imageData.element().createCopy();

		switch (pref) {
			case FULL_IMAGE:
				// Use the original, base64-encoded String if we have it
				if (imageData.encodedImage != null) {
					newImageElem.setText(imageData.encodedImage);
				} else {
					log.debug("Encoded String not available; re-encoding.");
					setContent(newImageElem, imageData.fullResImg);
				}
				break;
			case THUMBNAIL:
				setContent(newImageElem, imageData.getThumbnail());
				break;
			case URL_ONLY:
				break;
			case DATABASE:
				Assertion.notNull(imageData.getPrimaryKey());
				setContent(newImageElem, imageData.getPrimaryKey());
				break;
			default:
				// DEBUG line
				Assertion.fail();
		}

		return newImageElem;
	}

	/**
	* A mutator method to set the image text to the given byte array, encoding the image data in the process
	*/
	public static void setContent(Element imageElem, byte[] imageData) {
		String imageText = encodeImageData(imageData);
		imageElem.setText(imageText);
	}

	/**
	* Formats the given element to have the database primary key in the text field of the element
	*/
	public static void setContent(Element imageElem, Integer primaryKey) {
		imageElem.setText(primaryKey.toString());
	}

	/**
	* Converts the unencoded byte array to an encoded xml text string
	*/
	public static String encodeImageData(byte[] unencodedData) {
		byte[] encodedData = Base64.encodeBase64(unencodedData);
		return new String(encodedData);
	}

	/**
	* Converts the encoded xml text string to a byte[]
	*/
	public static byte[] decodeImageData(String imageText) {
		byte[] decodedData = Base64.decodeBase64(imageText.getBytes());
		return decodedData;
	}

	/**
	* Tries to parse an integer out of the element text (primary key of the image should be
	* stored in the element text)
	*/
	public static Integer parseTextToPrimaryKey(Element imageElem) {
		Integer primaryKey = null;
		String primaryText = imageElem.getText();

		if (!primaryText.equals("")) {
			// backward compatibility, will read databases
			try {
				primaryKey = Integer.valueOf(primaryText);
			} catch (NumberFormatException e) {
				log.error("Error reading image element text: couldn't parse to an integer");
			}
		}

		return primaryKey;
	}

	/**
	* Tries to parse a group of images for a specific cot message from the database.
	*
	* The imageMap is an *ordered map* (insertion ordering) primary key (image database) -{@literal >} full/thumb byte arrays,
	* and the imageElems list is the list of image elements in the order as retrieved by the database, where the image
	* element text should contain the matching image data primary key.
	*
	* This method attempts to pair image elements with the image data by parsing the image element text and looking
	* in the image map. If no element exists, we default to the zipped ordering
	*/
	public static List<ImageData> parseDatabaseFormat(Map<Integer,ImageData> imageMap, List<Element> imageElems) {
		// need to maintain backward compatibility... don't ask
		List<ImageData> dataList = new ArrayList<ImageData>(imageElems.size());

		for (Tuple<ImageData,Element> tuple : Iterables.zip(imageMap.values(),imageElems)) {
			// try to match with the Element text -> primary key, or fall back on zipped ordering
			Element imageElem = tuple.right();
			Integer imageKey = parseTextToPrimaryKey(imageElem);
			ImageData toAssociate;

			if (imageKey == null || !imageMap.containsKey(imageKey)) {
				// primary key couldn't be parsed, or we don't have an image with that pk
				log.debug("Couldn't find any images to pair based on primary key -- associating by arbitrary found order");
				toAssociate = tuple.left();
			} else {
				// parsed primary key matches
				log.debug("Correctly paired image data with original element for " + imageKey + " key");
				toAssociate = imageMap.get(imageKey);
			}

			// associate matched data with a clone of the element, add to dataList
			dataList.add(toAssociate
				.element(imageElem.createCopy())
			);

			// remove the element from the xml tree -- will not affect list membership w/dom4j
			imageElem.detach();
		}

		return dataList;
	}

	/**
	 * Creates a thumbnail-sized image from a full-size image
	 */
	private static BufferedImage createThumbNail(BufferedImage fullSizeImg)
			throws Exception {
		int width = fullSizeImg.getWidth();
		int height = fullSizeImg.getHeight();
		if (width <= 0 || height <= 0) {
			throw new Exception(
					"At least one dimension is invalid.  No image writen to message.");
		}
		int pixels = DistributedConfiguration.getInstance().getFilter().getThumbnail().getPixels();
		if (width > height) { // Ensure ratio is kept
			height = (int) ((double) (height) / (width) * pixels);
			width = pixels;
		} else {
			width = (int) ((double) (width) / (height) * pixels);
			height = pixels;
		}
		BufferedImage thumb = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_BGR);
		Graphics2D g = thumb.createGraphics();
		g.drawImage(fullSizeImg, 0, 0, width, height, null); // Shrink Image
		g.dispose();
		return thumb;
	}
}
