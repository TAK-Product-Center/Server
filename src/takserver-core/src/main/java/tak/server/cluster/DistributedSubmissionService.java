package tak.server.cluster;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.NavigableSet;
import java.util.UUID;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.cot.CotParserCreator;
import com.bbn.cot.filter.StreamingEndpointRewriteFilter;
import com.bbn.marti.remote.SubmissionInterface;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.User;

import tak.server.Constants;
import tak.server.cot.CotEventContainer;
import tak.server.cot.CotParser;
import tak.server.messaging.Messenger;

public class DistributedSubmissionService implements SubmissionInterface {
	
	private static final Logger logger = LoggerFactory.getLogger(DistributedSubmissionService.class);
	
	private ThreadLocal<CotParser> parser = new ThreadLocal<>(); // One CotParser per worker thread
	
	@Autowired
	Messenger<CotEventContainer> cotMessenger;

	public DistributedSubmissionService() { }

	@Override
	public boolean submitCot(String cotMessage, NavigableSet<Group> groups) {
		
        return submitCot(cotMessage, groups, true);
	}

	@Override
	public boolean submitCot(String cotMessage, NavigableSet<Group> groups, boolean federate) {
		
		if (logger.isTraceEnabled()) {
			logger.trace("submitCot string, groups, federate");
		}
		
		
		return submitCot(cotMessage, groups, true, null);
	}

	@Override
	public boolean submitCot(String cotMessage, NavigableSet<Group> groups, boolean federate, User user) {
		
		requireNonNull(groups, "submitCot groups");

        try {
        	
            CotEventContainer cot = new CotEventContainer(getCotParser().parse(cotMessage));

            // only set groups, not the user
            cot.setContext(Constants.GROUPS_KEY, groups);

            if (!federate) {
                // only set groups, not the user
                cot.setContext(Constants.NOFEDV2_KEY, "true");
            }

            if (user != null) {
                cot.setContext(Constants.USER_KEY, user);
            }
            
    		if (logger.isTraceEnabled()) {
    			logger.trace("sending to ignite");
    		}
            
    		// send message
    		cotMessenger.send(cot);

        } catch (Exception e) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("exception submitting CoT", e);
        	}
        }
        return false;
	}

	@Override
    public boolean submitMissionPackageCotAtTime(String cotMessage, UUID missionGuid, Date timestamp, NavigableSet<Group> groups, String clientUid) {
		requireNonNull(groups, "submitMissionCotAtTime groups");

		try {
            CotEventContainer cot = new CotEventContainer(getCotParser().parse(cotMessage));

			cot.setContext(Constants.GROUPS_KEY, groups);

			if (clientUid != null) {
				cot.setContext(Constants.CLIENT_UID_KEY, clientUid);
			}

			cot.setContext(Constants.OFFLINE_CHANGE_TIME_KEY, timestamp);
			
			// If the incoming message has any mission dests, remove them and 
			List<Node> missionDestNodes = cot.getDocument().selectNodes("/event/detail/marti/dest[@mission]");
			
			if (!missionDestNodes.isEmpty()) {
				for (Node node : missionDestNodes) {
					node.detach();
				}
			}
			
			// insert mission-guid dest that is more specific, instead
			String dest = "<dest mission-guid=\"" + missionGuid.toString() + "\"/>";
			SAXReader reader = new SAXReader();
			Document doc = reader.read(new ByteArrayInputStream(dest.getBytes(StandardCharsets.UTF_8)));
			Element missionDestElem = DocumentHelper.makeElement(cot.getDocument(), "/event/detail/marti/");
			missionDestElem.add(doc.getRootElement());

			Node flowTags = cot.getDocument().selectSingleNode("/event/detail/_flow-tags_");
			if (flowTags != null) {
				flowTags.detach();
			}

    		// send message
    		cotMessenger.send(cot);

			return true;
		} catch (Exception e) {
			logger.error("Exception is: ", e);
			logger.debug("exception submitting mission CoT", e);
		}
		
		return false;
	}
	
	@Override
	public boolean submitCot(String cotMessage, List<String> uids, List<String> callsigns, NavigableSet<Group> groups, boolean federate, boolean resubmission) {
        try {
            return submitCot(new CotEventContainer(getCotParser().parse(cotMessage)), uids, callsigns, groups, federate, resubmission);
        } catch (Exception e) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("exception submitting message", e);
        	}
        }
        
        return false;
	}
	
	@Override
	public boolean submitCot(CotEventContainer cot, List<String> uids, List<String> callsigns, NavigableSet<Group> groups, boolean federate, boolean resubmission) {
		requireNonNull(uids, "submitCot uids");
		requireNonNull(callsigns, "submitCot callsigns");
		requireNonNull(groups, "submitCot groups");

		try {
			if (logger.isDebugEnabled()) {
				logger.debug("CoT message submitted: " + cot + " for uids: " + uids + " - callsigns: " + callsigns
						+ " - groups: " + groups);
			}

			if (!callsigns.isEmpty() && !callsigns.contains("All Streaming")) {
				cot.setContext(StreamingEndpointRewriteFilter.EXPLICIT_CALLSIGN_KEY, callsigns);
			}

			if (!uids.isEmpty()) {
				cot.setContext(StreamingEndpointRewriteFilter.EXPLICIT_UID_KEY, uids);
			}

			// only set groups, not the user
			cot.setContext(Constants.GROUPS_KEY, groups);

			if (!federate) {
				// only set groups, not the user
				cot.setContext(Constants.NOFEDV2_KEY, "true");
			}

			if (resubmission) {
				// trim off existing flow tags so we can resend the message
				Node flowTags = cot.getDocument().selectSingleNode("/event/detail/_flow-tags_");
				if (flowTags != null) {
					flowTags.detach();
				}

				// turn off message archiving so we dont save the message again
				cot.setContext(Constants.ARCHIVE_EVENT_KEY, Boolean.FALSE);
			}

			// send message
			cotMessenger.send(cot);

			return true;

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception submitting message", e);
			}
		}
		return false;
	}

	private CotParser getCotParser() {
		if (parser.get() == null) {
			
			if (logger.isDebugEnabled()) {
        		logger.debug("instantiating CotParser for thread");
        	}
			
			parser.set(CotParserCreator.newInstance());
		}
		
		return parser.get();
	}	
}
