package tak.server.federation.hub.ui.graph;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterUtils {
    private static final String FILTER_TYPE = "filter";
    private static final String AND_TYPE = "and";
    private static final String OR_TYPE = "or";

    // Patterns
    private static final String FILTER_REGREX = "\\(|\\)|\\w+\\(?";
    private static final String METHOD_REGEX = "\\w+\\(";
    private static final String ARGUMENT_REGEX = "\\w+";
    private static final String OR_REGEX = "\\(";

    public static String filterNodeToString(FilterNode filterNode) {
        if (filterNode.getType().equals(FILTER_TYPE)) {
            return filterToString(filterNode.getFilter());
        } else if(filterNode.getType().equals(AND_TYPE)) {
            return andedFiltersToString(filterNode.getNodes());
        } else if(filterNode.getType().equals(OR_TYPE)) {
            return oredFiltersToString(filterNode.getNodes());
        }
        return "";
    }

    public static String filterToString(EdgeFilter edgeFilter) {
        StringBuilder filterStringBuilder = new StringBuilder();
        filterStringBuilder.append(edgeFilter.name).append("(");
        for(int i = 0; i < edgeFilter.getArgs().size(); i++) {
            filterStringBuilder.append(valueToString(edgeFilter.getArgs().get(i)));
            if(isNotLastIndex(edgeFilter.getArgs(), i)) {
                filterStringBuilder.append(", ");
            }
        }
        filterStringBuilder.append(")");
        return filterStringBuilder.toString();
    }

    public static String andedFiltersToString(List<FilterNode> filterNodes) {
        if (filterNodes.size() == 1) {
            return filterNodeToString(filterNodes.get(0));
        }

        List<FilterNode> nodeList = getValidNodeList(filterNodes);
        FilterNode firstNode = nodeList.remove(0);
        StringBuilder filterStringBuilder = new StringBuilder();
        if (firstNode.getType().equals(FILTER_TYPE)) {
            filterStringBuilder.append(firstNode.getFilter().getName()).append("(");
            filterStringBuilder.append(andedFiltersToString(nodeList));

            // Skip the message Arg
            for (int i = 1; i < firstNode.getFilter().getArgs().size(); i++) {
                filterStringBuilder.append(", ").append(valueToString(firstNode.getFilter().getArgs().get(i)));
            }

        } else if (firstNode.getType().equals(OR_TYPE)) {
            filterStringBuilder.append("(");
            for (FilterNode node : nodeList) {
                filterStringBuilder.append(filterNodeToString(node)).append(", ");
            }
            filterStringBuilder.append(andedFiltersToString(nodeList));
        }
        return filterStringBuilder.append(")").toString();
    }

    public static String oredFiltersToString(List<FilterNode> filterNodes) {
        if (filterNodes.size() == 1) {
            return filterNodeToString(filterNodes.get(0));
        }

        StringBuilder filterStringBuilder = new StringBuilder();
        filterStringBuilder.append("(");
        for (int i = 0; i < filterNodes.size(); i++) {
            filterStringBuilder.append(filterNodeToString(filterNodes.get(i)));
            if (isNotLastIndex(filterNodes, i)) {
                filterStringBuilder.append(", ");
            }
        }
        filterStringBuilder.append(")");
        return filterStringBuilder.toString();
    }


    /**
     * This method is required to deal with the case of AND'ed filter nodes where the first subnode (or all immediate subnodes)
     * are also ANDs or ORs.  The example AND(AND(F1, F2), F3) is not recursively buildable, but reordering it to
     * AND(F1, F2, F3) is buildable and logically equivalent.
     *
     * This should not occur often, but this attempts to assist a user who is building filter chains, and not have the UI
     * throw an error that causes the user to have to logically reduce his filter chains.
     */
    private static List<FilterNode> getValidNodeList(List<FilterNode> filterNodes) {
        List<FilterNode> nodeList = new LinkedList<>();
        List<FilterNode> andList = new LinkedList<>();
        List<FilterNode> orList = new LinkedList<>();
        filterNodes.stream().filter(node -> node.getType().equals(FILTER_TYPE)).forEach(nodeList::add);
        filterNodes.stream().filter(node -> node.getType().equals(AND_TYPE)).forEach(andList::add);
        filterNodes.stream().filter(node -> node.getType().equals(OR_TYPE)).forEach(orList::add);

        if(!nodeList.isEmpty()) {
            nodeList.addAll(andList);
            nodeList.addAll(orList);
            return nodeList;
        } else if(!andList.isEmpty()) {
            andList.stream().forEach(node -> nodeList.addAll(node.getNodes()));
            nodeList.addAll(orList);
            return getValidNodeList(nodeList);
        } else if(!orList.isEmpty()) {
            return orList;
        }

        return nodeList;
    }

    private static String valueToString(FilterArgument argument) {
        if(argument.getType().equals("string")) {
            return "\"" + argument.getValue() + "\"";
        }
        return argument.getValue();
    }

    private static boolean isNotLastIndex(List list, int index) {
        return (index < list.size() - 1);
    }

    public static List<FilterNode> filterExpressionsToFilterNodes(String filterExpression) {
        List<FilterNode> nodeList = new ArrayList<>();
        List<String> expressionTokens = tokenizeFilterExpression(filterExpression);
        int index = 0;
        nodeList.add(filterExpressionToFilterNode(expressionTokens, index));
        return nodeList;
    }

    private static FilterNode filterExpressionToFilterNode(List<String> expressionTokens, int index) {
        if (isMethod(expressionTokens.get(index))) {
            if (isMethod(expressionTokens.get(index + 1))){
                return processAndNode(expressionTokens, index);
            } else {
                return processMethodNode(expressionTokens, index);
            }
        } else if (isArgument(expressionTokens.get(index))) {
            // THROW EXCEPTION
        } else if (isOr(expressionTokens.get(index))) {
            return processOrNode(expressionTokens, index);
        } else if (isEnd(expressionTokens.get(index))) {

        } else {
            // THROW EXCEPTION
        }
        return null;
    }

    private static FilterNode processAndNode(List<String> expressionTokens, int index) {
        return null;
    }

    private static FilterNode processOrNode(List<String> expressionTokens, int index) {
        return null;
    }

    private static FilterNode processMethodNode(List<String> expressionTokens, int index) {
        String methodName = expressionTokens.get(index).replace("(", "");
        EdgeFilter filter = new EdgeFilter();
        if (getKnownFiltersFromPolicy().containsKey(methodName)) {
            EdgeFilter edgeFilter = getKnownFiltersFromPolicy().get(methodName);
            filter.setName(edgeFilter.getName());
            filter.setDescription(edgeFilter.getDescription());
            filter.setArgs(edgeFilter.getArgs());

            FilterNode methodNode = new FilterNode();
            methodNode.setType(FILTER_TYPE);

            index++;
            for (FilterArgument arg : filter.getArgs()) {
                FilterArgument policyArg;
                // TODO: Finish
            }


            methodNode.setFilter(filter);
            return methodNode;
        }
        // THROW EXCEPTION
        return null;
    }


    static List<String> tokenizeFilterExpression(String filterExpression) {
        Matcher filterMatcher = Pattern.compile(FILTER_REGREX).matcher(filterExpression);
        List<String> tokenList = new LinkedList<>();
        while (filterMatcher.find()) {
            tokenList.add(filterMatcher.group());
        }
        return tokenList;
    }

    private static boolean isOr(String token) {
        return token.equals("(");
    }

    private static boolean isEnd(String token) {
        return token.equals(")");
    }

    private static boolean isMethod(String token) {
        Pattern methodPattern = Pattern.compile(METHOD_REGEX);
        return methodPattern.matcher(token).matches();
    }

    private static boolean isArgument(String token) {
        Pattern methodPattern = Pattern.compile(ARGUMENT_REGEX);
        return methodPattern.matcher(token).matches();
    }

    public static Map<String, EdgeFilter> getKnownFiltersFromPolicy() {
        Map<String, EdgeFilter> filterMap = new HashMap<>();

        EdgeFilter allowEventType = new EdgeFilter();
        allowEventType.setDescription("allow the specified GeoEvent type");
        allowEventType.setName("allowEventType");
        List<FilterArgument> argList = new LinkedList<>();
        argList.add(new FilterArgument() {
            String name = "m";
            String type = "roger.message.Message";
            String value = "m";
        });
        argList.add(new FilterArgument() {
            String name = "eventType";
            String type = "string";
            String value = "";
        });
        allowEventType.setArgs(argList);
        filterMap.put(allowEventType.getName(), allowEventType);

        EdgeFilter allowAll = new EdgeFilter();
        allowAll.setDescription("allow all messages");
        allowAll.setName("allowAll");
        argList = new LinkedList<>();
        argList.add(new FilterArgument() {
            String name = "m";
            String type = "roger.message.Message";
            String value = "m";
        });
        allowAll.setArgs(argList);
        filterMap.put(allowAll.getName(), allowAll);

        EdgeFilter allowChatMessage = new EdgeFilter();
        allowChatMessage.setDescription("allow a message if it is a CoT chat message");
        allowChatMessage.setName("allowChatMessage");
        argList = new LinkedList<>();
        argList.add(new FilterArgument() {
            String name = "m";
            String type = "roger.message.Message";
            String value = "m";
        });
        allowChatMessage.setArgs(argList);
        filterMap.put(allowChatMessage.getName(), allowChatMessage);

        EdgeFilter mutateEventType = new EdgeFilter();
        mutateEventType.setDescription("Change the GeoEvent of the message to the given string");
        mutateEventType.setName("mutateEventType");
        argList = new LinkedList<>();
        argList.add(new FilterArgument() {
            String name = "m";
            String type = "roger.message.Message";
            String value = "m";
        });
        argList.add(new FilterArgument() {
            String name = "eventType";
            String type = "string";
            String value = "";
        });
        mutateEventType.setArgs(argList);
        filterMap.put(mutateEventType.getName(), mutateEventType);

        EdgeFilter chatMessagePrepend = new EdgeFilter();
        chatMessagePrepend.setDescription("Prepend the given string to the chat message");
        chatMessagePrepend.setName("chatMessagePrepend");
        argList = new LinkedList<>();
        argList.add(new FilterArgument() {
            String name = "m";
            String type = "roger.message.Message";
            String value = "m";
        });
        argList.add(new FilterArgument() {
            String name = "toPrepend";
            String type = "string";
            String value = "";
        });
        chatMessagePrepend.setArgs(argList);
        filterMap.put(chatMessagePrepend.getName(), chatMessagePrepend);

        EdgeFilter colorTrackByTeam = new EdgeFilter();
        colorTrackByTeam.setDescription("Colors track messages moving along this edge");
        colorTrackByTeam.setName("colorTrackByTeam");
        argList = new LinkedList<>();
        argList.add(new FilterArgument() {
            String name = "m";
            String type = "roger.message.Message";
            String value = "m";
        });
        argList.add(new FilterArgument() {
            String name = "color";
            String type = "string";
            String value = "";
        });
        colorTrackByTeam.setArgs(argList);
        filterMap.put(colorTrackByTeam.getName(), colorTrackByTeam);

        EdgeFilter updateLocation = new EdgeFilter();
        updateLocation.setDescription("Fuzzes the point by the specified fuzzDistance, in degrees");
        updateLocation.setName("updateLocation");
        argList = new LinkedList<>();
        argList.add(new FilterArgument() {
            String name = "m";
            String type = "roger.message.Message";
            String value = "m";
        });
        argList.add(new FilterArgument() {
            String name = "fuzzDistance";
            String type = "double";
            String value = "";
        });
        updateLocation.setArgs(argList);
        filterMap.put(updateLocation.getName(), updateLocation);

        return filterMap;
    }
}
