package tak.server.federation.hub.policy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import tak.server.federation.FederateGroup;
import tak.server.federation.FederateIdentity;
import tak.server.federation.FederationException;
import tak.server.federation.FederationNode;
import tak.server.federation.FederationPolicyGraph;
import tak.server.federation.hub.ui.graph.FederationPolicyModel;
import tak.server.federation.hub.ui.graph.PolicyObjectCell;

public class FederationHubPolicyManagerImpl implements FederationHubPolicyManager, Service {

    private static final long serialVersionUID = 1012094988435086891L;

    private static final String DEFAULT_POLICY_PATH = "/opt/tak/federation-hub/";
    private static final String DEFAULT_POLICY_FILENAME = "ui_generated_policy.json";

    /* JSON file constants. */
    private static final String POLICY_NAME = "name";
    /* A list of fully-qualified names of objects containing federate lambda filter implementations. */
    private static final String FILTER_OBJECTS = "filter_objects";
    private static final String FEDERATE_NODES = "federate_nodes";
    private static final String FEDERATE_EDGES = "federate_edges";
    private static final String GROUPS = "groups";
    private static final String NAME = "name";
    private static final String INTERCONNECTED = "interconnected";
    private static final String NODE_UID = "uid";
    private static final String NODE_ATTRIBUTES = "attributes";
    private static final String EDGE_SOURCE = "source";
    private static final String EDGE_DESTINATION = "destination";
    private static final String FILTER_EXPRESSION = "filterExpression";
    private static final String ADDITIONAL_DATA = "additionalData";

    private static final Logger logger = LoggerFactory.getLogger(FederationHubPolicyManagerImpl.class);

    private Set<FederateGroup> groupCas;
    private Set<Federate> dynamicallyAddedFederates;
    private Collection<PolicyObjectCell> cells = new ArrayList<>();

    @Override
    public FederationPolicyGraph getPolicyGraph() {
        return FederationHubPolicyStore.getInstance().getPolicyGraph();
    }

    @Override
    public void addCaFederate(Federate federate, List<String> federateCaNames) {
    	 for (String caName : federateCaNames) {
             if (getPolicyGraph().getGroup(caName) != null) {
                 federate.addGroupIdentity(new FederateIdentity(caName));
                 getPolicyGraph().getGroup(caName).addFederateToGroup(federate);
             }
         }
    	 
        if (getPolicyGraph().getNode(federate.getFederateIdentity().getFedId()) == null) {
             // Add iff federate had certs that matched CA groups in the policy graph
             if (!federate.getGroupIdentities().isEmpty()) {
            	 getPolicyGraph().addFederate(federate);
                 dynamicallyAddedFederates.add(federate);
             }
        } else {
        	for (String caName : federateCaNames) {
                if (getPolicyGraph().getGroup(caName) != null) {
                	getPolicyGraph().getFederate(federate.getFederateIdentity().getFedId()).addGroupIdentity(new FederateIdentity(caName));
                }
            }
        }
    }

    @Override
    public void addCaGroup(FederateGroup federateGroup) {
        groupCas.add(federateGroup);
    }

    @Override
    public Collection<FederateGroup> getCaGroups() {
        return groupCas;
    }

    private void updateFederate(Federate node) {
        Federate currentFederate = getPolicyGraph().getFederate(node.getFederateIdentity().getFedId());

        // Add objects to current federate first, then add combined list to new federate
        // This lets new objects supersede old ones.  We don't just update the current federate
        // because that can cause issues with removing outdated nodes later
        currentFederate.getGroupIdentities().addAll(node.getGroupIdentities());
        currentFederate.getAttributes().putAll(node.getAttributes());
        node.getGroupIdentities().addAll(currentFederate.getGroupIdentities());
        node.getAttributes().putAll(currentFederate.getAttributes());
        getPolicyGraph().addNode(node);
    }

    private void updateGroup(FederateGroup node) {
        FederateGroup currentGroup = getPolicyGraph().getGroup(node.getFederateIdentity());

        // Add objects to current group first, then add combined list to new group
        // This lets new objects supersede old ones.  We don't just update the current federate
        // because that can cause issues with removing outdated nodes later
        currentGroup.getFederatesInGroup().addAll(node.getFederatesInGroup());
        currentGroup.getAttributes().putAll(node.getAttributes());
        node.getFederatesInGroup().addAll(currentGroup.getFederatesInGroup());
        node.getAttributes().putAll(currentGroup.getAttributes());
        getPolicyGraph().addNode(node);
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
            if (getPolicyGraph().getNode(node.getFederateIdentity().getFedId()) != null) {
                updateNode(node);
            } else {
            	getPolicyGraph().addNode(node);
            }
        }
    }

    private void updateEdges(FederationPolicyGraph federationPolicyGraph) throws FederationException {
    	getPolicyGraph().getEdgeSet().clear();
    	getPolicyGraph().getEdgeSet().addAll(federationPolicyGraph.getEdgeSet());
    }

    private void updateFile(Object updateFile) {
        /* TODO allow policy file to be specified via configuration. */
        String policyFilePath = DEFAULT_POLICY_PATH + DEFAULT_POLICY_FILENAME;
        if (policyFilePath.contains(".yml"))
            policyFilePath = policyFilePath.replace(".yml", ".json");
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(policyFilePath), updateFile);
        } catch (IOException e) {
            logger.error("Could not write policy to file", e);
        }
    }

    @Override
    public void updatePolicyGraph(FederationPolicyModel federationPolicyModel,
            Object updateFile) throws FederationException {
    	getPolicyGraph().getNodes().clear();
    	getPolicyGraph().getEdgeSet().clear();
    	
        FederationPolicyGraph federationPolicyGraph = federationPolicyModel.getPolicyGraphFromModel();
        updateNodes(federationPolicyGraph.getNodes());
        updateEdges(federationPolicyGraph);
        if (federationPolicyModel.getCells() != null)
        	cells = federationPolicyModel.getCells();

        if (updateFile != null) {
            updateFile(updateFile);
        }
    }

    @Override
    public void setPolicyGraph(FederationPolicyModel newPolicyModel,
            Object updateFile) throws FederationException {
        FederationPolicyGraph newPolicyGraph = newPolicyModel.getPolicyGraphFromModel();
        FederationHubPolicyStore.getInstance().setPolicyGraph(newPolicyGraph);

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
        return new FederationPolicyGraphImpl("Default Policy", new HashSet<>());
    }

    @SuppressWarnings("unchecked")
    private FederationPolicyGraph createPolicyGraph(Map<String, Object> policyMap) {
        String graphName = (String)policyMap.get(POLICY_NAME);
        if (Strings.isNullOrEmpty(graphName)) {
            throw new RuntimeException("There was no policy name in the given policy file");
        }

        Set<String> filterObjectSet = null;

        if (policyMap.get(FILTER_OBJECTS) != null)
            filterObjectSet = Sets.newHashSet((List<String>)policyMap.get(FILTER_OBJECTS));

        if (filterObjectSet == null)
            filterObjectSet = new HashSet<>();

        return new FederationPolicyGraphImpl(graphName, filterObjectSet);
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
                    throw new RuntimeException("A federate node was repeated in the policy file");
                }

                Federate federate = new Federate(new FederateIdentity(nodeUID));

                if (nodeMap.containsKey(NODE_ATTRIBUTES)) {
                    Map<String, Object> nodeAttributes =
                        (Map<String, Object>)nodeMap.get(NODE_ATTRIBUTES);
                    federate.getAttributes().putAll(nodeAttributes);
                }

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
                    throw new RuntimeException("A federate node was repeated in the policy file");
                }

                FederateGroup newGroup = new FederateGroup(new FederateIdentity(groupId));

                if (groupMap.containsKey(NODE_ATTRIBUTES)) {
                    Map<String, Object> nodeAttributes = (Map<String, Object>)groupMap.get(NODE_ATTRIBUTES);
                    newGroup.getAttributes().putAll(nodeAttributes);
                }

                if (groupMap.containsKey(FILTER_EXPRESSION)) {
                    String filterExpression = (String)groupMap.get(FILTER_EXPRESSION);
                    newGroup.setFilterExpression(filterExpression);
                } else if (groupMap.containsKey(INTERCONNECTED)) {
                    boolean interconnected = (Boolean)groupMap.get(INTERCONNECTED);
                    newGroup.setInterconnected(interconnected);
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

                    String edgeFilterExpression = "";

                    if (edgeMap.containsKey(FILTER_EXPRESSION)) {
                        edgeFilterExpression = (String)edgeMap.get(FILTER_EXPRESSION);
                    }

                    FederateEdge federateEdge = new FederateEdge(sourceNode.getFederateIdentity(),
                        destinationNode.getFederateIdentity(), edgeFilterExpression);

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

    private boolean initializePolicyGraphFromFile(String policyFile) {
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
                map = mapper.readValue(getClass().getResourceAsStream(policyFile),
                    new TypeReference<HashMap<String, Object>>(){});
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
            
            try {
                Map<String, Object> additionalData = newPolicyGraph.getAdditionalData();
                FederationPolicyModel policyModel =  new ObjectMapper().convertValue(additionalData.get("uiData"), FederationPolicyModel.class);
        		cells = policyModel.getCells();
            } catch (Exception e) {
            	logger.error("err",e);
            }
            
            FederationHubPolicyStore.getInstance().setPolicyGraph(newPolicyGraph);
        } catch (IOException | RuntimeException e) {
            logger.error("Could not load policy graph from file: " + e);
            return false;
        }

        return true;
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
        String policyFilename = DEFAULT_POLICY_PATH + DEFAULT_POLICY_FILENAME;
        File policyFile = new File(policyFilename);
        if (!policyFile.exists() || !initializePolicyGraphFromFile(policyFilename))
        	FederationHubPolicyStore.getInstance().setPolicyGraph(getEmptyPolicy());

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
