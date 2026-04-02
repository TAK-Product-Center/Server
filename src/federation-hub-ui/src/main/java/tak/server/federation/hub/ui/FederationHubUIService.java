package tak.server.federation.hub.ui;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.owasp.esapi.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.util.UriComponentsBuilder;

import com.bbn.roger.fig.FederationUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Strings;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tak.server.federation.FederateGroup;
import tak.server.federation.FederationPolicyGraph;
import tak.server.federation.hub.broker.FederationHubBroker;
import tak.server.federation.hub.broker.FederationHubBrokerGlobalMetrics;
import tak.server.federation.hub.broker.FederationHubBrokerMetrics;
import tak.server.federation.hub.broker.FederationHubServerConfig;
import tak.server.federation.hub.broker.HubConnectionInfo;
import tak.server.federation.hub.plugin.FederationHubPluginMetadata;
import tak.server.federation.hub.plugin.PluginRegistrySyncService;
import tak.server.federation.hub.plugin.manager.FederationHubPluginManager;
import tak.server.federation.hub.policy.FederationHubPolicyManager;
import tak.server.federation.hub.ui.graph.FederateCell;
import tak.server.federation.hub.ui.graph.FederationOutgoingCell;
import tak.server.federation.hub.ui.graph.FederationTokenGroupCell;
import tak.server.federation.hub.ui.graph.FederationUIPolicyModel;
import tak.server.federation.hub.ui.graph.GroupCell;
import tak.server.federation.hub.ui.jwt.AuthRequest;
import tak.server.federation.hub.ui.jwt.AuthResponse;
import tak.server.federation.jwt.FederationJwtUtils;
import tak.server.federation.jwt.JwtTokenRequestModel;
import tak.server.federation.jwt.JwtTokenResponseModel;

@RequestMapping("/api")
@RestController
@Order(Ordered.LOWEST_PRECEDENCE)
public class FederationHubUIService {

    private static final Logger logger = LoggerFactory.getLogger(FederationHubUIService.class);

    private static final int CERT_FILE_UPLOAD_SIZE = 1048576;

    private String activePolicyName = null;

    private Validator validator = new MartiValidator();
    
    @Autowired
    private FederationHubPolicyManager fedHubPolicyManager;
    
    @Autowired
    private FederationHubBroker fedHubBroker;
    
    @Autowired
    private PluginRegistrySyncService pluginRegistrySyncService;
    
    @Autowired
    private FederationHubPluginManager pluginManager;

    @Autowired AuthenticationManager authManager;

    @Autowired JwtTokenUtil jwtUtil;

    @Autowired
    FederationHubUIConfig fedHubConfig;
    
    // this will return 401 or 403 if not admin
    @RequestMapping(value = "/isAdmin", method = RequestMethod.GET)
    public void handleAuthRequest(HttpServletResponse response) {
    	
    }
    
	@RequestMapping(value = "/generateJwtToken", method = RequestMethod.POST)
	public ResponseEntity<JwtTokenResponseModel> generateJwtToken(@RequestBody JwtTokenRequestModel tokenRequest) {
		try {
			String token = FederationJwtUtils.getInstance(fedHubConfig).createFedhubToken(tokenRequest.getAttributes(), tokenRequest.getExpiration());

			JwtTokenResponseModel response = new JwtTokenResponseModel();
			response.setToken(token);
			response.setExpiration(tokenRequest.getExpiration());
			
			return new ResponseEntity<>(response, new HttpHeaders(), HttpStatus.OK);
		} catch (Exception e) {
			logger.error("error with generateJwtToken", e);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
    
    @RequestMapping(value="/allowFlowIndicators", method=RequestMethod.GET)
	public ResponseEntity<Boolean> isAllowFlowIndicators() {
		try {
		    return new ResponseEntity<>(fedHubConfig.isEnableFlowIndicators() ,new HttpHeaders(), HttpStatus.OK);
		} catch (Exception e) {
			logger.error("error with isAllowFlowIndicators", e);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

    @RequestMapping(value = "/federation/{federationId}", method = RequestMethod.GET)
    @ResponseBody
    public FederationUIPolicyModel getFederationById(@PathVariable String federationId) {
        return fedHubPolicyManager.getPolicyGraph(federationId).getModel();
    }

    @RequestMapping(value = "/federations", method = RequestMethod.GET)
    @ResponseBody
    public Collection<FederationUIPolicyModel> getAllFederations() {
        return fedHubPolicyManager.getAllPolicies().values();
    }

    @RequestMapping(value = "/getActivePolicy", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Object> getActivePolicy() {
    	 FederationPolicyGraph policy = fedHubPolicyManager.getActivePolicyGraph();
         if (policy == null) {
             return new ResponseEntity<>("The active federation policy does not have any graph data associated with it", new HttpHeaders(), HttpStatus.NOT_FOUND);
         }
         
         FederationUIPolicyModel policyModel = policy.getModel();
         activePolicyName = policyModel.getName();
         
         return new ResponseEntity<>(policyModel, new HttpHeaders(), HttpStatus.OK);
    }
    
    @RequestMapping(value = "/graphAsJson/{federationId}", method = RequestMethod.GET)
    @ResponseBody
    public FederationUIPolicyModel getGraphAsJson(@PathVariable String federationId) {
        return fedHubPolicyManager.getPolicyGraph(federationId).getModel();
    }

    @RequestMapping(value = "/graphAsYaml/{federationId}", method = RequestMethod.GET)
    @ResponseBody
    public String getGraphAsYaml(@PathVariable String federationId) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            String result =  mapper.writeValueAsString(fedHubPolicyManager.getPolicyGraph(federationId).getModel());
            return result;
        } catch (JsonProcessingException jpe) {
                return "Error parsing Java object to YAML: " + jpe;
        }
    }
    
    // used when creating a new policy
    @RequestMapping(value = "/saveFederationPolicy", method = RequestMethod.POST)
    @ResponseBody
    public FederationUIPolicyModel saveFederationPolicy(RequestEntity<FederationUIPolicyModel> requestEntity) {
    	FederationUIPolicyModel policy = requestEntity.getBody();

        return fedHubPolicyManager.savePolicyFile(policy);
    }

    // used to update the policy graph nodes
    @RequestMapping(value = "/saveFederationGraphPolicy", method = RequestMethod.POST)
    @ResponseBody
    public FederationUIPolicyModel saveFederationGraphPolicy(RequestEntity<FederationUIPolicyModel> requestEntity) {
    	FederationUIPolicyModel policy = requestEntity.getBody();
        
        return fedHubPolicyManager.saveGraphPolicyFile(policy);
    }
    
    // used to update the persistent settings 
    @RequestMapping(value = "/saveFederationSettingsPolicy", method = RequestMethod.POST)
    @ResponseBody
    public FederationUIPolicyModel saveFederationSettingsPolicy(RequestEntity<FederationUIPolicyModel> requestEntity) {
    	FederationUIPolicyModel policy = requestEntity.getBody();
        
        return fedHubPolicyManager.saveSettingsPolicyFile(policy);
    }
    
    // used to update the plugins policy
    @RequestMapping(value = "/saveFederationPluginsPolicy", method = RequestMethod.POST)
    @ResponseBody
    public FederationUIPolicyModel saveFederationPluginsPolicy(RequestEntity<FederationUIPolicyModel> requestEntity) {
    	FederationUIPolicyModel policy = requestEntity.getBody();
    	
    	return fedHubPolicyManager.savePluginsPolicyFile(policy);
    }

    @RequestMapping(value = "/activateFederationPolicy/{federationId}")
    public ResponseEntity<Void> activateFederationPolicy(@PathVariable String federationId) {
    	FederationPolicyGraph policyGraph = fedHubPolicyManager.getPolicyGraph(federationId);
    	if (policyGraph != null) {
            try {
            	fedHubPolicyManager.setPolicyGraph(policyGraph);
                
            	activePolicyName = policyGraph.getName();

                fedHubBroker.updatePolicy(policyGraph.getModel());
                fedHubPolicyManager.savePolicyFile(policyGraph.getModel());

                return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);
            } catch (Exception e) {
                logger.error("Could save and activate policy", e);
                return new ResponseEntity<>(new HttpHeaders(), HttpStatus.BAD_REQUEST);
            }
        }
        return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = "/getKnownCaGroups", method = RequestMethod.GET)
    public ResponseEntity<List<RemoteGroup>> getKnownCaGroups(HttpServletRequest request) {
    	return new ResponseEntity<>(
                federateGroupsToGroupHolders(fedHubPolicyManager.getCaGroups()),
                new HttpHeaders(),
                HttpStatus.OK);
    }
    
    @RequestMapping(value = "/getCaGroupNames", method = RequestMethod.GET)
    public ResponseEntity<List<String>> getCaGroupNames(HttpServletRequest request) {
    	List<String> names = new ArrayList<>();
    	FederationUIPolicyModel activePolicy = fedHubPolicyManager.getActivePolicyGraph().getModel();
    	activePolicy.getGraphData().getNodes().forEach(cell -> {
			if (cell instanceof GroupCell) {
				GroupCell gCell = (GroupCell) cell;
				names.add(gCell.getProperties().getName());
			}
            if (cell instanceof FederationTokenGroupCell) {
                FederationTokenGroupCell tCell = (FederationTokenGroupCell) cell;
                names.add(tCell.getProperties().getName());
            }
    	});
    	
    	
        return new ResponseEntity<>(names,
            new HttpHeaders(),
            HttpStatus.OK);
    }

    @RequestMapping(value = "/getKnownGroupsForGraphNode/{graphNodeId}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<List<String>> getKnownGroupsForGraphNode(@PathVariable String graphNodeId) {
    	List<String> groupsForNode = new ArrayList<>();

    	FederationUIPolicyModel activePolicy = fedHubPolicyManager.getActivePolicyGraph().getModel();
    	activePolicy.getGraphData().getNodes().forEach(cell -> {
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
                if (cell instanceof FederationTokenGroupCell) {
                    FederationTokenGroupCell tCell = (FederationTokenGroupCell) cell;
                    groupsForNode.addAll(fedHubBroker.getGroupsForNode(tCell.getProperties().getName()));
                }
    		}
    	});

        return new ResponseEntity<List<String>>(groupsForNode, new HttpHeaders(), HttpStatus.OK);
    }

    @RequestMapping(value = "/getActiveConnections", method = RequestMethod.GET)
    public ResponseEntity<List<HubConnectionInfo>> getActiveConnections() {
    	return new ResponseEntity<List<HubConnectionInfo>>(fedHubBroker.getActiveConnections(), new HttpHeaders(), HttpStatus.OK);
    }

    @RequestMapping(value = "/getBrokerMetrics", method = RequestMethod.GET)
    public ResponseEntity<FederationHubBrokerMetrics> getFederationHubBrokerMetrics() {
        return new ResponseEntity<FederationHubBrokerMetrics>(fedHubBroker.getFederationHubBrokerMetrics(), new HttpHeaders(), HttpStatus.OK);
    }

    @RequestMapping(value = "/getBrokerGlobalMetrics", method = RequestMethod.GET)
    public ResponseEntity<FederationHubBrokerGlobalMetrics> getFederationHubBrokerGlobalMetrics() {
        return new ResponseEntity<FederationHubBrokerGlobalMetrics>(fedHubBroker.getFederationHubBrokerGlobalMetrics(), new HttpHeaders(), HttpStatus.OK);
    }

    @RequestMapping(value = "/addNewGroupCa", method = RequestMethod.POST)
    public ResponseEntity<Void> addNewGroupCa(@RequestPart("file") MultipartFile certificateFile) {
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
        return new ResponseEntity<>(new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @RequestMapping(value = "/deleteGroupCa/{uid}", method = RequestMethod.DELETE)
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
    
    @RequestMapping(value = "/disconnectFederate/{connectionId}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> disconnectFederate(@PathVariable("connectionId") String connectionId) {
    	try {
        	fedHubBroker.disconnectFederate(connectionId);
    		return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);
    	} catch (Exception e) {
    		logger.error("error with deleteGroupCa", e);
    		return new ResponseEntity<>(new HttpHeaders(), HttpStatus.BAD_REQUEST);
		}
    }
    
    @RequestMapping(value="/getSelfCaFile", method=RequestMethod.GET)
    public ResponseEntity<byte[]> getSelfCaFile() {
    	try {
    		byte[] contents = fedHubBroker.getSelfCaFile();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            return new ResponseEntity<byte[]>(contents, headers, HttpStatus.OK);
    	} catch (Exception e) {
    		logger.error("error with getSelfCaFile", e);
    		 return new ResponseEntity<byte[]>(new HttpHeaders(), HttpStatus.BAD_REQUEST);
		}
    }
    
    @RequestMapping(value = "/getBrokerConfig", method = RequestMethod.GET)
	public ResponseEntity<FederationHubServerConfig> addFederateGroup() {
        return new ResponseEntity<FederationHubServerConfig>(fedHubBroker.getFederationHubBrokerConfig(), new HttpHeaders(), HttpStatus.OK);
    }
    
    @RequestMapping(value = "/updateBrokerConfig", method = RequestMethod.POST)
   	public ResponseEntity<FederationHubServerConfig> updateBrokerConfig(@RequestBody FederationHubServerConfig brokerConfig) {
    	return new ResponseEntity<FederationHubServerConfig>(fedHubBroker.saveFederationHubServerConfig(brokerConfig), new HttpHeaders(), HttpStatus.OK);
    }
    
    @RequestMapping(value="/restartBroker", method=RequestMethod.GET)
	public ResponseEntity<Void> serverRestart() {
		try {		
			logger.info("Restarting Broker Service");

			int code = killBrokerProcess();
			
			if (code != 0) {
				return new ResponseEntity<>(new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
			}
			
		    // start
		    String startCmd= "./federation-hub-broker.sh &> /opt/tak/federation-hub/logs/federation-hub-broker-console.log &";
			String[] startCmds = new String[] {"bash", "-c", startCmd};
			
			ProcessBuilder startProcessBuilder = new ProcessBuilder(startCmds);
			startProcessBuilder.directory(new File("/opt/tak/federation-hub/scripts"));
		    Process startProcess = startProcessBuilder.start();
		    
		    startProcess.waitFor();
		    
		    canAccessBrokerProcess().get();
		    logger.info("Federation Hub Broker is available");
		    
		    return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);
		} catch (Exception e) {
			logger.error("error with restartBroker", e);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
    
    private int killBrokerProcess() {
        try {
            ProcessHandle.allProcesses()
                .filter(ph -> ph.info().commandLine().isPresent() &&
                              ph.info().commandLine().get().contains("federation-hub-broker"))
                .forEach(ph -> {
                    logger.info("Destroying PID=" + ph.pid());
                    ph.destroyForcibly(); // SIGKILL equivalent
                });
            return 0;
        } catch (Exception e) {
            logger.error("error with killBrokerProcess", e);
            return 2;
        }
    }
    
    @RequestMapping(value = "/getRegisteredPlugins", method = RequestMethod.GET)
    @ResponseBody
    public Collection<FederationHubPluginMetadata> getRegisteredPlugins() {
        return pluginRegistrySyncService.getRegisteredPlugins();
    }
    
    private boolean canAccessBrokerProcessServices() throws Exception {
    	fedHubBroker.getActiveConnections();
		return true;
	}
    
    private CompletableFuture<Boolean> canAccessBrokerProcess() {
		try {
			logger.info("Waiting for the Broker process...");
			return CompletableFuture.completedFuture(canAccessBrokerProcessServices());
		} catch (Exception e) {
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e1) {
				logger.error("interruped sleep", e1);
			}
			return canAccessBrokerProcess();
		}
	}

    private String sha256(String input) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] bytes = input.getBytes("US-ASCII");
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return Base64.getUrlEncoder().withoutPadding().encodeToString(md.digest(bytes));
    }

    private List<RemoteGroup> federateGroupsToGroupHolders(Collection<FederateGroup> groups) {
    	X509Certificate selfCa = null;
    	try {
        	byte[] selfCaB = fedHubBroker.getSelfCaFile();
        	selfCa = FederationUtils.loadX509CertFromBytes(selfCaB);
		} catch (Exception e) {
			logger.error("error computing self ca", e);
		}
    	    	
    	Map<String, X509Certificate> cas = fedHubBroker.getCAsFromFile();
        List<RemoteGroup> groupList = new LinkedList<>();
        for (FederateGroup group : groups) {
            String fedId = group.getFederateIdentity().getFedId();

            RemoteGroup remoteGroup = new RemoteGroup();
        	remoteGroup.setUid(fedId);
            if (cas.containsKey(fedId)) {
            	X509Certificate ca = cas.get(fedId);
            	remoteGroup.setIssuer(ca.getIssuerX500Principal().getName());
            	remoteGroup.setSubject(ca.getSubjectX500Principal().getName());
            	remoteGroup.setIsHubGroup(ca.equals(selfCa));
            }

            groupList.add(remoteGroup);
        }
        return groupList;
    }

    private class RemoteGroup {
    	private String uid;
    	private String issuer;
    	private String subject;
    	private boolean isHubGroup = false;
    	
    	public String getUid() {
			return uid;
		}
		public void setUid(String uid) {
			this.uid = uid;
		}
		public String getIssuer() {
			return issuer;
		}
		public void setIssuer(String issuer) {
			this.issuer = issuer;
		}
		public String getSubject() {
			return subject;
		}
		public void setSubject(String subject) {
			this.subject = subject;
		}
		public boolean isHubGroup() {
			return isHubGroup;
		}
		public void setIsHubGroup(boolean isHubGroup) {
			this.isHubGroup = isHubGroup;
		}
    }
}