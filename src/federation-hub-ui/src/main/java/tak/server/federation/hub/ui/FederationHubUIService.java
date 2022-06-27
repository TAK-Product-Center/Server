package tak.server.federation.hub.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.servlet.annotation.MultipartConfig;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import tak.server.federation.FederateGroup;
import tak.server.federation.FederationException;
import tak.server.federation.FederationPolicyGraph;

import tak.server.federation.hub.FederationHubConstants;
import tak.server.federation.hub.FederationHubUtils;
import tak.server.federation.hub.broker.FederationHubBroker;
import tak.server.federation.hub.broker.FederationHubBrokerProxyFactory;
import tak.server.federation.hub.policy.FederationHubPolicyManager;
import tak.server.federation.hub.policy.FederationHubPolicyManagerProxyFactory;
import tak.server.federation.hub.policy.FederationPolicy;
import tak.server.federation.hub.ui.graph.EdgeFilter;
import tak.server.federation.hub.ui.graph.FederationPolicyModel;
import tak.server.federation.hub.ui.graph.FilterUtils;
import tak.server.federation.hub.ui.manage.AuthorizationFileWatcher;

@SpringBootApplication
@Controller
@MultipartConfig
public class FederationHubUIService extends SpringBootServletInitializer {

    private static final String DEFAULT_CONFIG_FILE = "/opt/tak/federation-hub/configs/federation-hub-ui.yml";

    private static final int CERT_FILE_UPLOAD_SIZE = 1048576;

    private static final Logger logger = LoggerFactory.getLogger(FederationHubUIService.class);

    private static Ignite ignite = null;

    private static String configFile;

    private static String activePolicyName = null;
    private static Map<String, FederationPolicyModel> cachedPolicies;

    @Autowired
    private FederationHubUIConfig fedHubConfig;

    @Autowired
    private FederationHubPolicyManager fedHubPolicyManager;

    @Autowired
    private FederationHubBroker fedHubBroker;

    @Autowired
    private AuthorizationFileWatcher authFileWatcher;

    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Usage: java -jar federation-hub-ui.jar [CONFIG_FILE_PATH]");
            return;
        } else if (args.length == 1) {
            configFile = args[0];
        } else {
            configFile = DEFAULT_CONFIG_FILE;
        }

        SpringApplication application = new SpringApplication(FederationHubUIService.class);

        ignite = Ignition.getOrStart(FederationHubUtils.getIgniteConfiguration(
            FederationHubConstants.FEDERATION_HUB_UI_IGNITE_PROFILE,
            true));
        if (ignite == null) {
            System.exit(1);
        }

        setupInitialConfig(application);
        cachedPolicies = new HashMap<>();

        ApplicationContext context = application.run(args);
    }

    private static void setupInitialConfig(SpringApplication application) {
        List<String> profiles = new ArrayList<String>();
        application.setAdditionalProfiles(profiles.toArray(new String[0]));
        Properties properties = new Properties();
        properties.put("cloud.aws.region.auto", false);
        properties.put("cloud.aws.region.static", "us-east-1");
        properties.put("cloud.aws.stack.auto", false);
        application.setDefaultProperties(properties);
    }

    @Bean
    public Ignite getIgnite() {
        return ignite;
    }

    @Bean
    public FederationHubBrokerProxyFactory fedHubBrokerProxyFactory() {
        return new FederationHubBrokerProxyFactory();
    }

    public FederationHubBroker fedHubBroker() throws Exception {
        return fedHubBrokerProxyFactory().getObject();
    }

    @Bean
    public FederationHubPolicyManagerProxyFactory fedHubPolicyManagerProxyFactory() {
        return new FederationHubPolicyManagerProxyFactory();
    }

    public FederationHubPolicyManager fedHubPolicyManager() throws Exception {
        return fedHubPolicyManagerProxyFactory().getObject();
    }

    @Bean
    public ConfigurableServletWebServerFactory jettyServletFactory() {
        return new JettyServletWebServerFactory(fedHubConfig.getPort());
    }

    private FederationHubUIConfig loadConfig(String configFile)
            throws JsonParseException, JsonMappingException, FileNotFoundException, IOException {
        if (getClass().getResource(configFile) != null) {
            // It's a resource.
            return new ObjectMapper(new YAMLFactory()).readValue(getClass().getResourceAsStream(configFile),
                FederationHubUIConfig.class);
        }

        // It's a file.
        return new ObjectMapper(new YAMLFactory()).readValue(new FileInputStream(configFile),
            FederationHubUIConfig.class);
    }

    @Bean
    public FederationHubUIConfig getFedHubConfig()
            throws JsonParseException, JsonMappingException, IOException {
        return loadConfig(configFile);
    }

    @Bean
    public AuthorizationFileWatcher authFileWatcher() {
        AuthorizationFileWatcher authFileWatcher = new AuthorizationFileWatcher(fedHubConfig);
        try {
            authFileWatcher.start();
        } catch (IOException e) {
            logger.error("Could not start watch service on authorization file: " + e);
            return null;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(authFileWatcher::stop));
        return authFileWatcher;
    }

    @RequestMapping(value = "/home", method = RequestMethod.GET)
    public String getHome() {
        return "home";
    }

    @RequestMapping(value = "/fig/saveFederation", method = RequestMethod.POST)
    @ResponseBody
    public FederationPolicyModel saveFederation(RequestEntity<FederationPolicyModel> requestEntity) {
        FederationPolicyModel policy = requestEntity.getBody();
        this.cachedPolicies.put(policy.getName(), policy);
        return policy;
    }

    @RequestMapping(value = "/fig/federation/{federationId}", method = RequestMethod.GET)
    @ResponseBody
    public FederationPolicyModel getFederationById(@PathVariable String federationId) {
//        FederationPolicy response = this.cachedPolicies.get(federationId);
//        HttpStatus status = HttpStatus.OK;
//        if (response == null) {
//            status = HttpStatus.NOT_FOUND;
//        }
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
