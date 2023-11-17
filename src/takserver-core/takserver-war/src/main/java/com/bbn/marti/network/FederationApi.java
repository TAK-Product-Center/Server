

package com.bbn.marti.network;

import java.io.InputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

import org.jetbrains.annotations.NotNull;
import org.owasp.esapi.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bbn.marti.config.Federation;
import com.bbn.marti.config.Federation.Federate;
import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.remote.ConnectionStatus;
import com.bbn.marti.remote.FederationManager;
import com.bbn.marti.remote.RemoteContact;
import com.bbn.marti.service.FederationHttpConnectorManager;
import com.bbn.marti.sync.repository.FederationEventRepository;
import com.bbn.security.web.MartiValidator;
import com.bbn.security.web.MartiValidatorConstants;
import com.google.common.collect.ComparisonChain;
//import com.sun.tools.jxc.ap.Const;
import com.google.common.collect.Multimap;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import tak.server.Constants;
import tak.server.ignite.ApiIgniteBroker;

/**
 *
 * REST endpoint for interfacing with federation service
 *
 */
@RestController
public class FederationApi extends BaseRestController {

	Logger logger = LoggerFactory.getLogger(FederationApi.class);

	@Autowired
	private FederationManager federationInterface;
	
	@Autowired
	private FederationEventRepository fedEventRepository;

	public FederationManager getFederationInterface() {
		return federationInterface;
	}

	public void setFederationInterface(FederationManager federationInterface) {
		this.federationInterface = federationInterface;
	}

	@RequestMapping(value = "/outgoingconnections", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<SortedSet<OutgoingConnectionSummary>>> getOutgoingConnections(HttpServletResponse response) {

		if (logger.isTraceEnabled()) {
			logger.trace("GET outgoingconnections");
		}

		setCacheHeaders(response);

		SortedSet<OutgoingConnectionSummary> outgoingConnections = null;
		ResponseEntity<ApiResponse<SortedSet<OutgoingConnectionSummary>>> result = null;

		try {
			outgoingConnections = new ConcurrentSkipListSet<OutgoingConnectionSummary>(new Comparator<OutgoingConnectionSummary>() {
				@Override
				public int compare(OutgoingConnectionSummary thiz, OutgoingConnectionSummary that) {
					return ComparisonChain.start()
							.compare(thiz.getDisplayName(), that.getDisplayName())
							.result();
				}
			});

			List<Federation.FederationOutgoing> fos = federationInterface.getOutgoingConnections();

			if (logger.isTraceEnabled()) {
				logger.trace("outgoing connections from federationInterface: " + fos);
			}

			ConnectionStatus cs = null;

			for (Federation.FederationOutgoing fo : fos) {
				cs = federationInterface.getOutgoingStatus(fo.getDisplayName());

				if (logger.isTraceEnabled()) {
					logger.trace("getOutgoingStatus " + fo.getDisplayName() + " result: " + cs);
				}
				
				ConnectionInfoSummary csi = null;

				if (cs != null) {
					csi = new ConnectionInfoSummary(cs.getFederate(), cs, cs.getConnectionStatusValue());
				}
				
				outgoingConnections.add(new OutgoingConnectionSummary(fo, csi));

			}

			result = new ResponseEntity<ApiResponse<SortedSet<OutgoingConnectionSummary>>>(new ApiResponse<SortedSet<OutgoingConnectionSummary>>(Constants.API_VERSION, OutgoingConnectionSummary.class.getName(), outgoingConnections), HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("Exception getting outgoing connections.", e);
		}

		if (result == null) {
			//This would be an error condition (not an empty list)
			result = new ResponseEntity<ApiResponse<SortedSet<OutgoingConnectionSummary>>>(new ApiResponse<SortedSet<OutgoingConnectionSummary>>(Constants.API_VERSION, OutgoingConnectionSummary.class.getName(), null), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	@RequestMapping(value = "/federatecontacts/{federateId}", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<SortedSet<RemoteContact>>> getFederateContacts(HttpServletResponse response,
			@PathVariable("federateId") String federateId) {

		setCacheHeaders(response);

		SortedSet<RemoteContact> federateContacts = null;
		ResponseEntity<ApiResponse<SortedSet<RemoteContact>>> result = null;
		List<String> errors = null;

		try {
			federateContacts = new ConcurrentSkipListSet<RemoteContact>(new Comparator<RemoteContact>() {
				@Override
				public int compare(RemoteContact thiz, RemoteContact that) {
					return ComparisonChain.start()
							.compare(thiz.getContactName(), that.getContactName())
							.result();
				}
			});

			errors = getCertificateFingerprintValidationErrors(federateId);

			if (errors.isEmpty()) {
				Collection<RemoteContact> fc = federationInterface.getContactsForFederate(federateId, null);
				if (fc != null) {
					federateContacts.addAll(fc);
				}

				result = new ResponseEntity<ApiResponse<SortedSet<RemoteContact>>>(new ApiResponse<SortedSet<RemoteContact>>(Constants.API_VERSION, SortedSet.class.getName(), federateContacts), HttpStatus.OK);
			}
		} catch (Exception e) {
			errors.add("Unexpected error retrieving federate contacts.");
			logger.debug("Exception getting federate contacts.", e);
		}

		if (result == null) {
			result = new ResponseEntity<ApiResponse<SortedSet<RemoteContact>>>(new ApiResponse<SortedSet<RemoteContact>>(Constants.API_VERSION, RemoteContact.class.getName(), null, errors), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	@RequestMapping(value = "/activeconnections", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<List<ConnectionInfoSummary>>> getActiveConnections(HttpServletResponse response) {

		setCacheHeaders(response);

		List<ConnectionStatus> activeConnections = null;
		ResponseEntity<ApiResponse<List<ConnectionInfoSummary>>> result = null;
		List<ConnectionInfoSummary> connectionInfo = new ArrayList<ConnectionInfoSummary>();

		try {
			activeConnections = federationInterface.getActiveConnectionInfo();

			for (ConnectionStatus cs : activeConnections) {
				ConnectionInfoSummary csi = new ConnectionInfoSummary(cs.getFederate(), cs, cs.getConnectionStatusValue());
				
				csi.setGroups(cs.getGroups());
				
				connectionInfo.add(csi);
			}
			result = new ResponseEntity<ApiResponse<List<ConnectionInfoSummary>>>(new ApiResponse<List<ConnectionInfoSummary>>(Constants.API_VERSION, ConnectionInfoSummary.class.getName(), connectionInfo), HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("Exception getting active connections.", e);
		}

		if (result == null) {
			//This would be an error condition (not an empty list)
			result = new ResponseEntity<ApiResponse<List<ConnectionInfoSummary>>>(new ApiResponse<List<ConnectionInfoSummary>>(Constants.API_VERSION, ConnectionInfoSummary.class.getName(), null), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	@RequestMapping(value = "/outgoingconnectionstatus/{name}", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<ConnectionStatus>> getConnectionStatus(@PathVariable("name") String name, HttpServletResponse response) {

		setCacheHeaders(response);

		ConnectionStatus connectionStatus = null;
		ResponseEntity<ApiResponse<ConnectionStatus>> result = null;

		try {
			connectionStatus = federationInterface.getOutgoingStatus(name);
			if (connectionStatus != null) {
				result = new ResponseEntity<ApiResponse<ConnectionStatus>>(new ApiResponse<ConnectionStatus>(Constants.API_VERSION, ConnectionStatus.class.getName(), connectionStatus), HttpStatus.OK);
			} else {
				result = new ResponseEntity<ApiResponse<ConnectionStatus>>(new ApiResponse<ConnectionStatus>(Constants.API_VERSION, ConnectionStatus.class.getName(), connectionStatus), HttpStatus.NOT_FOUND);
			}
		} catch (Exception e) {
			logger.debug("Exception getting outgoing connection.", e);
		}

		if (result == null) {
			//This would be an error condition (not an empty list)
			result = new ResponseEntity<ApiResponse<ConnectionStatus>>(new ApiResponse<ConnectionStatus>(Constants.API_VERSION, ConnectionStatus.class.getName(), connectionStatus), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	@RequestMapping(value = "/outgoingconnectionstatus/{name}", method = RequestMethod.POST)
	public ResponseEntity<ApiResponse<Boolean>> changeConnectionStatus(@PathVariable("name") String name, @RequestParam("newStatus") boolean newStatus) {

		ResponseEntity<ApiResponse<Boolean>> result = null;

		try {
			if (newStatus) {
				federationInterface.enableOutgoing(name);
			} else {
				federationInterface.disableOutgoingForNode(name);
			}
			result = new ResponseEntity<ApiResponse<Boolean>>(new ApiResponse<Boolean>(Constants.API_VERSION, Boolean.class.getName(), newStatus), HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("Exception changing outgoing connection status.", e);
		}

		if (result == null) {
			//This would be an error condition
			result = new ResponseEntity<ApiResponse<Boolean>>(new ApiResponse<Boolean>(Constants.API_VERSION, Boolean.class.getName(), newStatus), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	@RequestMapping(value = "/outgoingconnections/{name}", method = RequestMethod.DELETE)
	public ResponseEntity<ApiResponse<Boolean>> deleteOutgoingConnection(@PathVariable("name") String name) {

		ResponseEntity<ApiResponse<Boolean>> result = null;

		try {
			federationInterface.removeOutgoing(name);
			result = new ResponseEntity<ApiResponse<Boolean>>(new ApiResponse<Boolean>(Constants.API_VERSION, Boolean.class.getName(), true), HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("Exception deleting outgoing connection.", e);
		}

		if (result == null) {
			//This would be an error condition
			result = new ResponseEntity<ApiResponse<Boolean>>(new ApiResponse<Boolean>(Constants.API_VERSION, Boolean.class.getName(), false), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	@RequestMapping(value = "/federates", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<SortedSet<Federate>>> getFederates(HttpServletResponse response) {

		if (logger.isTraceEnabled()) {
			logger.trace("getFederates");
		}

		setCacheHeaders(response);

		SortedSet<Federate> federates = null;
		ResponseEntity<ApiResponse<SortedSet<Federate>>> result = null;

		try {
			federates = new ConcurrentSkipListSet<Federate>(new Comparator<Federate>() {
				@Override
				public int compare(Federate thiz, Federate that) {
					return ComparisonChain.start()
							.compare(thiz.getName(), that.getName())
							.result();
				}
			});

			List<Federate> allFederates = federationInterface.getAllFederates();

			if (logger.isTraceEnabled()) {
				logger.trace("allFederates: " + allFederates);
			}

			federates.addAll(allFederates);
			result = new ResponseEntity<ApiResponse<SortedSet<Federate>>>(new ApiResponse<SortedSet<Federate>>(Constants.API_VERSION, Federate.class.getName(), federates), HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("Exception getting federates", e);
		}

		if (result == null) {
			//This would be an error condition (not an empty list)
			result = new ResponseEntity<ApiResponse<SortedSet<Federate>>>(new ApiResponse<SortedSet<Federate>>(Constants.API_VERSION, Federate.class.getName(), null), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	@RequestMapping(value = "/federategroups/{federateId}", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<List<FederateGroupAssociation>>> getFederateGroups(@PathVariable("federateId") String federateId, HttpServletResponse response) {

		setCacheHeaders(response);

		ResponseEntity<ApiResponse<List<FederateGroupAssociation>>> result = null;

		List<FederateGroupAssociation> federateGroupAssociations = new ArrayList<FederateGroupAssociation>();

		try {
			if (federateId == null || federateId.trim().length() == 0) {
				result = new ResponseEntity<ApiResponse<List<FederateGroupAssociation>>>(new ApiResponse<List<FederateGroupAssociation>>(Constants.API_VERSION, FederateGroupAssociation.class.getName(), null),
						HttpStatus.BAD_REQUEST);
			} else {
				List<String> groupsInbound = federationInterface.getGroupsInbound(federateId);
				List<String> groupsOutbound = federationInterface.getGroupsOutbound(federateId);
				Validator validator = MartiValidator.getInstance();
				if (groupsInbound != null && !groupsInbound.isEmpty()) {
					for (String group : groupsInbound){
						if(validator.isValidInput("Federate group", group, MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, false)) {
							federateGroupAssociations.add(new FederateGroupAssociation(federateId, group, DirectionValue.INBOUND));
						}
					}
				}

				if (groupsOutbound != null && !groupsOutbound.isEmpty()) {
					for (String group : groupsOutbound) {
						if(validator.isValidInput("Federate group", group, MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, false)) {
							federateGroupAssociations.add(new FederateGroupAssociation(federateId, group, DirectionValue.OUTBOUND));
						}
					}
				}

				result = new ResponseEntity<ApiResponse<List<FederateGroupAssociation>>>(new ApiResponse<List<FederateGroupAssociation>>(Constants.API_VERSION, FederateGroupAssociation.class.getName(), federateGroupAssociations),
						HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.debug("Exception getting federate groups.", e);
		}

		if (result == null) {
			//This would be an error condition (not an empty list)
			result = new ResponseEntity<ApiResponse<List<FederateGroupAssociation>>>(new ApiResponse<List<FederateGroupAssociation>>(Constants.API_VERSION, FederateGroupAssociation.class.getName(), null),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	@RequestMapping(value = "/federategroups", method = RequestMethod.POST)
	public ResponseEntity<ApiResponse<String>> addFederateGroup(@RequestBody FederateGroupAssociation federateGroupAssociation) {

		ResponseEntity<ApiResponse<String>> result = null;

		List<String> errors = null;

		try {
			errors = getValidationErrors(federateGroupAssociation);
			if (errors.isEmpty()) {
				Set<String> groups = new HashSet<String>(1);
				groups.add(federateGroupAssociation.getGroup());

				if (federateGroupAssociation.getDirection() == DirectionValue.INBOUND) {
					federationInterface.addFederateToGroupsInbound(federateGroupAssociation.getFederateId(), groups);
				} else if (federateGroupAssociation.getDirection() == DirectionValue.OUTBOUND) {
					federationInterface.addFederateToGroupsOutbound(federateGroupAssociation.getFederateId(), groups);
				} else {
					federationInterface.addFederateToGroupsInbound(federateGroupAssociation.getFederateId(), groups);
					federationInterface.addFederateToGroupsOutbound(federateGroupAssociation.getFederateId(), groups);
				}
				result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION,
						String.class.getName(), federateGroupAssociation.getGroup()), HttpStatus.OK);
			} else {
				result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION,
						String.class.getName(), null, errors), HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.error("Exception adding federate group.", e);
			//In general, revealing raw exceptions to the user is not a good idea, but for now, as discussed,
			//we'll leave this in until more granular exceptions are thrown by the service layer.
			//The types of errors we'll see here concern missing cert files or lack of root access
			//for lower port numbers the user has provided.
			errors.add(e.getMessage());
		}

		if (result == null) {
			//This would be an error condition (not a bad request)
			result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, String.class.getName(), null, errors),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	@RequestMapping(value = "/federategroupsmap/{federateId}", method = RequestMethod.POST)
	public ResponseEntity<ApiResponse<String>> addFederateGroupMap(@PathVariable("federateId") String federateId, @RequestParam("remoteGroup") String remoteGroup, @RequestParam("localGroup") String localGroup) {

		if (logger.isDebugEnabled()) {
			logger.debug("Received POST request addFederateGroupMap for federateId {}", federateId);
		}
		ResponseEntity<ApiResponse<String>> result = null;

		List<String> errors = null;

		try {
			errors = getValidationErrors(remoteGroup, localGroup);
			if (errors.isEmpty()) {
				federationInterface.addFederateGroupsInboundMap(federateId, remoteGroup, localGroup);
				result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION,
						String.class.getName(), localGroup), HttpStatus.OK);
			} else {
				result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION,
						String.class.getName(), null, errors), HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.error("Exception adding federate group map.", e);
			errors.add(e.getMessage());
		}
		if (result == null) {
			//This would be an error condition (not a bad request)
			result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, String.class.getName(), null, errors),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	@RequestMapping(value = "/federategroupsmap/{federateId}", method = RequestMethod.DELETE)
	public ResponseEntity<ApiResponse<String>> removeFederateGroupMap(@PathVariable("federateId") String federateId, String remoteGroup, String localGroup) {

		ResponseEntity<ApiResponse<String>> result = null;

		List<String> errors = null;

		try {
			errors = getValidationErrors(remoteGroup, localGroup);
			if (errors.isEmpty()) {
				federationInterface.removeFederateInboundGroupsMap(federateId, remoteGroup, localGroup);
				result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION,
						String.class.getName(), localGroup), HttpStatus.OK);
			} else {
				result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION,
						String.class.getName(), null, errors), HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.error("Exception removing federate group map.", e);
			errors.add(e.getMessage());
		}

		if (result == null) {
			//This would be an error condition (not a bad request)
			result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, String.class.getName(), null, errors),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	@RequestMapping(value = "/federategroupsmap/{federateId}", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<Map<String, String>>> getFederateGroupsMap(@PathVariable("federateId") String federateId, HttpServletResponse response) {

		setCacheHeaders(response);

		ResponseEntity<ApiResponse<Map<String, String>>> result = null;

		try {
			if (federateId == null || federateId.trim().length() == 0) {
				result = new ResponseEntity<ApiResponse<Map<String, String>>>(new ApiResponse<Map<String, String>>(Constants.API_VERSION, String.class.getName(), null),
						HttpStatus.BAD_REQUEST);
			} else {
				//TODO: what should we return as confirmation here?
				Multimap multimap = federationInterface.getInboundGroupMap(federateId);
				result = new ResponseEntity<ApiResponse<Map<String, String>>>(new ApiResponse<Map<String, String>>(Constants.API_VERSION, String.class.getName(), multimap.asMap()),
						HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.debug("Exception getting federate groups.", e);
		}

		if (result == null) {
			//This would be an error condition (not an empty list)
			result = new ResponseEntity<ApiResponse<Map<String, String>>>(new ApiResponse<Map<String, String>>(Constants.API_VERSION, String.class.getName(), null),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	@RequestMapping(value = "/federategroups/{federateId}", method = RequestMethod.DELETE)
	public ResponseEntity<ApiResponse<String>> removeFederateGroup(@PathVariable("federateId") String federateId, @RequestParam("group") String group, @RequestParam("direction") DirectionValue directionValue) {

		ResponseEntity<ApiResponse<String>> result = null;

		List<String> errors = null;

		try {
			FederateGroupAssociation federateGroupAssociation = new FederateGroupAssociation(federateId, group, directionValue);

			errors = getValidationErrors(federateGroupAssociation);
			if (errors.isEmpty()) {
				Set<String> groups = new HashSet<String>(1);
				groups.add(federateGroupAssociation.getGroup());

				if (federateGroupAssociation.getDirection() == DirectionValue.INBOUND) {
					federationInterface.removeFederateFromGroupsInbound(federateGroupAssociation.getFederateId(), groups);
				} else if (federateGroupAssociation.getDirection() == DirectionValue.OUTBOUND) {
					federationInterface.removeFederateFromGroupsOutbound(federateGroupAssociation.getFederateId(), groups);
				} else if (federateGroupAssociation.getDirection() == DirectionValue.BOTH) {
					//Not used for now, but we can do so if the presentation requires it
					federationInterface.removeFederateFromGroupsInbound(federateGroupAssociation.getFederateId(), groups);
					federationInterface.removeFederateFromGroupsOutbound(federateGroupAssociation.getFederateId(), groups);
				}
				//Ideally we need a more meaningful return here (like the number of records removed), but the service layer only returns void for now, so we just return the name of the group removed
				result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION,
						String.class.getName(), federateGroupAssociation.getGroup()), HttpStatus.OK);
			} else {
				result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION,
						String.class.getName(), null, errors), HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.error("Exception removing federate group.", e);
			//In general, revealing raw exceptions to the user is not a good idea, but for now, as discussed,
			//we'll leave this in until more granular exceptions are thrown by the service layer.
			//The types of errors we'll see here concern missing cert files or lack of root access
			//for lower port numbers the user has provided.
			errors.add(e.getMessage());
		}

		if (result == null) {
			//This would be an error condition (not a bad request)
			result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, String.class.getName(), null, errors),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	@RequestMapping(value = "/federategroupconfig", method = RequestMethod.POST)
	public ResponseEntity<ApiResponse<String>> saveFederateGroupConfiguration() {

		ResponseEntity<ApiResponse<String>> result = null;

		List<String> errors = null;

		try {
			result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION,
					String.class.getName(), null), HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Exception saving federate group configuration.", e);
			//In general, revealing raw exceptions to the user is not a good idea, but for now, as discussed,
			//we'll leave this in until more granular exceptions are thrown by the service layer.
			//The types of errors we'll see here concern missing cert files or lack of root access
			//for lower port numbers the user has provided.
		}

		if (result == null) {
			//This would be an error condition
			result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, String.class.getName(), null, errors),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	@RequestMapping(value = "/federateremotegroups/{federateId}", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<List<String>>> getFederateRemoteGroups(@PathVariable("federateId") String federateId, HttpServletResponse response) {

		setCacheHeaders(response);

		ResponseEntity<ApiResponse<List<String>>> result = null;

		List<String> remoteGroups = new ArrayList<String>();

		try {
			if (federateId == null || federateId.trim().length() == 0) {
				result = new ResponseEntity<ApiResponse<List<String>>>(new ApiResponse<List<String>>(Constants.API_VERSION, String.class.getName(), null),
						HttpStatus.BAD_REQUEST);
			} else {
				remoteGroups = federationInterface.getFederateRemoteGroups(federateId);

				result = new ResponseEntity<ApiResponse<List<String>>>(new ApiResponse<List<String>>(Constants.API_VERSION, String.class.getName(), remoteGroups),
						HttpStatus.OK);
			}
		}  catch (Exception e) {
			logger.error("Exception retrieving the federate's remote groups.", e);

		}

		return result;
	}

	@RequestMapping(value = "/federatecagroups/{caId}", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<List<FederateCAGroupAssociation>>> getFederateCAGroups(@PathVariable("caId") String caId, HttpServletResponse response){
		setCacheHeaders(response);

		ResponseEntity<ApiResponse<List<FederateCAGroupAssociation>>> result = null;

		List<FederateCAGroupAssociation> federateCAGroupAssociations = new ArrayList<FederateCAGroupAssociation>();

		try{
			if(caId == null || caId.trim().length() == 0){
				result = new ResponseEntity<ApiResponse<List<FederateCAGroupAssociation>>>(new ApiResponse<List<FederateCAGroupAssociation>>(Constants.API_VERSION, FederateCAGroupAssociation.class.getName(), null),
						HttpStatus.BAD_REQUEST);
			}
			else{
				List<String> inboundGroups = federationInterface.getCAGroupsInbound(caId);
				List<String> outboundGroups = federationInterface.getCAGroupsOutbound(caId);

				Validator validator = MartiValidator.getInstance();
				for(String groupName : inboundGroups){
					if(validator.isValidInput("Federate CA group", groupName, MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, false)) {
						federateCAGroupAssociations.add(new FederateCAGroupAssociation(caId, groupName, DirectionValue.INBOUND));
					}
				}

				for(String groupName : outboundGroups){
					if(validator.isValidInput("Federate CA group", groupName, MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, false)) {
						federateCAGroupAssociations.add(new FederateCAGroupAssociation(caId, groupName, DirectionValue.OUTBOUND));
					}
				}

				result = new ResponseEntity<ApiResponse<List<FederateCAGroupAssociation>>>(new ApiResponse<List<FederateCAGroupAssociation>>(Constants.API_VERSION,
						FederateCAGroupAssociation.class.getName(), federateCAGroupAssociations), HttpStatus.OK);
			}
		}
		catch (Exception e){
			logger.error("Exception getting federate CA groups", e);
		}

		if(result == null){
			result = new ResponseEntity<ApiResponse<List<FederateCAGroupAssociation>>>(new ApiResponse<List<FederateCAGroupAssociation>>(Constants.API_VERSION, FederateCAGroupAssociation.class.getName(),
					null), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	@RequestMapping(value = "/federatecagroups", method = RequestMethod.POST)
	public ResponseEntity<ApiResponse<String>> addFederateCAGroup(@RequestBody FederateCAGroupAssociation federateCAGroupAssociation){
		ResponseEntity<ApiResponse<String>> result = null;

		List<String> errors = null;

		try{
			errors = getValidationErrors(federateCAGroupAssociation);
			if(errors.isEmpty()){
				Set<String> groups = new HashSet<String>(1);
				groups.add(federateCAGroupAssociation.getGroup());
				logger.debug("Federate CA Group Association: " + federateCAGroupAssociation);
				if(federateCAGroupAssociation.getDirection() == DirectionValue.INBOUND){
					federationInterface.addInboundGroupToCA(federateCAGroupAssociation.getCaId(), groups);
				}
				else if(federateCAGroupAssociation.getDirection() == DirectionValue.OUTBOUND){
					federationInterface.addOutboundGroupToCA(federateCAGroupAssociation.getCaId(), groups);
				}
				else{
					federationInterface.addInboundGroupToCA(federateCAGroupAssociation.getCaId(), groups);
					federationInterface.addOutboundGroupToCA(federateCAGroupAssociation.getCaId(), groups);
				}
				result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION,
						String.class.getName(), federateCAGroupAssociation.getGroup()), HttpStatus.OK);
			}
			else {
				result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION,
						String.class.getName(), null, errors), HttpStatus.BAD_REQUEST);
			}
		}
		catch(Exception e){
			logger.error("Exception adding federate CA group", e);
			errors.add(e.getMessage());
		}

		if(result == null){
			result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, String.class.getName(), null, errors),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return result;
	}

	@RequestMapping(value = "/federatecagroups/{caId}", method = RequestMethod.DELETE)
	public ResponseEntity<ApiResponse<String>> removeFederateCAGroup(@PathVariable("caId") String caId,  @RequestParam("group") String group, @RequestParam("direction") DirectionValue directionValue){
		ResponseEntity<ApiResponse<String>> result = null;

		List<String> errors = null;

		try{
			FederateCAGroupAssociation federateCAGroupAssociation = new FederateCAGroupAssociation(caId, group, directionValue);
			logger.debug("DELETE FederateCAGroupAssociation: " + federateCAGroupAssociation);
			errors = getValidationErrors(federateCAGroupAssociation);
			if(errors.isEmpty()){
				Set<String> groups = new HashSet<String>(1);
				groups.add(federateCAGroupAssociation.getGroup());

				if(federateCAGroupAssociation.getDirection() == DirectionValue.INBOUND){
					federationInterface.removeInboundGroupFromCA(federateCAGroupAssociation.getCaId(), groups);
				}
				else if(federateCAGroupAssociation.getDirection() == DirectionValue.OUTBOUND){
					federationInterface.removeOutboundGroupFromCA(federateCAGroupAssociation.getCaId(), groups);
				}
				else{
					federationInterface.removeInboundGroupFromCA(federateCAGroupAssociation.getCaId(), groups);
					federationInterface.removeOutboundGroupFromCA(federateCAGroupAssociation.getCaId(), groups);
				}
				result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION,
						String.class.getName(), federateCAGroupAssociation.getGroup()), HttpStatus.OK);
			}
			else{
				result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION,
						String.class.getName(), null, errors), HttpStatus.BAD_REQUEST);
			}
		}
		catch(Exception e){
			logger.error("Exception removing federate CA group.", e);
			errors.add(e.getMessage());

		}

		if(result ==  null){
			result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, String.class.getName(), null, errors),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return result;
	}

	@RequestMapping(value = "/outgoingconnections", method = RequestMethod.POST)
	public ResponseEntity<ApiResponse<Federation.FederationOutgoing>> createOutgoingConnection(@RequestBody Federation.FederationOutgoing outgoingConnection) {

		ResponseEntity<ApiResponse<Federation.FederationOutgoing>> result = null;

		List<String> errors = null;

		try {
			errors = getValidationErrors(outgoingConnection);
			if (errors.isEmpty()) {
				federationInterface.addOutgoingConnection(outgoingConnection.getDisplayName(),
						outgoingConnection.getAddress(),
						outgoingConnection.getPort(),
						outgoingConnection.getReconnectInterval(),
						outgoingConnection.getMaxRetries(),
						outgoingConnection.isUnlimitedRetries(),
						outgoingConnection.isEnabled(),
						outgoingConnection.getProtocolVersion(),
						outgoingConnection.getFallback());

				result = new ResponseEntity<ApiResponse<Federation.FederationOutgoing>>(new ApiResponse<Federation.FederationOutgoing>(Constants.API_VERSION,
						Federation.FederationOutgoing.class.getName(), outgoingConnection), HttpStatus.OK);
			} else {
				result = new ResponseEntity<ApiResponse<Federation.FederationOutgoing>>(new ApiResponse<Federation.FederationOutgoing>(Constants.API_VERSION,
						Federation.FederationOutgoing.class.getName(), outgoingConnection, errors), HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.error("Exception adding outgoing connection.", e);
			//The types of errors we'll see here concern missing cert files or lack of root access
			//for lower port numbers the user has provided.
			errors.add(e.getMessage());
		}

		if (result == null) {
			//This would be an error condition (not an empty input list or bad request)
			result = new ResponseEntity<ApiResponse<Federation.FederationOutgoing>>(new ApiResponse<Federation.FederationOutgoing>(Constants.API_VERSION,
					Federation.FederationOutgoing.class.getName(), null, errors), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	@RequestMapping(value = "/outgoingconnections/{name}", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<Federation.FederationOutgoing>> getOutgoingConnection(@PathVariable("name") String name, HttpServletResponse response) {

		setCacheHeaders(response);

		Federation.FederationOutgoing outgoing = null;
		ResponseEntity<ApiResponse<Federation.FederationOutgoing>> result = null;

		try {
			if (!getOutgoingConnectionDisplayNameValidationErrors(name).isEmpty()) {
				result = new ResponseEntity<ApiResponse<Federation.FederationOutgoing>>(new ApiResponse<Federation.FederationOutgoing>(Constants.API_VERSION,
						Federation.FederationOutgoing.class.getName(), null), HttpStatus.BAD_REQUEST);
			} else {
				Collection<Federation.FederationOutgoing> outgoingConnections = federationInterface.getOutgoingConnections();
				if (outgoingConnections != null) {
					//Wait for j2se 1.8
					//metric = metrics.stream().filter(i -> i.getInput().getName().equals(name)).findFirst();
					for (Federation.FederationOutgoing oc : outgoingConnections) {
						if (oc.getDisplayName().equalsIgnoreCase(name)) {
							outgoing = oc;
							break;
						}
					}
				}
				result = new ResponseEntity<ApiResponse<Federation.FederationOutgoing>>(new ApiResponse<Federation.FederationOutgoing>(Constants.API_VERSION,
						Federation.FederationOutgoing.class.getName(), outgoing), HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.debug("Exception getting outgoing connection.", e);
		}

		if (result == null) {
			//This would be an error condition (not an empty input list or bad request)
			result = new ResponseEntity<ApiResponse<Federation.FederationOutgoing>>(new ApiResponse<Federation.FederationOutgoing>(Constants.API_VERSION,
					Federation.FederationOutgoing.class.getName(), null), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}


	@RequestMapping(value = "/federatecertificates", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<List<CertificateSummary>>> getFederateCertificates(HttpServletResponse response) {

		setCacheHeaders(response);

		ResponseEntity<ApiResponse<List<CertificateSummary>>> result = null;

		List<X509Certificate> x509Certificates = null;
		List<CertificateSummary> certificateSummaries = new ArrayList<CertificateSummary>();

		try {
			x509Certificates = federationInterface.getCAList();
			if (x509Certificates != null) {
				for (X509Certificate x : x509Certificates) {
					certificateSummaries.add(new CertificateSummary(x.getIssuerDN().getName(), x.getSubjectDN().getName(), x.getSerialNumber(), getCertFingerprint(x)));
				}
			}
			result = new ResponseEntity<ApiResponse<List<CertificateSummary>>>(new ApiResponse<List<CertificateSummary>>(Constants.API_VERSION, CertificateSummary.class.getName(), certificateSummaries), HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("Exception getting federate certificates.", e);
		}

		if (result == null) {
			//This would be an error condition (not an empty list)
			result = new ResponseEntity<ApiResponse<List<CertificateSummary>>>(new ApiResponse<List<CertificateSummary>>(Constants.API_VERSION, CertificateSummary.class.getName(), null),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	@RequestMapping(value = "/federatecertificates", method = RequestMethod.POST)
	public ResponseEntity<ApiResponse<String>> saveFederateCertificateCA(@RequestPart("file") MultipartFile certificateFile) {

		ResponseEntity<ApiResponse<String>> result = null;
		List<String> errors = new ArrayList<String>();

		try {
			//The size is checked upstream by org.springframework.web.multipart.commons.CommonsMultipartResolver for better security (see api-context.xml and keep
			//CERT_FILE_UPLOAD_SIZE defined here in sync with the value provided there). The size check is duplicated here in case the global value in api-context.xml
			//needs to be expanded in the future to accommodate other transactions. The change in api-context.xml does not break the Enterrprise Sync functionality, which accepts larger files.

			//Also, checking mime type here (should be application/x-x509-ca-cert) is not going to be effective. Each OS/Browser has a different algorithm for determining mime type on file uploads and only
			//in some cases is the file content actually part of the check; in many cases the check is dependent on the actual client OS installation and registration of file extensions.
			//In any event, the value can be easily overridden for an upload, so the benefit is very limited/suggestive.
			if (certificateFile != null && certificateFile.getSize() < CERT_FILE_UPLOAD_SIZE) {
				try(InputStream certIS = certificateFile.getInputStream()) {
					CertificateFactory factory = CertificateFactory.getInstance("X.509");
					X509Certificate x509Certificate = (X509Certificate) factory.generateCertificate(certIS);
					
					federationInterface.addCA(x509Certificate);
					
					ApiIgniteBroker.brokerVoidServiceCalls(s -> ((FederationHttpConnectorManager) s)
							.asyncReloadFederationHttpConnector(), Constants.DISTRIBUTED_FEDERATION_HTTP_CONNECTOR_SERVICE, FederationHttpConnectorManager.class);

					result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, String.class.getName(), null), HttpStatus.OK);
				}
			} else {
				errors.add("Certificate is missing, the wrong type of file or too large.");
			}
		} catch (Exception e) {
			errors.add(e.getMessage());
			logger.debug("Exception saving federate certificate CA.", e);
		}

		if (result == null) {
			//This would be an error condition (not an empty list)
			result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, String.class.getName(), null, errors),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	@RequestMapping(value = "/federatecertificates/{fingerprint}", method = RequestMethod.DELETE)
	public ResponseEntity<ApiResponse<String>> deleteFederateCertificateCA(@PathVariable("fingerprint") String fingerprint) {

		ResponseEntity<ApiResponse<String>> result = null;
		List<String> errors = new ArrayList<String>();

		try {
			errors.addAll(getCertificateFingerprintValidationErrors(fingerprint));

			if (errors.isEmpty()) {
				List<X509Certificate> certificates = federationInterface.getCAList();
				if (certificates != null) {
					for (X509Certificate x : certificates) {
						if (getCertFingerprint(x).equals(fingerprint)) {
							
							federationInterface.removeCA(x);
							
							ApiIgniteBroker.brokerVoidServiceCalls(s -> ((FederationHttpConnectorManager) s)
									.asyncReloadFederationHttpConnector(), Constants.DISTRIBUTED_FEDERATION_HTTP_CONNECTOR_SERVICE, FederationHttpConnectorManager.class);

							result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, String.class.getName(), null), HttpStatus.OK);
							break;
						}
					}
					if (result == null) {
						errors.add("Certificate not found for deletion.");
						logger.error("Unable to location certificate for deletion.", fingerprint);
					}
				} else {
					errors.add("Certificate not found for deletion.");
					logger.error("No certificates returned from federationInterface during deletion.");
				}
			}
		} catch (Exception e) {
			errors.add(e.getMessage());
			logger.error("Exception deleting certificate.", e);
		}

		if (result == null) {
			result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, String.class.getName(), null, errors),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	@RequestMapping(value = "/federatedetails", method = RequestMethod.PUT)
	public ResponseEntity<ApiResponse<Federate>> updateFederateDetails(@RequestBody Federate federate) {

		ResponseEntity<ApiResponse<Federate>> result = null;

		logger.trace("RMI federationInterface: " + federationInterface);
		List<String> errors = null;

		try {
			errors = getValidationErrors(federate);
			if (errors.isEmpty()) {
				federationInterface.updateFederateDetails(federate.getId(), federate.isArchive(), federate.isShareAlerts(),
						federate.isFederatedGroupMapping(), federate.isAutomaticGroupMapping(), federate.getNotes());

				result = new ResponseEntity<ApiResponse<Federate>>(new ApiResponse<Federate>(Constants.API_VERSION,
						Federate.class.getName(), federate), HttpStatus.OK);
			} else {
				result = new ResponseEntity<ApiResponse<Federate>>(new ApiResponse<Federate>(Constants.API_VERSION,
						Federate.class.getName(), federate, errors), HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.error("Exception updating federate details.", e);
			errors.add(e.getMessage());
		}

		if (result == null) {
			//This would be an error condition (not an empty input list or bad request)
			result = new ResponseEntity<ApiResponse<Federate>>(new ApiResponse<Federate>(Constants.API_VERSION,
					Federate.class.getName(), null, errors), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	@RequestMapping(value = "/federatedetails/{id}", method = RequestMethod.GET)
	public ResponseEntity<ApiResponse<Federation.Federate>> getFederateDetails(@PathVariable("id") String id, HttpServletResponse response) {

		setCacheHeaders(response);

		Federate federate = null;
		ResponseEntity<ApiResponse<Federate>> result = null;

		try {
			if (id != null && id.trim().length() > 0) {
				federate = federationInterface.getFederate(id);
				result = new ResponseEntity<ApiResponse<Federate>>(new ApiResponse<Federate>(Constants.API_VERSION, Federate.class.getName(), federate), HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.debug("Exception getting federates.", e);
		}

		if (result == null) {
			//This would be an error condition (not an empty list)
			result = new ResponseEntity<ApiResponse<Federate>>(new ApiResponse<Federate>(Constants.API_VERSION, Federate.class.getName(), null), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;

	}

	@RequestMapping(value = "/federatedetails/{federateId}", method = RequestMethod.DELETE)
	public ResponseEntity<ApiResponse<Boolean>> deleteFederate(@PathVariable("federateId") String federateId) {

		ResponseEntity<ApiResponse<Boolean>> result = null;

		try {
			federationInterface.removeFederate(federateId);
			result = new ResponseEntity<ApiResponse<Boolean>>(new ApiResponse<Boolean>(Constants.API_VERSION, Boolean.class.getName(), true), HttpStatus.OK);
		} catch (Exception e) {
			logger.info("Exception when trying to delete the federate", e);
		}

		if (result == null) {
			//This would be an error condition
			result = new ResponseEntity<ApiResponse<Boolean>>(new ApiResponse<Boolean>(Constants.API_VERSION, Boolean.class.getName(), false), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}

	private List<String> getValidationErrors(FederateGroupAssociation federateGroupAssociation) {

		List<String> errors = new ArrayList<String>();

		if (federateGroupAssociation.getFederateId() == null || federateGroupAssociation.getFederateId().trim().length() == 0) {
			errors.add("Federate ID must not be null or a zero length string.");
		} else {
			federateGroupAssociation.setFederateId(federateGroupAssociation.getFederateId().trim());
			errors.addAll(getCertificateFingerprintValidationErrors(federateGroupAssociation.getFederateId()));
		}

		if (federateGroupAssociation.getGroup() == null || federateGroupAssociation.getGroup().trim().length() == 0) {
			errors.add("Group must not be null or a zero length string.");
		} else {
			federateGroupAssociation.setGroup(federateGroupAssociation.getGroup().trim());

			if (federateGroupAssociation.getGroup().length() > FEDERATE_GROUP_NAME_MAX_LENGTH) {
				errors.add("Federate group name must not be longer than " + FEDERATE_GROUP_NAME_MAX_LENGTH + " characters.");
			}
			if (federateGroupAssociation.getGroup().replaceFirst("^[\\w\\d\\s\\.\\(\\)@#$_\\=\\-\\+\\[\\]\\{\\}:,\\/\\|\\\\]*$", "").length() > 0) {
				errors.add("Federate group name contains invalid characters.");
			}
		}

		return errors;
	}

	private List<String> getValidationErrors(FederateCAGroupAssociation federateCAGroupAssociation){
		List<String> errors = new ArrayList<String>();

		if(federateCAGroupAssociation.getCaId() == null || federateCAGroupAssociation.getCaId().trim().length() == 0){
			errors.add("Certificate Authority ID must not be null or a zero length string.");
		}
		else{
			federateCAGroupAssociation.setCaId(federateCAGroupAssociation.getCaId().trim());
		}

		if(federateCAGroupAssociation.getGroup() == null || federateCAGroupAssociation.getGroup().trim().length() == 0){
			errors.add("Group must not be null or a zero length string.");
		}
		else{
			federateCAGroupAssociation.setGroup(federateCAGroupAssociation.getGroup().trim());

			if(federateCAGroupAssociation.getGroup().length() > FEDERATE_GROUP_NAME_MAX_LENGTH){
				errors.add("Group name must not be longer than " + FEDERATE_GROUP_NAME_MAX_LENGTH + " characters.");
			}

			if (federateCAGroupAssociation.getGroup().replaceFirst("^[\\w\\d\\s\\.\\(\\)@#$_\\=\\-\\+\\[\\]\\{\\}:,\\/\\|\\\\]*$", "").length() > 0) {
				errors.add("Group name contains invalid characters.");
			}

		}
		return errors;
	}

	private List<String> getValidationErrors(String remoteGroup, String localGroup){
		List<String> errors = new ArrayList<String>();

		if (remoteGroup == null || remoteGroup.trim().length() == 0){
			errors.add("Remote Group name must not be null or a zero length string.");
		}
		else {
			if (remoteGroup.length() > FEDERATE_GROUP_NAME_MAX_LENGTH) {
				errors.add("Group name must not be longer than " + FEDERATE_GROUP_NAME_MAX_LENGTH + " characters.");
			}

			if (remoteGroup.replaceFirst("^[\\w\\d\\s\\.\\(\\)@#$_\\=\\-\\+\\[\\]\\{\\},\\/\\|\\\\]*$", "").length() > 0) {
				errors.add(" Remote group name contains invalid characters.");
			}

		}

		if (localGroup == null || localGroup.trim().length() == 0) {
			errors.add("Local group must not be null or a zero length string.");
		}
		else {

			if (localGroup.length() > FEDERATE_GROUP_NAME_MAX_LENGTH) {
				errors.add("Local group name must not be longer than " + FEDERATE_GROUP_NAME_MAX_LENGTH + " characters.");
			}

			if (localGroup.replaceFirst("^[\\w\\d\\s\\.\\(\\)@#$_\\=\\-\\+\\[\\]\\{\\},\\/\\|\\\\]*$", "").length() > 0) {
				errors.add(" Local group name contains invalid characters.");
			}

		}
		return errors;
	}

	private List<String> getValidationErrors(Federation.FederationOutgoing outgoingConnection) {

		List<String> errors = new ArrayList<String>();

		//Validate display name
		errors.addAll(getOutgoingConnectionDisplayNameValidationErrors(outgoingConnection.getDisplayName()));
		if (outgoingConnection.getDisplayName() != null) {
			outgoingConnection.setDisplayName(outgoingConnection.getDisplayName().trim());
		}

		//Validate address
		if (outgoingConnection.getAddress() == null || outgoingConnection.getAddress().trim().length() == 0) {
			errors.add("Address must not be null or a zero length string.");
		} else {
			outgoingConnection.setAddress(outgoingConnection.getAddress().trim());

			if (outgoingConnection.getAddress().replaceFirst("^[\\w\\d_\\-\\.]*$", "").length() > 0) {
				errors.add("Invalid address value.");
			}
		}

		//Validate port
		if (outgoingConnection.getPort() < PORT_RANGE_LOW || outgoingConnection.getPort() > PORT_RANGE_HIGH) {
			errors.add("Invalid port value.");
		}

		//Validate reconnect interval
		if (outgoingConnection.getReconnectInterval() < 0) {
			errors.add("Invalid reconnect interval.");
		}

		//Validate max retries
		if (outgoingConnection.getFallback() != null) {
			if (outgoingConnection.isUnlimitedRetries() && !outgoingConnection.getFallback().isEmpty()) {
				errors.add("Max retries must be finite if there is a fallback connection");
			}
		}

		return errors;
	}

	private List<String> getValidationErrors(Federation.Federate federate) {

		List<String> errors = new ArrayList<String>();

		//Validate notes
		if (federate.getNotes() != null) {
			federate.setNotes(federate.getNotes().trim());

			if (federate.getNotes().length() > FEDERATE_NOTES_NAME_MAX_LENGTH) {
				errors.add("Federate notes must be no longer than " + FEDERATE_NOTES_NAME_MAX_LENGTH + " characters.");
			} else {
				String temp = federate.getNotes();
				if (temp.replaceFirst("^[A-Za-z0-9_\\s]+$", "").length() > 0) {
					errors.add("Invalid federate notes.");
				}
			}
		}

		return errors;
	}

	/**
	 * Validates outgoing connection display name for both save and query REST endpoints.
	 *
	 * @param displayName
	 * @return
	 */
	private List<String> getOutgoingConnectionDisplayNameValidationErrors(String displayName) {

		List<String> errors = new ArrayList<String>();

		//Validate input name
		if (displayName == null || displayName.trim().length() == 0) {
			errors.add("Missing outgoing connection display name.");
		} else {
			displayName = displayName.trim();

			if (displayName.length() > OUTGOING_CONNECTION_DISPLAY_NAME_MAX_LENGTH) {
				errors.add("Outgoing connection display name must be no longer than " + OUTGOING_CONNECTION_DISPLAY_NAME_MAX_LENGTH + " characters.");
			} else {
				if (displayName.replaceFirst("^[A-Za-z0-9_\\s]+$", "").length() > 0) {
					errors.add("Invalid outgoing connection display name.");
				}
			}
		}

		return errors;
	}

	/**
	 * Validates certificate fingerprint
	 *
	 * @param fingerPrint
	 * @return
	 */
	private List<String> getCertificateFingerprintValidationErrors(String fingerPrint) {

		List<String> errors = new ArrayList<String>();

		//Validate input name
		if (fingerPrint == null || fingerPrint.trim().length() == 0) {
			errors.add("Missing certificate fingerprint.");
		} else {
			fingerPrint = fingerPrint.trim();
			if (fingerPrint.replaceFirst("^(([A-Fa-f0-9]){2,2}:){31}([A-Fa-f0-9]){2,2}$", "").length() > 0) {
				errors.add("Fingerprint is not valid.");
			}
		}

		return errors;
	}

	private String getCertFingerprint(@NotNull X509Certificate cert) {

		//NOTE: This code is copied from RemoteUtil in the core router to ensure the fingerprint is computed consistently (though
		//technically this is not necessary provided the hash of the encoding is calculated the same way when the certificate list is sent
		//to the browser and when one comes in for deletion, etc.).

		//We need to calculate the hash of the encoded cert for later use (during deletions) to ensure we can pick out the certificate uniquely
		//(issuerDN and serial number alone will not work).

		HashCode hc = null;

		try {
			hc = Hashing.sha256().newHasher().putBytes(cert.getEncoded()).hash();
		} catch (CertificateEncodingException e) {
			throw new RuntimeException(e);
		}

		String fingerprint = DatatypeConverter.printHexBinary(hc.asBytes());

		StringBuilder fpBuilder = new StringBuilder();

		for (int i = 0; i < fingerprint.length(); i++) {
			if (i > 0 && i % 2 == 0) {
				fpBuilder.append(':');
			}

			fpBuilder.append(fingerprint.charAt(i));
		}

		return fpBuilder.toString();
	}

	enum DirectionValue {

		INBOUND("Inbound"),
		OUTBOUND("Outbound"),
		BOTH("Both (Inbound/Outbound)");

		private final String value;

		DirectionValue(String value) {
			this.value = value;
		}

		public String value() {
			return value;
		}

		public static DirectionValue fromValue(String v) {

			DirectionValue result = null;

			for (DirectionValue c: DirectionValue.values()) {
				if (c.value.equals(v)) {
					result = c;
				}
			}

			return result;
		}
	}

	public static final int FEDERATE_GROUP_NAME_MAX_LENGTH = 255;
	public static final int FEDERATE_NAME_MAX_LENGTH = 255;
	public static final int OUTGOING_CONNECTION_DISPLAY_NAME_MAX_LENGTH = 30;
	public static final int FEDERATE_NOTES_NAME_MAX_LENGTH = 30;

	private static final int CERT_FILE_UPLOAD_SIZE = 1048576;

	private static final int PORT_RANGE_LOW = 1;
	private static final int PORT_RANGE_HIGH = 65535;


	@RequestMapping(value = "/fednum", method = RequestMethod.GET)
	public int getNum() {
		return federationInterface.incrementAndGetCounter();
	}
	
	@RequestMapping(value = "/clearFederationEvents", method = RequestMethod.GET)
	public void clearDisruptionData() {
		fedEventRepository.clearFederationEvents();
		
		logger.info("Federation events cleared");
	}
	
	@RequestMapping(value = "/federatemissions/{federateId}", method = RequestMethod.PUT)
	public ResponseEntity<ApiResponse<FederateMissionPerConnectionSettings>> updateFederateMissions(@PathVariable String federateId, @RequestBody FederateMissionPerConnectionSettings federateMissionPerConnectionSettings) {

		ResponseEntity<ApiResponse<FederateMissionPerConnectionSettings>> result = null;

		logger.trace("RMI federationInterface: " + federationInterface);
		List<String> errors = new ArrayList<String>();

		try {
			federationInterface.updateFederateMissionSettings(federateId, federateMissionPerConnectionSettings.isMissionFederateDefault(), federateMissionPerConnectionSettings.getMissions());

			result = new ResponseEntity<ApiResponse<FederateMissionPerConnectionSettings>>(new ApiResponse<FederateMissionPerConnectionSettings>(Constants.API_VERSION,
					FederateMissionPerConnectionSettings.class.getName(), federateMissionPerConnectionSettings), HttpStatus.OK);
			
		} catch (Exception e) {
			logger.error("Exception updating federate details.", e);
			errors.add(e.getMessage());
		}

		if (result == null) {
			//This would be an error condition (not an empty input list or bad request)
			result = new ResponseEntity<ApiResponse<FederateMissionPerConnectionSettings>>(new ApiResponse<FederateMissionPerConnectionSettings>(Constants.API_VERSION,
					String.class.getName(), null, errors), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return result;
	}
}
