/*
 * 
 * The PersistenceStore contains utilities for reading and writing Feeds to the database. 
 * 
 */

package com.bbn.marti.video;

import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.UUID;

import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;

import com.bbn.marti.JDBCQueryAuditLogHelper;
import com.bbn.marti.logging.AuditLogUtil;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.remote.util.SecureXmlParser;
import com.bbn.marti.video.Feed.Type;
import com.bbn.security.web.MartiValidator;


public class VideoManagerService {
	
	@Autowired
	private JDBCQueryAuditLogHelper wrapper;
	
	@Autowired
	private DataSource ds;

	@Autowired
	private VideoConnectionRepository videoConnectionRepository;

	protected static final Logger logger = LoggerFactory.getLogger(VideoManagerService.class);
		
	public boolean addFeed(Feed feed, String groupVector, Validator validator) {

    	boolean status = false;
    	
    	try {

   	    	JAXBContext jaxbContext = JAXBContext.newInstance(Feed.class);
	        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
	        StringWriter sw = new StringWriter();
        	jaxbMarshaller.marshal(feed, sw);
        	String xml = sw.toString();    		
    		
		//
		// is the feed (URL/alias) in the database already?
		//
		try (Connection connection = ds.getConnection(); PreparedStatement query = wrapper.prepareStatement(
					"select * from video_connections where uuid = ? " +
					" and deleted=false and ( groups is null or " + RemoteUtil.getInstance().getGroupClause() + ")",
				connection)) {
			query.setString(1, feed.getUuid());
			query.setString(2, groupVector);
			logger.debug(query.toString());
        	ResultSet results =  query.executeQuery();        	
        	boolean found = results.next();
    		results.close();
    		query.close();
    		
    		//
    		// add the feed if not found
    		//
    		if (!found) {
            			
            	try {
        			validator.getValidInput("feed", xml, 
        					MartiValidator.Regex.XmlBlackList.name(), MartiValidator.LONG_STRING_CHARS, true);
        			Feed.Type type = feed.getType();
        			if (type != null) {
						validator.getValidInput("feed type", type.toString(), MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
					}
        			validator.getValidInput("feed alias", feed.getAlias(), MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
        			validator.getValidInput("feed fov", feed.getFov(), MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
        			validator.getValidInput("feed heading", feed.getHeading(), MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
        			validator.getValidInput("feed range", feed.getRange(), MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
        			validator.getValidInput("feed uuid", feed.getUuid(), MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
        	    } catch (ValidationException e) {
        	        logger.error("Exception!", e);	    	
        	        return false;
        	    }            	
            	
	    		try (PreparedStatement quer = wrapper.prepareStatement("insert into video_connections (" +
		    			"owner, type, alias, " +
		    			"latitude, longitude, fov, heading, range, uuid, xml, groups)  " +
		    			"values (?,?,?,?,?,?,?,?,?,?,?" + RemoteUtil.getInstance().getGroupType() +") ", connection)) {
            	
	    		quer.setString(1, AuditLogUtil.getUsername());
	    		quer.setString(2, feed.getType() != null ? feed.getType().toString() : "VIDEO");
	    		quer.setString(3, feed.getAlias());
	    		quer.setString(4, feed.getLatitude());
	    		quer.setString(5, feed.getLongitude());
	    		quer.setString(6, feed.getFov());
	    		quer.setString(7, feed.getHeading());
	    		quer.setString(8, feed.getRange());
	    		quer.setString(9, feed.getUuid());
				quer.setString(10, xml);
				quer.setString(11, groupVector);
				logger.debug(query.toString());
            			            				    		
	    		quer.executeUpdate();
	    		}
    			        	
	        	status = true;
	        	
    		} else {
    			status = updateFeed(feed, groupVector, validator);
    		}
		}
        	
	    } catch (NamingException | SQLException e) {
	    	status = false;
	        logger.error("Exception!", e);
	    } catch (JAXBException e) {
	    	status = false;
	        logger.error("Exception!", e);
	    } finally { }
    	
    	return status;
	}
	
	public boolean updateFeed(Feed feed, String groupVector, Validator validator) {

    	boolean status = false;
    	
    	try (Connection connection = ds.getConnection(); PreparedStatement query = wrapper.prepareStatement("update video_connections set " +
    			" owner=?, type=?, alias=?, " +
    			" latitude=?, longitude=?, fov=?, heading=?, range=?, uuid=?, xml=?, groups=?" + RemoteUtil.getInstance().getGroupType() +
    			" where uuid=? and ( groups is null or " + RemoteUtil.getInstance().getGroupClause() + " )",
				connection)){
        	
   	    	JAXBContext jaxbContext = JAXBContext.newInstance(Feed.class);
	        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
	        StringWriter sw = new StringWriter();
        	jaxbMarshaller.marshal(feed, sw);
        	String xml = sw.toString();    		
        			
        	try {
    			validator.getValidInput("feed", xml, 
    					MartiValidator.Regex.XmlBlackList.name(), MartiValidator.LONG_STRING_CHARS, true);
    	    } catch (ValidationException e) {
    	        logger.error("Exception!", e);	    	
    	        return false;
    	    }            	
        	
    		query.setString(1, AuditLogUtil.getUsername());
    		query.setString(2, feed.getType() != null ? feed.getType().toString() : "VIDEO");
    		query.setString(3, feed.getAlias());
    		query.setString(4, feed.getLatitude());
    		query.setString(5, feed.getLongitude());
    		query.setString(6, feed.getFov());
    		query.setString(7, feed.getHeading());
    		query.setString(8, feed.getRange());
    		query.setString(9, feed.getUuid());
			query.setString(10, xml);
			query.setString(11, groupVector);
    		query.setString(12, feed.getUuid());
			query.setString(13, groupVector);

			logger.debug(query.toString());
        			            				    		
    		query.executeUpdate();
        	query.close();      
        	status = true;
        	
	    } catch (NamingException | SQLException e) {
	    	status = false;
	        logger.error("Exception!", e);
	    } catch (JAXBException e) {
	    	status = false;
	        logger.error("Exception!", e);
	    }
    	
    	return status;
	}

	public Feed feedFromResultSet(ResultSet results) {
		
		Feed feed = new Feed();
    	try
    	{
			int id = Integer.valueOf(results.getString("id"));
			feed.setId(id);
			feed.setType(Type.valueOf(results.getString("type")));
			feed.setAlias(results.getString("alias"));
			feed.setLatitude(results.getString("latitude"));
			feed.setLongitude(results.getString("longitude"));
			feed.setFov(results.getString("fov"));
			feed.setHeading(results.getString("heading"));
			feed.setRange(results.getString("range"));	
			feed.setUuid(results.getString("uuid"));
			
			String xml = results.getString("xml");
			Document doc = SecureXmlParser.makeDocument(results.getString("xml"));

			Feed tmpFeed = null;
	       	JAXBContext jaxbContext = JAXBContext.newInstance(Feed.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            tmpFeed = (Feed)jaxbUnmarshaller.unmarshal(doc);
			
			feed.setActive(tmpFeed.getActive());
			feed.setAddress(tmpFeed.getAddress());
			feed.setMacAddress(tmpFeed.getMacAddress());
			feed.setPort(tmpFeed.getPort());
			feed.setRoverPort(tmpFeed.getRoverPort());
			feed.setIgnoreEmbeddedKLV(tmpFeed.getIgnoreEmbeddedKLV());
			feed.setPath(tmpFeed.getPath());
			feed.setProtocol(tmpFeed.getProtocol());
			feed.setSource(tmpFeed.getSource());
			feed.setNetworkTimeout(tmpFeed.getNetworkTimeout());
			feed.setBufferTime(tmpFeed.getBufferTime());
			feed.setRtspReliable(tmpFeed.getRtspReliable());
			feed.setThumbnail(tmpFeed.getThumbnail());
			feed.setClassification(tmpFeed.getClassification());

	    } catch (SQLException e) {
	        logger.error("Exception!", e);
	    } catch (JAXBException e) {
	        logger.error("Exception!", e);
	    } 
    	
		return feed;
	}
	
    public Feed getFeed(int id, String groupVector) {
    	
    	Feed feed = null;
    	try (Connection connection = ds.getConnection(); PreparedStatement query = wrapper.prepareStatement(
    			"select * from video_connections where id = ? and " +
						" ( groups is null or " + RemoteUtil.getInstance().getGroupClause() + " )"
				, connection)) {
			query.setInt(1, id);
			query.setString(2, groupVector);
			try (ResultSet results =  query.executeQuery()) {        	
				if (results.next())
				{
					feed = feedFromResultSet(results);
				}
			}
	    	
	    } catch (NamingException | SQLException e) {
	        logger.error("Exception!", e);
	    }     
    	
    	return feed;
    }	
	
    public VideoConnections getVideoConnections(boolean onlyActive, boolean includeV2, String groupVector) {
    	
    	VideoConnections videoConnections = null;
    	try
    	{
    		String sql = "select * from video_connections where deleted=false and" +
					" ( groups is null or " + RemoteUtil.getInstance().getGroupClause() + " )";

    		if (onlyActive) {
    			sql += " and xml like '%<active>true</active>%'";
    		}

    		try (Connection connection = ds.getConnection(); PreparedStatement p = wrapper.prepareStatement(sql, connection)) {
    			p.setString(1, groupVector);
    			try (ResultSet results = wrapper.doQuery(p)) {
    				videoConnections = new VideoConnections();
    				while (results.next()) {
    					Feed feed = feedFromResultSet(results);	    		
    					videoConnections.getFeeds().add(feed);
    				}
    			}
    		}

//    		if (includeV2) {
//    			VideoCollections videoCollections = getVideoCollections(null, false, groupVector);
//    			for (VideoConnection videoConnection : videoCollections.getVideoConnections()) {
//    				for (FeedV2 feedV2 : videoConnection.getFeeds()) {
//						Feed feedV1 = new Feed(feedV2);
//						videoConnections.getFeeds().add(feedV1);
//					}
//				}
//			}

    	} catch (NamingException | SQLException e) {
    		logger.error("Exception!", e);
    	}
    	return videoConnections;
    }	
    
    public void deleteFeed(String feedId, String groupVector) {
    	
    	try (Connection connection = ds.getConnection(); PreparedStatement query = wrapper.prepareStatement(
					"update video_connections set deleted=true where id = ? and "
							+ "( groups is null or " + RemoteUtil.getInstance().getGroupClause() + " )"
				, connection)) {
			query.setInt(1, Integer.parseInt(feedId));
			query.setString(2, groupVector);
			logger.debug(query.toString());
			
        	query.executeUpdate();
        	query.close();
        	
 	    } catch (NamingException | SQLException e) {
	        logger.error("Exception!", e);
	    }       	
    }

    public void createVideoCollections(VideoCollections videoCollections, String groupVector) {
		try {
			for (VideoConnection videoConnection : videoCollections.getVideoConnections()) {
				VideoConnection existingConnection = null;
				if (videoConnection.getUuid() != null && !videoConnection.getUuid().isEmpty()) {
					existingConnection = videoConnectionRepository.getByUid(videoConnection.getUuid(), groupVector);
				} else {
					videoConnection.setUuid(UUID.randomUUID().toString());
				}

				if (existingConnection != null) {
					updateVideoConnection(videoConnection, groupVector);
				} else {
					videoConnectionRepository.create(
							videoConnection.getUuid(),
							videoConnection.getActive(),
							videoConnection.getAlias(),
							videoConnection.getThumbnail(),
							videoConnection.getClassification(),
							videoConnectionToXml(videoConnection),
							groupVector);
				}
			}
		} catch (Exception e) {
			logger.error("exception in createVideoCollections!", e);
		}
	}

	private void inflateFeedsFromXml(VideoConnection videoConnection) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(VideoConnection.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			Document doc = SecureXmlParser.makeDocument(videoConnection.getXml());
			VideoConnection tmpVideoConnection = (VideoConnection) jaxbUnmarshaller.unmarshal(doc);
			videoConnection.getFeeds().addAll(tmpVideoConnection.getFeeds());
		} catch (Exception e) {
			logger.error("exception in getVideoConnection!", e);
		}
	}

	private String videoConnectionToXml(VideoConnection videoConnection) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(VideoConnection.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			StringWriter sw = new StringWriter();
			jaxbMarshaller.marshal(videoConnection, sw);
			String xml = sw.toString();
			return xml;
		} catch (Exception e) {
			logger.error("exception in videoConnectionToXml!", e);
		}
		return null;
	}

	public VideoConnection getVideoConnection(String uid, String groupVector) {
		try {
			VideoConnection videoConnection = videoConnectionRepository.getByUid(uid, groupVector);
			if (videoConnection != null) {
				inflateFeedsFromXml(videoConnection);
			}
			return videoConnection;
		} catch (Exception e) {
			logger.error("exception in getVideoConnection!", e);
		}
		return null;
	}

	public VideoCollections getVideoCollections(String protocol, boolean includeV1, String groupVector) {
		try {
			VideoCollections videoCollections = new VideoCollections();
			videoCollections.getVideoConnections().addAll(videoConnectionRepository.get(groupVector));
			for (VideoConnection videoConnection : videoCollections.getVideoConnections()) {
				inflateFeedsFromXml(videoConnection);
			}

			if (includeV1) {
				VideoConnections videoConnectionsV1 = getVideoConnections(false, false, groupVector);
				Iterator<Feed> feedV1Iterator = videoConnectionsV1.getFeeds().iterator();
				while (feedV1Iterator.hasNext()) {
					Feed feedV1 = feedV1Iterator.next();

					VideoConnection wrapper = new VideoConnection();
					wrapper.setActive(true);
					wrapper.setUuid(feedV1.getUuid());
					wrapper.setAlias(feedV1.getAlias());
					wrapper.setThumbnail(feedV1.getThumbnail());
					wrapper.setClassification(feedV1.getClassification());

					FeedV2 wrapped = new FeedV2(feedV1);
					wrapper.getFeeds().add(wrapped);
					videoCollections.getVideoConnections().add(wrapper);
				}
			}

			if (protocol != null) {
				Iterator<VideoConnection> videoConnectionIterator = videoCollections.getVideoConnections().iterator();
				while (videoConnectionIterator.hasNext()) {
					VideoConnection videoConnection = videoConnectionIterator.next();
					Iterator<FeedV2> feedV2Iterator = videoConnection.getFeeds().iterator();
					while (feedV2Iterator.hasNext()) {
						FeedV2 feedV2 = feedV2Iterator.next();
						if (!feedV2.getUrl().startsWith(protocol)) {
							feedV2Iterator.remove();
						}
					}
					if (videoConnection.getFeeds().isEmpty()) {
						videoConnectionIterator.remove();
					}
				}
			}

			return videoCollections;
		} catch (Exception e) {
			logger.error("exception in getVideoCollections!", e);
		}
		return null;
	}

	public void updateVideoConnection(VideoConnection videoConnection, String groupVector) {
		try {
			videoConnectionRepository.update(
					videoConnection.getUuid(),
					videoConnection.getActive(),
					videoConnection.getAlias(),
					videoConnection.getThumbnail(),
					videoConnection.getClassification(),
					videoConnectionToXml(videoConnection),
					groupVector);
		} catch (Exception e) {
			logger.error("exception in updateVideoConnection!", e);
		}
	}

	public void deleteVideoConnection(String uid, String groupVector) {
		try {
			videoConnectionRepository.delete(uid, groupVector);
		} catch (Exception e) {
			logger.error("exception in deleteVideoConnection!", e);
		}
	}
}
