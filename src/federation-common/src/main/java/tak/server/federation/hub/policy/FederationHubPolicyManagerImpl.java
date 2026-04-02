package tak.server.federation.hub.policy;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.ignite.Ignite;
import org.apache.ignite.services.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import tak.server.federation.Federate;
import tak.server.federation.FederateGroup;
import tak.server.federation.FederateIdentity;
import tak.server.federation.FederationException;
import tak.server.federation.FederationPolicyGraph;
import tak.server.federation.hub.FederationHubCache;
import tak.server.federation.hub.ui.graph.FederationUIPolicyModel;
import tak.server.federation.hub.ui.graph.PolicyObjectCell;

public class FederationHubPolicyManagerImpl implements FederationHubPolicyManager, Service {

    private static final long serialVersionUID = 1012094988435086891L;
    
    private final Ignite ignite;

    private static String DEFAULT_POLICY_FILE = "/opt/tak/federation-hub/ui_generated_policy.json";
    private static String DEFAULT_POLICY_DIR = "/opt/tak/federation-hub/policies";
    
    private static String VERSION = "v2";

    private static final Logger logger = LoggerFactory.getLogger(FederationHubPolicyManagerImpl.class);

    private Set<FederateGroup> groupCas;
    private Set<Federate> dynamicallyAddedFederates;
    private Collection<PolicyObjectCell> graphNodes = new ArrayList<>();
    
    private FederationPolicyGraph federationPolicyGraph;
    
    public FederationHubPolicyManagerImpl(Ignite ignite, String DEFAULT_POLICY_FILE) {
    	this.ignite = ignite;
    	
    	if (!Strings.isNullOrEmpty(DEFAULT_POLICY_FILE)) 
    		FederationHubPolicyManagerImpl.DEFAULT_POLICY_FILE = DEFAULT_POLICY_FILE;
    }
    
    @Override
    public FederationPolicyGraph getActivePolicyGraph() {
		return this.federationPolicyGraph;
	}

	@Override
	public FederationPolicyGraph getPolicyGraph(String policyId) {
		return loadPolicyFile(Paths.get(DEFAULT_POLICY_DIR).resolve(policyId + ".json").toFile().toString())
				.getPolicyGraphFromModel();
	}
    
	public void cachePolicyGraph() {
		// cache events will alert other services of the change so they don't have to constantly ask for the
		// most up to date policy
		FederationHubCache.getFederationHubPolicyStoreCache(ignite).put(FederationHubCache.POLICY_GRAPH_CACHE_KEY, federationPolicyGraph);
	}

    @Override
    public FederationPolicyGraph addCaFederate(Federate federate, List<String> federateCaNames) {
    	 for (String caName : federateCaNames) {
             if (federationPolicyGraph.getGroup(caName) != null) {
                 federate.addGroupIdentity(new FederateIdentity(caName));
                 federationPolicyGraph.getGroup(caName).addFederateToGroup(federate);
             }
         }
    	 
        if (federationPolicyGraph.getNode(federate.getFederateIdentity().getFedId()) == null) {
             // Add iff federate had certs that matched CA groups in the policy graph
             if (!federate.getGroupIdentities().isEmpty()) {
            	 federationPolicyGraph.addFederate(federate);
                 dynamicallyAddedFederates.add(federate);
             }
        } else {
        	for (String caName : federateCaNames) {
                if (federationPolicyGraph.getGroup(caName) != null) {
                	federationPolicyGraph.getFederate(federate.getFederateIdentity().getFedId()).addGroupIdentity(new FederateIdentity(caName));
                }
            }
        }
        
        cachePolicyGraph();
        
        return federationPolicyGraph;
    }

    @Override
    public void addCaGroup(FederateGroup federateGroup) {
        groupCas.add(federateGroup);
        cachePolicyGraph();
    }
    
    @Override
    public void removeCaGroup(FederateGroup federateGroup) {
        groupCas.remove(federateGroup);
        cachePolicyGraph();
    }

    @Override
    public Collection<FederateGroup> getCaGroups() {
        return groupCas;
    }
    
  
    public synchronized FederationUIPolicyModel loadPolicyFile(String policyFilePath) {
        if (policyFilePath.contains(".yml")) {
            policyFilePath = policyFilePath.replace(".yml", ".json");
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(new File(policyFilePath), FederationUIPolicyModel.class);
        } catch (IOException e) {
            logger.warn("Could not read policy from policy dir: " +  policyFilePath + " - Trying default file." );

            policyFilePath = DEFAULT_POLICY_FILE;
            try {
                return mapper.readValue(new File(policyFilePath), FederationUIPolicyModel.class);
            } catch (IOException e2) {
                logger.error("Could not read policy file: " + policyFilePath, e2);
                return null;
            }
        }
    }
    
    @Override
	public synchronized FederationUIPolicyModel savePolicyFile(FederationUIPolicyModel policy) {
    	 String policyFilePath = DEFAULT_POLICY_FILE;
         if (policyFilePath.contains(".yml"))
             policyFilePath = policyFilePath.replace(".yml", ".json");
         ObjectMapper mapper = new ObjectMapper();
         try {
         	// make sure the policy we are saving is the active one before updating the active policy file
         	if (getActivePolicyGraph() != null && policy.getName().equals(getActivePolicyGraph().getName()))
         		mapper.writerWithDefaultPrettyPrinter().writeValue(new File(policyFilePath), policy);
             
         	mapper.writerWithDefaultPrettyPrinter().writeValue(Paths.get(DEFAULT_POLICY_DIR).resolve(policy.getName() + ".json").toFile(), policy);
         } catch (IOException e) {
             logger.error("Could not write policy to file", e);
         }
         return policy;
	}

	@Override
	public synchronized FederationUIPolicyModel saveGraphPolicyFile(FederationUIPolicyModel update) {
		FederationUIPolicyModel policy = getPolicyGraph(update.getName()).getModel();
		policy.setGraphData(update.getGraphData());
		policy.setSettings(update.getSettings());
        policy.setDescription(update.getDescription());
        policy.setName(update.getName());
        policy.setType(update.getType());
        policy.setThumbnail(update.getThumbnail());
        policy.setVersion(update.getVersion());
		savePolicyFile(policy);
		return policy;
	}

	@Override
	public synchronized FederationUIPolicyModel saveSettingsPolicyFile(FederationUIPolicyModel update) {
		FederationUIPolicyModel policy = getPolicyGraph(update.getName()).getModel();
		policy.setSettings(update.getSettings());
		savePolicyFile(policy);
		return policy;
	}

	@Override
	public synchronized FederationUIPolicyModel savePluginsPolicyFile(FederationUIPolicyModel update) {
		FederationUIPolicyModel policy = getPolicyGraph(update.getName()).getModel();
		policy.setPluginsData(update.getPluginsData());
		
		// if we update the plugins policy for the active policy, we need to recache the object
    	if (getActivePolicyGraph() != null && policy.getName().equals(getActivePolicyGraph().getName())) {    		
    		federationPolicyGraph.getModel().setPluginsData(update.getPluginsData());
    		cachePolicyGraph();
    	}
		
		savePolicyFile(policy);
		
		return policy;
	}

    @Override
    public void setPolicyGraph(FederationPolicyGraph newPolicyGraph) throws FederationException {
        this.federationPolicyGraph = newPolicyGraph;
        
        cachePolicyGraph();

        if (newPolicyGraph != null)
        	graphNodes = newPolicyGraph.getModel().getGraphData().getNodes();
    }


    @Override
    public void cancel() {
        if (logger.isDebugEnabled()) {
            logger.debug("cancel() in " + getClass().getName());
        }
    }

    private FederationPolicyGraph getEmptyPolicy() {
    	FederationUIPolicyModel model = new FederationUIPolicyModel();
    	
        return new FederationPolicyGraphImpl(model);
    }
    
    @Override
    public Map<String, FederationUIPolicyModel> getAllPolicies() {
        Map<String, FederationUIPolicyModel> policies = new HashMap<>();
        try {
            Path dirPath = Paths.get(DEFAULT_POLICY_DIR);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*.json")) {
                for (Path entry : stream) {
                	String filename = entry.getFileName().toString();
                	
                	if (Strings.isNullOrEmpty(filename)) break;
                	
                	FederationUIPolicyModel policy = loadPolicyFile(dirPath.resolve(filename).toString());
                	
                	if (policy == null) break;
                	
                	policies.put(policy.getName(), policy);
                }
            }
        } catch (Exception e) {
			logger.error("Error trying to read from policy directory", e);
		}

        return policies;
    }
    
	@Override
    public Collection<PolicyObjectCell> getPolicyCells() {
    	return graphNodes;
    }

    @Override
    public void init() throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("init() in " + getClass().getName());
        }

        Path defaultPolicyDir = Paths.get(DEFAULT_POLICY_DIR);
        // Create directory if it doesn't exist
        try {
            if (!Files.exists(defaultPolicyDir)) {
                Files.createDirectories(defaultPolicyDir);
            }
        } catch (Exception e) { logger.error("Could not create missing policy dir", e); }

        /* TODO allow policy file to be specified via configuration. */
        String policyFilename = DEFAULT_POLICY_FILE;
        File policyFile = new File(policyFilename);
        if (!policyFile.exists()) {
        	this.federationPolicyGraph = getEmptyPolicy();
        } else {
        	FederationUIPolicyModel policy = loadPolicyFile(policyFilename);
        	if (policy == null) {
        		this.federationPolicyGraph = getEmptyPolicy();
        	} else {
        		try {
        			if (Strings.isNullOrEmpty(policy.getVersion())) {
        		        ObjectMapper mapper = new ObjectMapper();
    		            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(DEFAULT_POLICY_FILE + ".bak"), policy);
        			}
                } catch (Exception e) {
                	logger.error("err backing up policy file", e);
                }
                try {
            		graphNodes = policy.getGraphData().getNodes();
                } catch (Exception e) {
                	logger.error("err",e);
                }
                this.federationPolicyGraph = policy.getPolicyGraphFromModel();
        	}
        }
        
        cachePolicyGraph();
        
        groupCas = new HashSet<>();
        dynamicallyAddedFederates = new HashSet<>();
    }

    @Override
    public void execute() throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("execute() in " + getClass().getName());
        }
    }
}
