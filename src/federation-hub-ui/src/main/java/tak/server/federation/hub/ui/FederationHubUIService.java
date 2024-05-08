package tak.server.federation.hub.ui;

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

import org.owasp.esapi.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Strings;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tak.server.federation.FederateGroup;
import tak.server.federation.FederationException;
import tak.server.federation.FederationPolicyGraph;
import tak.server.federation.hub.broker.FederationHubBroker;
import tak.server.federation.hub.broker.FederationHubBrokerMetrics;
import tak.server.federation.hub.broker.HubConnectionInfo;
import tak.server.federation.hub.policy.FederationHubPolicyManager;
import tak.server.federation.hub.policy.FederationPolicy;
import tak.server.federation.hub.ui.graph.EdgeFilter;
import tak.server.federation.hub.ui.graph.FederateCell;
import tak.server.federation.hub.ui.graph.FederationOutgoingCell;
import tak.server.federation.hub.ui.graph.FederationPolicyModel;
import tak.server.federation.hub.ui.graph.FilterUtils;
import tak.server.federation.hub.ui.graph.GroupCell;
import tak.server.federation.hub.ui.jwt.AuthRequest;
import tak.server.federation.hub.ui.jwt.AuthResponse;
import tak.server.federation.hub.ui.keycloak.AuthCookieUtils;

@RequestMapping("/")
@RestController
@Order(Ordered.LOWEST_PRECEDENCE)
public class FederationHubUIService implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(FederationHubUIService.class);

    private static final int CERT_FILE_UPLOAD_SIZE = 1048576;

    private String activePolicyName = null;
    private Map<String, FederationPolicyModel> cachedPolicies = new HashMap<>();

    private Validator validator = new MartiValidator();
    @Autowired
    private FederationHubPolicyManager fedHubPolicyManager;
    @Autowired
    private FederationHubBroker fedHubBroker;

    @Autowired AuthenticationManager authManager;

    @Autowired JwtTokenUtil jwtUtil;

    @Autowired
    FederationHubUIConfig fedHubConfig;
    
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
    	// hydrate cache
    	getActivePolicy();
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
	public ModelAndView getLoginPage() {
		return new ModelAndView(new InternalResourceView("/login/index.html"));
	}

    @RequestMapping(value = "/login/authserver", method = RequestMethod.GET)
    public ResponseEntity<String>  getAuthServerName() {

        if (fedHubConfig.isAllowOauth()) {
        	return new ResponseEntity<>("{\"data\":\"" + fedHubConfig.getKeycloakServerName() + "\"}", new HttpHeaders(), HttpStatus.OK);
        } else {
        	return new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "/login/auth", method = RequestMethod.GET)
    public void handleAuthRequest(HttpServletResponse response) {
        try {
            // get the auth server config
            if (!fedHubConfig.isAllowOauth() ||
            		Strings.isNullOrEmpty(fedHubConfig.getKeycloakAuthEndpoint()) ||
            		 Strings.isNullOrEmpty(fedHubConfig.getKeycloakTokenEndpoint()) ||
            		 Strings.isNullOrEmpty(fedHubConfig.getKeycloakrRedirectUri())) {
            	throw  new IllegalStateException("missing auth server config");
            }

            // create a random state value to track the auth request
            SecureRandom secureRandom = new SecureRandom();
            byte[] code = new byte[32];
            secureRandom.nextBytes(code);
            String state = Base64.getUrlEncoder().withoutPadding().encodeToString(code);

            // attach the state to a cookie that we will validate in the redirect
            response.addHeader(HttpHeaders.SET_COOKIE, AuthCookieUtils.createCookie(
                    "state", state, -1, false).toString());

            // build the auth url
            UriComponentsBuilder uriComponentBuilder =
                    UriComponentsBuilder.fromHttpUrl(fedHubConfig.getKeycloakAuthEndpoint())
                            .queryParam("response_type", "code")
                            .queryParam("client_id", fedHubConfig.getKeycloakClientId())
                            .queryParam("redirect_uri", fedHubConfig.getKeycloakrRedirectUri())
                            .queryParam("state", sha256(state));

//            // add the scope if provided
//            if (authServer.getScope() != null) {
//                uriComponentBuilder = uriComponentBuilder
//                        .queryParam("scope", authServer.getScope());
//            }

            // send the redirect
            response.sendRedirect(uriComponentBuilder.toUriString());

        } catch (Exception e) {
            logger.error("exception in handleAuth", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/login/redirect", method = RequestMethod.GET)
    public ModelAndView handleRedirect(
            @RequestParam(value = "code", required = true) String code,
            @RequestParam(value = "state", required = true) String state,
            @CookieValue(value = "state", required = true) String stateCookie,
            HttpServletRequest request, HttpServletResponse response) {
        try {
            // validate the inputs
            validator.getValidInput(
                    FederationHubUIService.class.getName(), code,
                    MartiValidator.Regex.MartiSafeString.name(),
                    2047, false);
            validator.getValidInput(
            		FederationHubUIService.class.getName(), state,
                    MartiValidator.Regex.MartiSafeString.name(),
                    2047, false);
            validator.getValidInput(
            		FederationHubUIService.class.getName(), stateCookie,
                    MartiValidator.Regex.MartiSafeString.name(),
                    2047, false);

            // validate the request state
            if (!sha256(stateCookie).equals(state)) {
                throw new IllegalStateException("state did not match request!");
            }

            // clean up the state cookie
            response.addHeader(HttpHeaders.SET_COOKIE, AuthCookieUtils.createCookie(
                    "state", stateCookie, 0, false).toString());

            // build up the parameters for the token request
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<String, String>();
            requestBody.add("grant_type", "authorization_code");
            requestBody.add("code", code);
            requestBody.add("client_id", fedHubConfig.getKeycloakClientId());
            requestBody.add("client_secret", fedHubConfig.getKeycloakSecret());
            requestBody.add("redirect_uri", fedHubConfig.getKeycloakrRedirectUri());

            jwtUtil.processAuthServerRequest(requestBody, request, response);

            return new ModelAndView(new InternalResourceView("/login/redirect.html"));

        } catch (Exception e) {
            logger.error("exception in handleRedirect", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    // TODO use this if we ever support plain username + password auth from a db or file
    @PostMapping("/oauth/token")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            User user = (User) authentication.getPrincipal();
            String accessToken = jwtUtil.generateAccessToken(request.getUsername());
            AuthResponse authResponse = new AuthResponse(user.getUsername(), accessToken);

            ResponseCookie.ResponseCookieBuilder responseCookieBuilder = ResponseCookie
                    .from("hubState", authResponse.getAccessToken())
                    .secure(true)
                    .httpOnly(true)
                    .path("/")
                    .maxAge(86400);

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.set(HttpHeaders.SET_COOKIE, responseCookieBuilder.build().toString());

            return ResponseEntity.ok().headers(responseHeaders).body(authResponse);

        } catch (BadCredentialsException ex) {
        	logger.error("Bad credentials", ex);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        catch (Exception e) {
        	logger.error("Error with login",e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
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
    public ResponseEntity<List<RemoteGroup>> getKnownCaGroups(HttpServletRequest request) {
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

    @RequestMapping(value = "/fig/getBrokerMetrics", method = RequestMethod.GET)
    public ResponseEntity<FederationHubBrokerMetrics> getFederationHubBrokerMetrics() {
        return new ResponseEntity<FederationHubBrokerMetrics>(fedHubBroker.getFederationHubBrokerMetrics(), new HttpHeaders(), HttpStatus.OK);
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
    
    @RequestMapping(value = "/fig/disconnectFederate/{connectionId}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> disconnectFederate(@PathVariable("connectionId") String connectionId) {
    	try {
        	fedHubBroker.disconnectFederate(connectionId);
    		return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);
    	} catch (Exception e) {
    		logger.error("error with deleteGroupCa", e);
    		return new ResponseEntity<>(new HttpHeaders(), HttpStatus.BAD_REQUEST);
		}
    }
    
    @RequestMapping(value="/fig/getSelfCaFile", method=RequestMethod.GET)
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

    private String sha256(String input) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] bytes = input.getBytes("US-ASCII");
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return Base64.getUrlEncoder().withoutPadding().encodeToString(md.digest(bytes));
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

    private List<RemoteGroup> federateGroupsToGroupHolders(Collection<FederateGroup> groups) {
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
            }

            groupList.add(remoteGroup);
        }
        return groupList;
    }

    private class RemoteGroup {
    	private String uid;
    	private String issuer;
    	private String subject;

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


    }
}