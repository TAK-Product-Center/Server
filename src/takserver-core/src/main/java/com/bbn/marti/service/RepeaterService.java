

package com.bbn.marti.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.XPath;

import com.bbn.marti.config.Repeater;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.util.DateUtil;
import com.bbn.marti.repeater.DistributedRepeaterManager;
import com.bbn.marti.util.Tuple;

import tak.server.Constants;
import tak.server.cot.CotEventContainer;

/**
 * This service acts as a repeater for certain messages. Any messages added to this service
 * will be repeated out to all interested parties at regular intervals. This service will
 * adapt itself to the configuration specified in the config file. Those parameters are:
 * 
 * KEY						| TYPE		| DEFAULT	| DESCRIPTION
 * --------------------------------------------------------------------------------------------------------------------
 * enable					| Boolean 	| false		| determines if this service will be started or not
 * maxAllowedRepeatables	| Integer 	| int max	| the maximum number of messages that this service will manage
 * periodMillis				| Integer	| 10000		| the period in milliseconds in which this service will initiate dissemination
 * 
 * In addition the repeater node can contain 0 or more sub-nodes of the form:
 * 	<repeatableType initiate-test="/event/detail/initiate-repeat" cancel-test="/event/detail/cancel" _name="TestType1"/>
 * Each such sub-node will specify the test that must be satisfied by an incoming message in order to initiate repeating
 * and the test that must be satisfied in order to cancel an already repeating message.
 * 
 *
 */

public class RepeaterService extends BaseService {
	// ** statics
	private static final Logger LOGGER = Logger.getLogger(RepeaterService.class);
	private static final String SERVICE_NAME = "RepeaterService";
	private static final String CONFIG_KEY_BASE = "repeater.";
	private static final String[] DEFAULT_TYPES = { "/event/detail/emergency[@type='In Contact']", "/event/detail/emergency[@type='Alert']" };
	private static final String[] DEFAULT_NAMES = { "In Contact", "Alert" };

	// ** internal logic
	private DistributedRepeaterManager repeaterManager;
	private BrokerService brokerService;
	private CoreConfig coreConfig;
	private GroupManager groupManager;
	
	// ** configuration driven
	private Boolean active = Boolean.TRUE;
	private Integer maxAllowedRepeatables;
	private Map<String, Tuple<String, String>> repeatableTypes = new HashMap<String, Tuple<String, String>>();
	private ThreadLocal<HashMap<String, XPath>> xpathMap = new ThreadLocal<HashMap<String, XPath>>();

	public RepeaterService(CoreConfig coreConfig, BrokerService brokerService, GroupManager groupManager, DistributedRepeaterManager repeaterMgr) {
		this.brokerService = brokerService;
		this.coreConfig = coreConfig;
		this.groupManager = groupManager;
		this.repeaterManager = repeaterMgr;

		Repeater repeater = DistributedConfiguration.getInstance().getRepeater();
		maxAllowedRepeatables = repeater.getMaxAllowedRepeatables();
		repeaterManager.setPeriodMillis(repeater.getPeriodMillis());

		loadRepeatableTypesFromConfig();
		loadBuildItRepeatableTypes();
		initiatePeriodicExecution();
		
		LOGGER.debug("Repeater Service created [maxAllowedRepeatables=" + maxAllowedRepeatables + ", periodMillis=" + repeaterManager.getPeriodMillis() + "]");
	}

	private void loadRepeatableTypesFromConfig() {
		List<Repeater.RepeatableType> repeatableTypeObjects = coreConfig.getRemoteConfiguration().getRepeater().getRepeatableType();
		for(Repeater.RepeatableType repeatableType : repeatableTypeObjects) {
			String initiateTest = repeatableType.getInitiateTest();
			String cancelTest = repeatableType.getCancelTest();
			Tuple<String, String> initiateAndCancelTests = new Tuple<String, String>(initiateTest, cancelTest);
			repeatableTypes.put(repeatableType.getName(),initiateAndCancelTests);
		}
	}

	private void loadBuildItRepeatableTypes() {
		for (int i = 0; i < DEFAULT_TYPES.length; i++) {
			Tuple<String, String> initiateAndCancelTests = new Tuple<String, String>(DEFAULT_TYPES[i], "/event[@type='b-a-o-can']");
			repeatableTypes.put(DEFAULT_NAMES[i], initiateAndCancelTests);
		}
	}

	/**
	 * Start the scheduled executions. Should only need to be called once at service startup. To pause/resume
	 * execution while the service is running use setActive(boolean).
	 */
	private void initiatePeriodicExecution() {
		final Runnable periodicTask = new Runnable() {
			
			@Override
			public void run() {
				try {
					executePeriodicTask();
				} catch (Exception e) {
					LOGGER.debug("Exception occurred while processing periodic execution of repeater service.", e);
				} finally {
					initiatePeriodicExecution();
				}
			}
		};
		
		//scheduler.scheduleAtFixedRate(periodicTask, 0, repeaterManager.getPeriodMillis(), TimeUnit.MILLISECONDS);
		Resources.repeaterPool.schedule(periodicTask, repeaterManager.getPeriodMillis(), TimeUnit.MILLISECONDS);
	}

	@Override
	public boolean hasRoomInQueueFor(CotEventContainer c) {
		return repeaterManager.getRepeatableMessageCount() < maxAllowedRepeatables;
	}

	@Override
	public String name() {
		return SERVICE_NAME;
	}

	@Override
	public boolean addToInputQueue(CotEventContainer cotMsg) {
		// ** At the moment this service receives all incoming messages, and determines itself if they are applicable or
		// ** not. An applicable message is one that either initiates a repeating treatment, or cancels such treatment.
		// ** All other messages are ignored by this service.
		Tuple<Boolean, String> isInitiateMsg = isRepeatInitiatorMessage(cotMsg);
		if(isInitiateMsg.left()) {
			
			
		    
//		   User user = (User) cotMsg.getContextValue(SubmissionService.USER_KEY);
//		    
//		    if (user == null) {
//		        throw new TakException("repeatable message " + cotMsg + " does not contain a user - skipping");
//		    }
		    
		    LOGGER.debug("New repeatable message request found. ");
		    
		    // Get a copy of the user
		    try {
		        
		        
		        User user = (User) cotMsg.getContextValue(Constants.USER_KEY);

		        if (user != null) {
			        User rptUser = groupManager.replicateUserAndGroupMembership(user);
		        	cotMsg.setContext(Constants.USER_KEY, rptUser);
		        }
		        
                cotMsg.setContext(Constants.REPEATER_KEY, true);
		    } catch (Exception e) {
		        throw new TakException(e);
		    }
		    
			if(hasRoomInQueueFor(cotMsg)) {
				repeaterManager.addMessage(cotMsg, isInitiateMsg.right());
				return true;
			} else {
				// ** Here we just log a "sufficient" amount of the message if we can't store it in the queue.
				// ** The truncation is done simply to prevent attacks against the log system as otherwise
				// ** we're piping user data straight to the log.
				String msgXML = cotMsg.asXml();
				String safeMessage = msgXML.substring(0, (msgXML.length() < 1000 ? msgXML.length() : 1000));
				LOGGER.error("No room to store received repeatable message of type: " + isInitiateMsg.right() + ". Details follow: " + safeMessage);
				return false;
			}
		} else if(isRepeatCancellationMessage(cotMsg)) {
			if(repeaterManager.removeMessage(cotMsg.getUid(), false)) {
				LOGGER.debug("Cancelling repeater treatment for " + cotMsg.getUid());
				return true;
			} else {
				LOGGER.warn("Received request to cancel repeat treatment for " + cotMsg.getUid() + ", however messages for this UID were not being repeated.");
				return false; 
			}
		} else {
			return false;
		}
	}

	private XPath getXPath(String xpath) {

		if (xpathMap.get() == null) {
			xpathMap.set(new HashMap<String, XPath>());
			for(Tuple<String, String> xpathTests : repeatableTypes.values()) {
				xpathMap.get().put(xpathTests.left(), DocumentHelper.createXPath(xpathTests.left()));
				xpathMap.get().put(xpathTests.right(), DocumentHelper.createXPath(xpathTests.right()));
			}
		}

		return xpathMap.get().get(xpath);
	}

	/**
	 * Determine if the given message is a cancellation message or not. 
	 * @param cotMsg The message to analyze
	 * @return True if it represents a cancellation order for a repeated message
	 */
	private boolean isRepeatCancellationMessage(CotEventContainer cotMsg) {
		for(Tuple<String, String> xpathTests : repeatableTypes.values()) {

			if (getXPath(xpathTests.right()).booleanValueOf(cotMsg.getDocument())) {
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Determine if the given message is an initiate message or not.
	 * @param cotMsg The message to analyze
	 * @return A tuple with the first element being true if this is an initiate message, and
	 * the second being the type of initiate message (or null if this is not an initiate message).
	 */
	private Tuple<Boolean, String> isRepeatInitiatorMessage(CotEventContainer cotMsg) {
		for(String repeatableType : repeatableTypes.keySet()) {
			Tuple<String, String> xpathTests = repeatableTypes.get(repeatableType);

			if (getXPath(xpathTests.left()).booleanValueOf(cotMsg.getDocument())) {
				return new Tuple<Boolean, String> (true, repeatableType);
			}
		}
		return new Tuple<Boolean, String> (false, null);
	}

	@Override
	protected void processNextEvent() {
		// ** No processing occurs on a per message basis, instead it occurs periodically. See executePeriodicTask(void).
	}
	
	private static String getConfigurationKey(String propertyName) {
		return CONFIG_KEY_BASE + propertyName;
	}
	
	/**
	 * Allows for pausing and resuming of this service. Set to true to allow service to execute, false to pause.
	 * @param active
	 */
	public void setActive(boolean active) {
		this.active = active;
	}
	
	@Override
	public void startService() {
		setActive(true);
	}
	
	@Override
	public void stopService(boolean wait) {
		setActive(false);
	}
	
	/**
	 * This is the core logic that is executed on a periodic basis. It results in all repeatable messages being disseminated.
	 */
	private void executePeriodicTask() {
		if (active && coreConfig.getRemoteConfiguration().getRepeater().isEnable()) {
			for (CotEventContainer cotMsg : repeaterManager.getMessages()) {
				long now = System.currentTimeMillis();
				cotMsg.setTime(DateUtil.toCotTime(now));
				cotMsg.setStale(DateUtil.toCotTime(now + coreConfig.getRemoteConfiguration().getRepeater().getStaleDelayMillis()));
				
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("submitted repeated message to broker service: " + cotMsg);

					if (cotMsg.getContextValue(Constants.GROUPS_KEY) != null) {
						try {
							@SuppressWarnings("unchecked")
							NavigableSet<Group> groups = (NavigableSet<Group>) cotMsg.getContextValue(Constants.GROUPS_KEY);

							LOGGER.debug("repeatable message groups: " + groups);

						} catch (ClassCastException e) {
							LOGGER.debug("Ignoring message with invalid type of group set object - " + cotMsg);
						}
					}
				}
				
				brokerService.addToInputQueue(cotMsg);
			}

			LinkedList<String> removedEntries = new LinkedList<String>();
			for (CotEventContainer cotMsg : repeaterManager
					.getCancelledMessages().values()) {

				long now = System.currentTimeMillis();
				cotMsg.setTime(DateUtil.toCotTime(now));
				cotMsg.setStale(DateUtil.toCotTime(now + coreConfig.getRemoteConfiguration().getRepeater().getStaleDelayMillis()));
				cotMsg.setType("b-a-o-can");

				String callsign = cotMsg.getCallsign();
				
				Node detailNode = cotMsg.getDocument().selectSingleNode("/event/detail");
				detailNode.detach();
				
				Element eventElement = (Element)cotMsg.getDocument().nodeIterator().next();
				Element newDetail = eventElement.addElement("detail");
				Element newEmergency = newDetail.addElement("emergency");
				newEmergency.addAttribute("cancel", "true");
				newEmergency.setText(callsign);
				
				brokerService.addToInputQueue(cotMsg);

				removedEntries.add(cotMsg.getUid());
			}

			for (String s : removedEntries) {
				repeaterManager.getCancelledMessages().remove(s);

			}
		}
	}

	public DistributedRepeaterManager getRepeaterManager() {
		return repeaterManager;
	}

}
