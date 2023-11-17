package tak.server.federation.hub.ui;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import tak.server.federation.FederateGroup;
import tak.server.federation.FederationException;
import tak.server.federation.FederationPolicyGraph;
import tak.server.federation.hub.broker.FederationHubBroker;
import tak.server.federation.hub.broker.HubConnectionInfo;
import tak.server.federation.hub.policy.FederationHubPolicyManager;
import tak.server.federation.hub.policy.FederationPolicy;
import tak.server.federation.hub.ui.graph.EdgeFilter;
import tak.server.federation.hub.ui.graph.FederateCell;
import tak.server.federation.hub.ui.graph.FederationOutgoingCell;
import tak.server.federation.hub.ui.graph.FederationPolicyModel;
import tak.server.federation.hub.ui.graph.FilterUtils;
import tak.server.federation.hub.ui.graph.GroupCell;

@RequestMapping("/")
public class FederationHubUIService {

    private static final Logger logger = LoggerFactory.getLogger(FederationHubUIService.class);
    
    private static final int CERT_FILE_UPLOAD_SIZE = 1048576;

    private String activePolicyName = null;
    private Map<String, FederationPolicyModel> cachedPolicies = new HashMap<>();

    @Autowired
    private FederationHubPolicyManager fedHubPolicyManager;

    @Autowired
    private FederationHubBroker fedHubBroker;


    @RequestMapping(value = "/home", method = RequestMethod.GET)
    public String getHome() {
        return "home";
    }

    @RequestMapping(value = "/fig/saveFederation", method = RequestMethod.POST)
    @ResponseBody
    public FederationPolicyModel saveFederation(RequestEntity<FederationPolicyModel> requestEntity) {
        FederationPolicyModel policy = requestEntity.getBody();
        if (policy != null) {
        	this.cachedPolicies.put(policy.getName(), policy);
        }
        
        return policy;
    }

    @RequestMapping(value = "/fig/federation/{federationId}", method = RequestMethod.GET)
    @ResponseBody
    public FederationPolicyModel getFederationById(@PathVariable String federationId) {
        return this.cachedPolicies.get(federationId);
    }

    @RequestMapping(value = "/fig/federations", method = RequestMethod.GET)
    @ResponseBody
    public Collection<FederationPolicyModel> getAllFederations() {
        return this.cachedPolicies.values();
    }

    @RequestMapping(value = "/fig/getActivePolicy", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Object> getActivePolicy() {
        if (isUpdateActiveFederation()) {
            FederationPolicyModel policyModel = getActivePolicyAsPolicyModel();
            if (policyModel == null) {
                return new ResponseEntity<>("The active federation policy does not have any graph data associated with it", new HttpHeaders(), HttpStatus.NOT_FOUND);
            }

            if (policyModel.getName() == null || policyModel.getName().equals("")) {
                policyModel.setName("CURRENT_POLICY");
            }

            this.cachedPolicies.put(policyModel.getName(), policyModel);
            activePolicyName = policyModel.getName();
            return new ResponseEntity<>(policyModel, new HttpHeaders(), HttpStatus.OK);
        }
        return new ResponseEntity<>(new HttpHeaders(), HttpStatus.FORBIDDEN);
    }

    @RequestMapping(value = "/fig/knownFilter", method = RequestMethod.GET)
    @ResponseBody
    public Collection<EdgeFilter> getKnownFilters() {
        return getKnownFiltersFromPolicy();
    }

    @RequestMapping(value = "/fig/graphAsJson/{federationId}", method = RequestMethod.GET)
    @ResponseBody
    public FederationPolicy getGraphAsJson(@PathVariable String federationId) {
        return this.cachedPolicies.get(federationId).getFederationPolicyObjectFromModel();
    }

    @RequestMapping(value = "/fig/graphAsYaml/{federationId}", method = RequestMethod.GET)
    @ResponseBody
    public String getGraphAsYaml(@PathVariable String federationId) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            String result =  mapper.writeValueAsString(
                this.cachedPolicies.get(federationId).getFederationPolicyObjectFromModelWithoutGraphData());
            return result;
        } catch (JsonProcessingException jpe) {
                return "Error parsing Java object to YAML: " + jpe;
        }
    }

    @RequestMapping(value = "/fig/updateFederationManager/{federationId}")
    public ResponseEntity<Void> updateFederationManager(@PathVariable String federationId) {
        if (isUpdateActiveFederation()) {
            if (this.cachedPolicies.containsKey(federationId)) {
                FederationPolicyModel policyModel = cachedPolicies.get(federationId);
                try {
                    if (policyModel.getName().equals(activePolicyName)) {
                        fedHubPolicyManager.updatePolicyGraph(policyModel, null);
                    } else {
                        fedHubPolicyManager.setPolicyGraph(policyModel, null);
                        activePolicyName = policyModel.getName();
                    }
                    
                    fedHubBroker.updatePolicy(policyModel);

                    return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);
                } catch (FederationException e) {
                    logger.error("Could not save the policy graph to the federation manager", e);
                    return new ResponseEntity<>(new HttpHeaders(), HttpStatus.BAD_REQUEST);
                }

            }
            return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(new HttpHeaders(), HttpStatus.FORBIDDEN);
    }

    @RequestMapping(value = "/fig/updateFederationManagerAndFile/{federationId}")
    public ResponseEntity<Void> updateFederationManagerAndFile(@PathVariable String federationId) {
        if (isUpdateActiveFederation()) {
            if (this.cachedPolicies.containsKey(federationId)) {
                FederationPolicyModel policyModel = cachedPolicies.get(federationId);
                try {
                    if (policyModel.getName().equals(activePolicyName)) {
                        fedHubPolicyManager.updatePolicyGraph(policyModel,
                            policyModel.getFederationPolicyObjectFromModel());
                    } else {
                        fedHubPolicyManager.setPolicyGraph(policyModel,
                            policyModel.getFederationPolicyObjectFromModel());
                        activePolicyName = policyModel.getName();
                    }
                    
                    fedHubBroker.updatePolicy(policyModel);
                    
                    return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);
                } catch (FederationException e) {
                    logger.error("Could not save the policy graph to the federation manager", e);
                    return new ResponseEntity<>(new HttpHeaders(), HttpStatus.BAD_REQUEST);
                }
            }
            return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(new HttpHeaders(), HttpStatus.FORBIDDEN);
    }

    @RequestMapping(value = "/fig/getKnownCaGroups", method = RequestMethod.GET)
    public ResponseEntity<List<GroupHolder>> getKnownCaGroups() {	    	
        if (isUpdateActiveFederation()) {
            return new ResponseEntity<>(
                federateGroupsToGroupHolders(fedHubPolicyManager.getCaGroups()),
                new HttpHeaders(),
                HttpStatus.OK);
        }
        return new ResponseEntity<>(new HttpHeaders(), HttpStatus.FORBIDDEN);
    }
    
    @RequestMapping(value = "/fig/getKnownGroupsForGraphNode/{graphNodeId}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<List<String>> getKnownGroupsForGraphNode(@PathVariable String graphNodeId) {
    	List<String> groupsForNode = new ArrayList<>();
    	
    	FederationPolicyModel activePolicy = getActivePolicyAsPolicyModel();
    	activePolicy.getCells().forEach(cell -> {
    		if (cell.getId().equals(graphNodeId)) {
    			if (cell instanceof FederateCell) {
    				FederateCell fCell = (FederateCell) cell;
    				groupsForNode.addAll(fedHubBroker.getGroupsForNode(fCell.getProperties().getName()));
    			}
    			if (cell instanceof GroupCell) {
    				GroupCell gCell = (GroupCell) cell;
    				groupsForNode.addAll(fedHubBroker.getGroupsForNode(gCell.getProperties().getName()));
    			}
    			if (cell instanceof FederationOutgoingCell) {
    				FederationOutgoingCell oCell = (FederationOutgoingCell) cell;
    				groupsForNode.addAll(fedHubBroker.getGroupsForNode(oCell.getProperties().getName()));
    			}
    		}
    	});
    	
        return new ResponseEntity<List<String>>(groupsForNode, new HttpHeaders(), HttpStatus.OK);
    }
    
    @RequestMapping(value = "/fig/getActiveConnections", method = RequestMethod.GET)
    public ResponseEntity<List<HubConnectionInfo>> getActiveConnections() {
    	return new ResponseEntity<List<HubConnectionInfo>>(fedHubBroker.getActiveConnections(), new HttpHeaders(), HttpStatus.OK);
    }

    @RequestMapping(value = "/fig/addNewGroupCa", method = RequestMethod.POST)
    public ResponseEntity<Void> addNewGroupCa(@RequestPart("file") MultipartFile certificateFile) {
        if (isUpdateActiveFederation()) {
            //The size is checked upstream by org.springframework.web.multipart.commons.CommonsMultipartResolver for better security (see api-context.xml and keep
            //CERT_FILE_UPLOAD_SIZE defined here in sync with the value provided there). The size check is duplicated here in case the global value in api-context.xml
            //needs to be expanded in the future to accommodate other transactions. The change in api-context.xml does not break the Enterrprise Sync functionality, which accepts larger files.

            //Also, checking mime type here (should be application/x-x509-ca-cert) is not going to be effective. Each OS/Browser has a different algorithm for determining mime type on file uploads and only
            //in some cases is the file content actually part of the check; in many cases the check is dependent on the actual client OS installation and regis     tration of file extensions.
            //In any event, the value can be easily overridden for an upload, so the benefit is very limited/suggestive.
            try {
                if (certificateFile != null && certificateFile.getSize() < CERT_FILE_UPLOAD_SIZE) {
                    CertificateFactory factory = CertificateFactory.getInstance("X.509");
                    X509Certificate x509Certificate = (X509Certificate)factory
                        .generateCertificate(certificateFile.getInputStream());
                    fedHubBroker.addGroupCa(x509Certificate);
                    return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);
                }
            } catch (CertificateException | IOException e) {
                return new ResponseEntity<>(new HttpHeaders(), HttpStatus.BAD_REQUEST);
            }
        }
        return new ResponseEntity<>(new HttpHeaders(), HttpStatus.FORBIDDEN);
    }
    
    @RequestMapping(value = "/fig/deleteGroupCa/{uid}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteGroupCa(@PathVariable("uid") String uid) {
    	try {
        	Collection<FederateGroup> federateGroups = fedHubPolicyManager.getCaGroups();
    		if (federateGroups != null) {
    			for (FederateGroup fg : federateGroups) {
    				if (fg.getFederateIdentity().getFedId().equals(uid)) {
    					fedHubBroker.deleteGroupCa(uid);
    				}
    			}
    		}
    		return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);
    	} catch (Exception e) {
    		logger.error("error with deleteGroupCa", e);
    		return new ResponseEntity<>(new HttpHeaders(), HttpStatus.BAD_REQUEST);
		}
    }
    
    private boolean isUpdateActiveFederation() {
        /* TODO do we really need to configure this? */
        return true;
    }

    private Collection<EdgeFilter> getKnownFiltersFromPolicy() {
        return FilterUtils.getKnownFiltersFromPolicy().values();
    }

    private FederationPolicyModel getActivePolicyAsPolicyModel() {
        // Production code.
        FederationPolicyGraph activePolicy = fedHubPolicyManager.getPolicyGraph();
        Map<String, Object> additionalData = activePolicy.getAdditionalData();
        
   
        // UI Test code.
        //FederationPolicyGraph testActualPolicy = cachedPolicys.values().iterator().next().getPolicyGraphFromModel();
        //Map<String, Object> additionalData =  testActualPolicy.getAdditionalData();

        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(additionalData.get("uiData"), FederationPolicyModel.class);
    }

    private List<GroupHolder> federateGroupsToGroupHolders(Collection<FederateGroup> groups) {
        List<GroupHolder> groupList = new LinkedList<>();
        for (FederateGroup group : groups) {
            GroupHolder groupHolder = new GroupHolder(group.getFederateIdentity().getFedId());
            groupList.add(groupHolder);
        }
        return groupList;
    }
}
