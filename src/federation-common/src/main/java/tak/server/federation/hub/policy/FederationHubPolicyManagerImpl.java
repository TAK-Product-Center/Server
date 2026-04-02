package tak.server.federation.hub.policy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.Set;

import org.apache.ignite.Ignite;
import org.apache.ignite.services.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import tak.server.federation.Federate;
import tak.server.federation.FederateEdge;
import tak.server.federation.FederateEdge.GroupFilterType;
import tak.server.federation.FederateGroup;
import tak.server.federation.FederateIdentity;
import tak.server.federation.FederationException;
import tak.server.federation.FederationNode;
import tak.server.federation.FederationPolicyGraph;
import tak.server.federation.hub.FederationHubCache;
import tak.server.federation.hub.ui.graph.FederationPolicyModel;
import tak.server.federation.hub.ui.graph.PolicyObjectCell;

public class FederationHubPolicyManagerImpl implements FederationHubPolicyManager, Service {

    private static final long serialVersionUID = 1012094988435086891L;
    
    private final Ignite ignite;

    private static String DEFAULT_POLICY_FILE = "/opt/tak/federation-hub/ui_generated_policy.json";
    private static String DEFAULT_POLICY_DIR = "/opt/tak/federation-hub/policies";
    
    /* JSON file constants. */
    private static final String POLICY_NAME = "name";
    private static final String VERSION_NAME = "version";
    /* A list of fully-qualified names of objects containing federate lambda filter implementations. */
    private static final String FILTER_OBJECTS = "filter_objects";
    private static final String FEDERATE_NODES = "federate_nodes";
    private static final String FEDERATE_EDGES = "federate_edges";
    private static final String GROUPS = "groups";
    private static final String NAME = "name";
    private static final String INTERCONNECTED = "interconnected";
    private static final String ALLOW_TOKEN_AUTH = "allowTokenAuth";
    private static final String TOKEN_AUTH_DURATION = "tokenAuthDuration";
    private static final String NODE_UID = "uid";
    private static final String EDGE_SOURCE = "source";
    private static final String EDGE_DESTINATION = "destination";
    private static final String ALLOWED_GROUPS = "allowedGroups";
    private static final String DISALLOWED_GROUPS = "disallowedGroups";
    private static final String GROUPS_FILTER_TYPE = "groupsFilterType";
    private static final String ADDITIONAL_DATA = "additionalData";

    private static final Logger logger = LoggerFactory.getLogger(FederationHubPolicyManagerImpl.class);

    private Set<FederateGroup> groupCas;
    private Set<Federate> dynamicallyAddedFederates;
    private Collection<PolicyObjectCell> cells = new ArrayList<>();
    
    private FederationPolicyGraph federationPolicyGraph;
    
    public FederationHubPolicyManagerImpl(Ignite ignite, String DEFAULT_POLICY_FILE) {
    	this.ignite = ignite;
    	
    	if (!Strings.isNullOrEmpty(DEFAULT_POLICY_FILE)) 
    		FederationHubPolicyManagerImpl.DEFAULT_POLICY_FILE = DEFAULT_POLICY_FILE;
    }

    @Override
    public FederationPolicyGraph getPolicyGraph() {
		return federationPolicyGraph;
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

    private void updateFederate(Federate node) {
        Federate currentFederate = federationPolicyGraph.getFederate(node.getFederateIdentity().getFedId());

        // Add objects to current federate first, then add combined list to new federate
        // This lets new objects supersede old ones.  We don't just update the current federate
        // because that can cause issues with removing outdated nodes later
        currentFederate.getGroupIdentities().addAll(node.getGroupIdentities());
        node.getGroupIdentities().addAll(currentFederate.getGroupIdentities());
        federationPolicyGraph.addNode(node);
    }

    private void updateGroup(FederateGroup node) {
        FederateGroup currentGroup = federationPolicyGraph.getGroup(node.getFederateIdentity());

        // Add objects to current group first, then add combined list to new group
        // This lets new objects supersede old ones.  We don't just update the current federate
        // because that can cause issues with removing outdated nodes later
        currentGroup.getFederatesInGroup().addAll(node.getFederatesInGroup());
        node.getFederatesInGroup().addAll(currentGroup.getFederatesInGroup());
        federationPolicyGraph.addNode(node);
    }

    private void updateNode(FederationNode node) {
        if (node instanceof Federate) {
            updateFederate((Federate) node);
        } else if (node instanceof FederateGroup) {
            updateGroup((FederateGroup) node);
        }
    }

    private void updateNodes(Collection<FederationNode> federationNodes) {
        for (FederationNode node : federationNodes) {
            if (federationPolicyGraph.getNode(node.getFederateIdentity().getFedId()) != null) {
                updateNode(node);
            } else {
            	federationPolicyGraph.addNode(node);
            }
        }
    }

    private void updateEdges(FederationPolicyGraph federationPolicyGraph) throws FederationException {
    	federationPolicyGraph.getEdgeSet().clear();
    	federationPolicyGraph.getEdgeSet().addAll(federationPolicyGraph.getEdgeSet());
    }

    private void updateFile(FederationPolicy updateFile) {
        /* TODO allow policy file to be specified via configuration. */
        String policyFilePath = DEFAULT_POLICY_FILE;
        if (policyFilePath.contains(".yml"))
            policyFilePath = policyFilePath.replace(".yml", ".json");
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(policyFilePath), updateFile);
            mapper.writerWithDefaultPrettyPrinter().writeValue(Paths.get(DEFAULT_POLICY_DIR).resolve(updateFile.getName() + ".json").toFile(), updateFile);
        } catch (IOException e) {
            logger.error("Could not write policy to file", e);
        }
    }

    @Override
    public void updatePolicyGraph(FederationPolicyModel federationPolicyModel,
            FederationPolicy updateFile) throws FederationException {
    	federationPolicyGraph.getNodes().clear();
    	federationPolicyGraph.getEdgeSet().clear();
    	
        FederationPolicyGraph federationPolicyGraph = federationPolicyModel.getPolicyGraphFromModel();
        updateNodes(federationPolicyGraph.getNodes());
        updateEdges(federationPolicyGraph);
        if (federationPolicyModel.getCells() != null)
        	cells = federationPolicyModel.getCells();

        if (updateFile != null) {
            updateFile(updateFile);
        }
        cachePolicyGraph();
    }

    @Override
    public void setPolicyGraph(FederationPolicyModel newPolicyModel,
            FederationPolicy updateFile) throws FederationException {
        FederationPolicyGraph newPolicyGraph = newPolicyModel.getPolicyGraphFromModel();
        this.federationPolicyGraph = newPolicyGraph;
        cachePolicyGraph();

        if (newPolicyModel.getCells() != null)
        	cells = newPolicyModel.getCells();
        
        if (updateFile != null) {
            updateFile(updateFile);
        }
    }


    @Override
    public void cancel() {
        if (logger.isDebugEnabled()) {
            logger.debug("cancel() in " + getClass().getName());
        }
    }

    private FederationPolicyGraph getEmptyPolicy() {
        return new FederationPolicyGraphImpl("Default Policy", "v2");
    }

    @SuppressWarnings("unchecked")
    private FederationPolicyGraph createPolicyGraph(Map<String, Object> policyMap) {
        String graphName = (String)policyMap.get(POLICY_NAME);
        String graphVersion = (String)policyMap.get(VERSION_NAME);
        if (Strings.isNullOrEmpty(graphName)) {
            throw new RuntimeException("There was no policy name in the given policy file");
        }

        Set<String> filterObjectSet = null;

        if (policyMap.get(FILTER_OBJECTS) != null)
            filterObjectSet = Sets.newHashSet((List<String>)policyMap.get(FILTER_OBJECTS));

        if (filterObjectSet == null)
            filterObjectSet = new HashSet<>();

        return new FederationPolicyGraphImpl(graphName, graphVersion);
    }

    @SuppressWarnings("unchecked")
    private void setPolicyGraphNodes(Map<String, Object> policyMap,
            FederationPolicyGraph newPolicyGraph) {
        List nodeList = (List)policyMap.get(FEDERATE_NODES);
        if (nodeList != null && nodeList.size() > 0) {
            for (Object node : nodeList) {
                Map<String, Object> nodeMap = (Map<String, Object>)node;
                String nodeUID = (String)nodeMap.get(NODE_UID);

                if (newPolicyGraph.getFederate(new FederateIdentity(nodeUID)) != null) {
                    throw new RuntimeException("A federate node was repeated in the policy file for uid " + nodeUID);
                }

                Federate federate = new Federate(new FederateIdentity(nodeUID));

                if (nodeMap.containsKey(GROUPS)) {
                    for (String groupId : ((List<String>)nodeMap.get(GROUPS))) {
                        federate.addGroupIdentity(new FederateIdentity(groupId));
                    }
                }
                newPolicyGraph.addNode(federate);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void setPolicyGraphGroups(Map<String, Object> policyMap,
            FederationPolicyGraph newPolicyGraph) {
        if (policyMap.containsKey(GROUPS) && policyMap.get(GROUPS) instanceof List) {
            List groupList = (List)policyMap.get(GROUPS);
            for (Object group : groupList) {
                Map<String, Object> groupMap = (Map<String, Object>)group;
                String groupName = (String)groupMap.get(NAME);
                String groupId = (String)groupMap.get(NODE_UID);

                if (newPolicyGraph.getGroup(new FederateIdentity(groupId)) != null) {
                    throw new RuntimeException("A federate group was repeated in the policy file for uid " + groupId);
                }

                FederateGroup newGroup = new FederateGroup(new FederateIdentity(groupId));

                if (groupMap.containsKey(INTERCONNECTED)) {
                    boolean interconnected = (Boolean)groupMap.get(INTERCONNECTED);
                    newGroup.setInterconnected(interconnected);
                }
                if (groupMap.containsKey(ALLOW_TOKEN_AUTH)) {
                    boolean allowTokenAuth = (Boolean)groupMap.get(ALLOW_TOKEN_AUTH);
                    newGroup.setAllowTokenAuth(allowTokenAuth);
                }
                if (groupMap.containsKey(TOKEN_AUTH_DURATION)) {
                    long tokenAuthDuration = (Long)groupMap.get(TOKEN_AUTH_DURATION);
                    newGroup.setTokenAuthDuration(tokenAuthDuration);
                }
                newPolicyGraph.addGroup(newGroup);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void setPolicyGraphEdges(Map<String, Object> policyMap,
            FederationPolicyGraph newPolicyGraph) {
        List edgeList = (List)policyMap.get(FEDERATE_EDGES);
        if (edgeList != null && !edgeList.isEmpty()) {
            for (Object edge : edgeList) {
                Map<String, Object> edgeMap = (Map<String, Object>)edge;
                String source = (String)edgeMap.get(EDGE_SOURCE);
                String destination = (String)edgeMap.get(EDGE_DESTINATION);

                if (Strings.isNullOrEmpty(source) || Strings.isNullOrEmpty(destination)) {
                    throw new RuntimeException("Edge in the policy file is missing a source or destination node");
                }
                
                FederationNode sourceNode = newPolicyGraph.getNode(source);
                FederationNode destinationNode = newPolicyGraph.getNode(destination);
                if (sourceNode != null && destinationNode != null) {

                    Set<String> allowedGroups = new HashSet<>();
                    Set<String> disallowedGroups = new HashSet<>();
                    FederateEdge.GroupFilterType groupFilterType = GroupFilterType.ALL;
                    
                    if (edgeMap.containsKey(ALLOWED_GROUPS)) {
                        List<String> allowedGroupsList = (List<String>) edgeMap.get(ALLOWED_GROUPS);
                        allowedGroups.addAll(allowedGroupsList);
                    }
                    
                    if (edgeMap.containsKey(DISALLOWED_GROUPS)) {
                        List<String> disallowedGroupsList = (List<String>) edgeMap.get(DISALLOWED_GROUPS);;
                        disallowedGroups.addAll(disallowedGroupsList);
                    }
                    
                    if (edgeMap.containsKey(GROUPS_FILTER_TYPE)) {
                    	String groupFilterTypeString = (String) edgeMap.get(GROUPS_FILTER_TYPE);
                    	groupFilterType = FederateEdge.getGroupFilterType(groupFilterTypeString);
                    }

                    FederateEdge federateEdge = new FederateEdge(sourceNode.getFederateIdentity(),
                        destinationNode.getFederateIdentity(), allowedGroups, disallowedGroups, groupFilterType);

                    try {
                        newPolicyGraph.addEdge(federateEdge);
                    } catch (FederationException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    throw new RuntimeException("Given source and destination nodes did not exist in the policy graph");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void setAdditionalData(Map<String, Object> policyMap,
            FederationPolicyGraph newPolicyGraph) {
        if (policyMap.get(ADDITIONAL_DATA) instanceof Map) {
            newPolicyGraph.setAddtionalData((Map)policyMap.get(ADDITIONAL_DATA));
        }
    }
    
    @Override
    public Map<String, FederationPolicyGraph> getAllPolicies() throws IOException {
        Map<String, FederationPolicyGraph> policies = new HashMap<>();

        Path dirPath = Paths.get(DEFAULT_POLICY_DIR);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*.json")) {
            for (Path entry : stream) {
            	String filename = entry.getFileName().toString();
            	
            	if (Strings.isNullOrEmpty(filename)) break;
            	
            	FederationPolicyGraph policy = initializePolicyGraphFromFile(dirPath.resolve(filename).toString());
            	
            	if (policy == null) break;
            	
            	policies.put(policy.getName(), policy);
            }
        }

        return policies;
    }

    public FederationPolicyGraph initializePolicyGraphFromFile(String policyFile) {
        ObjectMapper mapper;
        if (policyFile.endsWith(".yml")) {
            mapper = new ObjectMapper(new YAMLFactory());
        } else {
            mapper = new ObjectMapper();
        }

        try {
            HashMap<String, Object> map = null;

            if (getClass().getResource(policyFile) != null) {
                // It's a resource.
            	try (InputStream is = getClass().getResourceAsStream(policyFile)) {
            		map = mapper.readValue(is, new TypeReference<HashMap<String, Object>>(){});
            	}
            } else {
                // It's a file.
                map = mapper.readValue(new File(policyFile),
                    new TypeReference<HashMap<String, Object>>(){});
            }
            FederationPolicyGraph newPolicyGraph;
            newPolicyGraph = createPolicyGraph(map);
            setPolicyGraphNodes(map, newPolicyGraph);
            setPolicyGraphGroups(map, newPolicyGraph);
            setPolicyGraphEdges(map, newPolicyGraph);
            setAdditionalData(map, newPolicyGraph);
            
            return newPolicyGraph;
        } catch (IOException | RuntimeException e) {
            logger.error("Could not load policy graph from file: " + e);
            return null;
        }
    }
    
	@Override
    public Collection<PolicyObjectCell> getPolicyCells() {
    	return cells;
    }

    @Override
    public void init() throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("init() in " + getClass().getName());
        }

        /* TODO allow policy file to be specified via configuration. */
        String policyFilename = DEFAULT_POLICY_FILE;
        File policyFile = new File(policyFilename);
        if (!policyFile.exists()) {
        	this.federationPolicyGraph = getEmptyPolicy();
        } else {
        	FederationPolicyGraph policy = initializePolicyGraphFromFile(policyFilename);
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
                    Map<String, Object> additionalData = policy.getAdditionalData();
                    FederationPolicyModel policyModel =  new ObjectMapper().convertValue(additionalData.get("uiData"), FederationPolicyModel.class);
            		cells = policyModel.getCells();
                } catch (Exception e) {
                	logger.error("err",e);
                }
                this.federationPolicyGraph = policy;
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
