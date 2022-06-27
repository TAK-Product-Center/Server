package com.bbn.marti.test.shared;

import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.engines.UserIdentificationData;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tak.server.cot.CotEventContainer;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This copies a lot of stuff from CotSAFactory. I didn't pull that into the project since other stuff in the same directory needs additional unnecessary imports
 * <p>
 * Created on 10/1/15.
 */

public class CotGenerator {

	private static class UserIdentificationPair {
		String uid;
		String callsign;

		public UserIdentificationPair(String uid, String callsign) {
			this.uid = uid;
			this.callsign = callsign;
		}
	}


	public static final String DEFAULT_ERROR = "9999999";
	public static final String DEFAULT_GROUP = "Cyan";
	public static final String DEFAULT_HOW = "h-g-i-g-o";
	public static final String DEFAULT_TYPE = "a-f-G-U-C";
	public static final String PROTOBUF_UPGRADE_TYPE = "t-x-takp-v";

	private static final String imageBase64String = "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wAARCAAGABADASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD+ffw1+3sNR8CJ4x8SfCn4e6VqFzq/xD8F6Douk+Etd8R6Jr/iHw7a/CzWV1bxHc3vxU8Lan4Wi0rR/HEy6ZPpEviG21PUUa11bw3BBHHqj5Ph/wD4KMaN4ah8ZJ8Tf2cfh14uTXfBGq6J4B1Dw0+ueHLnwf8AESXU9E1zSvHGr6fea5rEXijQ9Og0rU/Bs/ggahprX/h7xTfa8/ieLxX4e0G6mKK7aHGfGNTKOJMZPiviF4ihmWHVCf8AbWZKNKPs6GPcKVJYpUoU3iG70owVN4dvAuDwP7h+TPhvhyGY5ThY5Bk3sauCqqrF5XgZSqNYjFYRTlVlh3VdRUYRtUc/aKqliedYpe2f/9k=";


	// Rough mostly-continental chunk of the US
	public static final double MAX_RANDOM_LAT = 48.562068;
	public static final double MIN_RANDOM_LAT = 30.755641;
	public static final double MAX_RANDOM_LON = -81.490127;
	public static final double MIN_RANDOM_LON = -122.387100;


	public static final int DEFAULT_STALE_HOURS = 24;

	public static Document createAuthMessage(String username, String password, String uid) {
		Document doc = DocumentHelper.createDocument();

		Element authElem = DocumentHelper.createElement("auth");
		doc.setRootElement(authElem);

		Element cotElem = DocumentHelper.createElement("cot");
		authElem.add(cotElem);
		cotElem.addAttribute("username", username)
				.addAttribute("password", password)
				.addAttribute("uid", uid);

		return doc;
	}

	public static Document createMessage(String uid) {
		// nextDouble max is exclusive, but it doesn't matter for this...
		return createMessage(uid,
				ThreadLocalRandom.current().nextDouble(MIN_RANDOM_LAT, MAX_RANDOM_LAT),
				ThreadLocalRandom.current().nextDouble(MIN_RANDOM_LON, MAX_RANDOM_LON));
	}

	private static Document createMessage(String uid, double currentLatitude, double currentLongitude) {
		Document cot = DocumentHelper.createDocument();
		Element eventElement = cot.addElement("event")
				.addAttribute("version", "2.0")
				.addAttribute("type", DEFAULT_TYPE)
				.addAttribute("how", DEFAULT_HOW);

		// Set start, stale, and time values to something sensible
		LocalDateTime time = LocalDateTime.now();
		LocalDateTime stale = time.plusHours(DEFAULT_STALE_HOURS);

		if (uid != null) {
			eventElement.addAttribute("uid", uid);
		}
		eventElement.addAttribute("time", time.toString() + "Z");
		eventElement.addAttribute("start", time.toString() + "Z");
		eventElement.addAttribute("stale", stale.toString() + "Z");

		eventElement.addElement("point")
				.addAttribute("lat", Double.toString(currentLatitude))
				.addAttribute("lon", Double.toString(currentLongitude))
				.addAttribute("hae", DEFAULT_ERROR)
				.addAttribute("ce", DEFAULT_ERROR)
				.addAttribute("le", DEFAULT_ERROR);

		eventElement.addElement("detail");

		return cot;
	}

	public static Document createLatestSAMessage(@NotNull String userNameUid, @NotNull String endpoint, @NotNull String group) {
		return innerCreateMessageWithCallsign(userNameUid,
				ThreadLocalRandom.current().nextDouble(MIN_RANDOM_LAT, MAX_RANDOM_LAT),
				ThreadLocalRandom.current().nextDouble(MIN_RANDOM_LON, MAX_RANDOM_LON),
				userNameUid,
				endpoint,
				false,
				null,
				null);
	}

	public static Document createLatestSAMessage(AbstractUser user) {
		return createLatestSAMessage(UserIdentificationData.UID_AND_CALLSIGN, user, UserIdentificationData.UID_AND_CALLSIGN, false, null);
	}

	public static Document createLatestSAMessage(@NotNull UserIdentificationData providedSenderData, @NotNull AbstractUser sourceUser, @NotNull UserIdentificationData providedRecipientData, boolean sendImage, @Nullable String missionName, @Nullable AbstractUser... targetClients) {
		UserIdentificationPair[] identificationPairs = null;

		if (targetClients != null && targetClients.length > 0) {
			identificationPairs = new UserIdentificationPair[targetClients.length];

			for (int i = 0; i < targetClients.length; i++) {
				AbstractUser client = targetClients[i];
				if (providedRecipientData == UserIdentificationData.UID_AND_CALLSIGN) {
					identificationPairs[i] = new UserIdentificationPair(client.getCotUid(), client.getCotCallsign());
				} else if (providedRecipientData == UserIdentificationData.UID) {
					identificationPairs[i] = new UserIdentificationPair(client.getCotUid(), null);
				} else if (providedRecipientData == UserIdentificationData.CALLSIGN) {
					identificationPairs[i] = new UserIdentificationPair(null, client.getCotCallsign());
				}
			}
		}

		return innerCreateMessageWithCallsign(
				((providedSenderData == UserIdentificationData.UID || providedSenderData == UserIdentificationData.UID_AND_CALLSIGN) ? sourceUser.getCotUid() : null),
				ThreadLocalRandom.current().nextDouble(MIN_RANDOM_LAT, MAX_RANDOM_LAT),
				ThreadLocalRandom.current().nextDouble(MIN_RANDOM_LON, MAX_RANDOM_LON),
				((providedSenderData == UserIdentificationData.CALLSIGN || providedSenderData == UserIdentificationData.UID_AND_CALLSIGN) ? sourceUser.getCotCallsign() : null),
				sourceUser.toString(),
				sendImage,
				identificationPairs,
				missionName);
	}

	public static Document createLatestSAMessageWithImage(AbstractUser user) {
		Document doc = createLatestSAMessage(UserIdentificationData.UID_AND_CALLSIGN, user, UserIdentificationData.UID_AND_CALLSIGN, false, null);

		Element root = doc.getRootElement();
		Element detail = root.element("detail");

		if (detail == null) {
			detail = root.addElement("detail");
		}

		Element imageElement = detail.addElement("image");
		imageElement.addAttribute("height", "200");
		imageElement.addAttribute("width", "200");
		imageElement.addAttribute("mime", "image/jpeg");
		imageElement.addAttribute("resolution", "1");
		imageElement.addAttribute("type", "EO");
		imageElement.addAttribute("size", String.valueOf(imageBase64String.length()));
		imageElement.setText(imageBase64String);

		return doc;
	}

	public static Document createMessage(AbstractUser user) {
		return createMessage(user.getCotUid());
	}

	/**
	 * Creates a CoT Situational-Awareness (SA) message that includes
	 *
	 * @param uid          The uid
	 * @param latitude     The latitiude
	 * @param longitude    The longitude
	 * @param callsign     The callsign
	 * @param endpoint     THe endpoint (callsign)
	 * @param includeImage Include an image?
	 * @return a well-formed CoT event
	 */
	private static Document innerCreateMessageWithCallsign(String uid,
	                                                       double latitude,
	                                                       double longitude,
	                                                       String callsign,
	                                                       String endpoint,
	                                                       boolean includeImage,
	                                                       @Nullable UserIdentificationPair[] destinationIdentification,
	                                                       @Nullable String missionName) {

		Document cot = createMessage(uid, latitude, longitude);
		Element rootElement = cot.getRootElement();
		for (Iterator itr = rootElement.elementIterator("detail"); itr.hasNext(); ) {
			Element detailElement = (Element) itr.next();

			if (callsign != null || endpoint != null) {
				Element contactElement = detailElement.addElement("contact");

				if (callsign != null) {
					contactElement.addAttribute("callsign", callsign);
				}

				if (endpoint != null) {
					contactElement.addAttribute("endpoint", endpoint);
				}
			}

			if (includeImage) {
				Element imageElement = detailElement.addElement("image");
				imageElement.addAttribute("height", "200");
				imageElement.addAttribute("width", "200");
				imageElement.addAttribute("mime", "image/jpeg");
				imageElement.addAttribute("resolution", "1");
				imageElement.addAttribute("type", "EO");
				imageElement.addAttribute("size", String.valueOf(imageBase64String.length()));
				imageElement.setText(imageBase64String);
			}

			Element groupElement = detailElement.addElement("__group");

			if (destinationIdentification != null || missionName != null) {
				Element martiElement = detailElement.addElement("marti");

				if (missionName != null) {
					Element destElement = martiElement.addElement("dest");
					destElement.addAttribute("mission", missionName);
				}

				if (destinationIdentification != null) {
					for (UserIdentificationPair userIdentificationPair : destinationIdentification) {
						Element destElement = martiElement.addElement("dest");
						if (userIdentificationPair.uid != null) {
							destElement.addAttribute("uid", userIdentificationPair.uid);
						}

						if (userIdentificationPair.callsign != null) {
							destElement.addAttribute("callsign", userIdentificationPair.callsign);
						}
					}
				}
			}
		}
		return cot;
	}

	public static Document createAuthMessage(String usernameColonPasswd, String uid) {
		if (usernameColonPasswd != null) {
			String[] split = usernameColonPasswd.split(":");

			String username = split[0];
			String passwd = split[1];

			return createAuthMessage(username, passwd, uid);
		} else {
			return null;
		}
	}


//                if (callsign != null || endpoint != null) {
//        Element contactElement = detailElement.addElement("contact");
//
//        if (callsign != null) {
//            contactElement.addAttribute("callsign", callsign);
//        }
//
//        if (endpoint != null) {
//            contactElement.addAttribute("endpoint", endpoint);
//        }
//    }
//
//    Element groupElement = detailElement.addElement("__group");
//
//            if (destinationIdentification != null) {
//        Element martiElement = detailElement.addElement("marti");
//
//        for (UserIdentificationPair userIdentificationPair : destinationIdentification) {
//            Element destElement = martiElement.addElement("dest");
//
//            if (userIdentificationPair.uid != null) {
//                destElement.addAttribute("uid", userIdentificationPair.uid);
//            }
//
//            if (userIdentificationPair.callsign != null) {
//                destElement.addAttribute("callsign", userIdentificationPair.callsign);
//            }
//        }
//    }

	public static String parseCallsign(String cotMessage) {
		try {
			Document doc = DocumentHelper.parseText(cotMessage);
			return parseCallsign(doc);
		} catch (DocumentException e) {
			e.printStackTrace();
			System.err.println(cotMessage);
			throw new RuntimeException(e);
		}
	}

	public static String parseType(String cotMessage) {
		try {
			Document doc = DocumentHelper.parseText(cotMessage);
			Element event = doc.getRootElement();
			return event.attribute("type").getStringValue();

		} catch (DocumentException e) {
			e.printStackTrace();
			System.err.println(cotMessage);
			throw new RuntimeException(e);
		}
	}

	public static String parseCallsign(Document doc) {
		Element event = doc.getRootElement();

		Element detail = event.element("detail");

		if (detail != null) {
			Element group = detail.element("__group");
			if (group != null) {
				Element marti = group.element("marti");
				if (marti != null) {
					Element dest = marti.element("dest");
					if (dest != null) {
						String callsign = dest.attributeValue("callsign");
						if (callsign != null) {
							return callsign;
						}
					}
				}
			}

			Element contact = detail.element("contact");
			if (contact != null) {
				String endpoint = contact.attributeValue("callsign");
				if (endpoint != null) {
					return endpoint;
				}
			}
		}
		return null;
	}

	public static boolean isEmpty(String cotMessage) {
		try {
			Document doc = DocumentHelper.parseText(cotMessage);
			CotEventContainer cot = new CotEventContainer(doc);
			return (cot.getLat() == null || cot.getLat().equals("") || cot.getLat().equals("0.0")) &&
					(cot.getLon() == null || cot.getLon().equals("") || cot.getLon().equals("0.0")) &&
					(cot.getCallsign() == null || cot.getCallsign().equals("")) &&
					(cot.getUid() == null || cot.getUid().equals("")) &&
					(cot.getType() == null || cot.getType().equals("")) &&
					(cot.getHow() == null || cot.getHow().equals("")) &&
					(cot.getHae() == null || cot.getHae() == 0 || cot.getHae() == 0.0) &&
					(cot.getCe() == null || cot.getCe() == 999999 || cot.getCe() == 0.0) &&
					(cot.getLe() == null || cot.getLe() == 0 || cot.getLe() == 0.0);
		} catch (DocumentException e) {
			throw new RuntimeException(e);
		}
	}

	public static String parseClientUID(String cotMessage) {
		try {
			Document doc = DocumentHelper.parseText(cotMessage);
			return parseClientUID(doc);
		} catch (DocumentException e) {
			e.printStackTrace();
			System.err.println(cotMessage);
			throw new RuntimeException(e);
		}
	}

	public static String parseClientUID(Document doc) {
		Element event = doc.getRootElement();

		// TODO: Should I also check __group.marti.dest.uid here?

		Element detail = event.element("detail");
		if (detail != null) {
			Element link = detail.element("link");
			if (link != null) {
				String uid = link.attributeValue("uid");
				if (uid != null) {
					return uid;
				}
			}

			Element mission = detail.element("mission");
			if (mission != null) {
				String authorUid = mission.attributeValue("authorUid");
				if (authorUid != null) {
					return authorUid;
				}
			}
		}

		return event.attributeValue("uid");
	}

	public static String parseEndpoint(String cotMessage) {
		try {
			Document doc = DocumentHelper.parseText(cotMessage);

			Element event = doc.getRootElement();

			Element detail = event.element("detail");
			if (detail != null) {
				Element contact = detail.element("contact");
				if (contact != null) {
					String endpoint = contact.attributeValue("endpoint");
					if (endpoint != null) {
						return endpoint;
					}
				}
			}

			return null;

		} catch (DocumentException e) {
			e.printStackTrace();
			System.err.println(cotMessage);
			throw new RuntimeException(e);
		}
	}

	public static List<String> parseMessages(String receivedData) {
		try {
			List<String> returnList = new LinkedList<>();

			String[] stringList;

			if (receivedData.contains("\n")) {
				stringList = receivedData.split("\n");
			} else {
				stringList = new String[]{receivedData};
			}

			for (String string : stringList) {

				if (string.startsWith("<?xml")) {
					string = string.substring(string.indexOf("?>") + 2);
				} else if (string.endsWith("?>")) {
					string = string.substring(0, string.lastIndexOf("<?xml"));
				}
				if (string != null && !string.isEmpty()) {
					Document doc = DocumentHelper.parseText(string);
					returnList.add(doc.asXML());
				}
			}

			return returnList;
		} catch (DocumentException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static String createAuthPayload(@Nullable String username, @Nullable String password, @Nullable String uid, @Nullable String xmlData) {
		String authString = null;

		if (username != null || password != null || uid != null) {
			authString = CotGenerator.createAuthMessage(username, password, uid).asXML();
		}

		if (authString == null) {
			if (xmlData == null) {
				throw new RuntimeException("Cannot create a payload with incomplete authentication data (username, password, uid) and no xml Data!");

			} else {
				return xmlData;
			}

		} else {
			if (xmlData == null) {
				return authString;
			} else {
				return authString + xmlData;
			}

		}
	}

	public static String stripXmlDeclaration(Document doc) {
		try {
			OutputFormat format = new OutputFormat();
			format.setSuppressDeclaration(true);
			StringWriter sw = new StringWriter();
			XMLWriter writer = new XMLWriter(sw, format);
			writer.write(doc);
			writer.flush();
			writer.close();
			return sw.toString();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}
