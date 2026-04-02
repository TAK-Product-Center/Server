package com.bbn.marti.takcl.AppModules;

import com.bbn.marti.takcl.AppModules.generic.ServerAppModuleInterface;
import com.bbn.marti.takcl.cli.simple.Command;
import com.bbn.marti.takcl.config.common.TakclRunMode;
import com.bbn.marti.takcl.connectivity.TakserverClient;
import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.connections.BaseConnections;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.data.templates.ImmutableConnectionsTemplate_471E9257;
import org.jetbrains.annotations.NotNull;
import tak.server.api.client.SubmissionApiApi;
import tak.server.api.client.models.ApiResponseSortedSetInputMetric;
import tak.server.api.client.models.DataFeed;
import tak.server.api.client.models.Input;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Used to modify the server's inputs through an online remote interface. Will not work if there is no server to connect to.
 */
public class OnlineInputModule implements ServerAppModuleInterface {

    public static Input createApiInput(AbstractConnection connection) {
        if (connection.getConnectionType() != ProtocolProfiles.ConnectionType.INPUT) {
            throw new RuntimeException(("Cannot generate an input for for connection '" + connection.getConsistentUniqueReadableIdentifier() + "' because it is not an input type!"));
        }
        Input input = new Input();
        input.setName(connection.getConsistentUniqueReadableIdentifier());
        input.setProtocol(connection.getProtocol().getValue());
        input.setPort(connection.getPort());
        input.setAuth(Input.AuthEnum.valueOf(connection.getAuthType().name()));
        input.setGroup(connection.getMCastGroup());
        input.setAnongroup(connection.getRawAnonAccessFlag());
        Integer networkVersion = connection.getProtocol().getCoreNetworkVersion();
        if (networkVersion != null) {
            input.setCoreVersion(networkVersion);
        }
        if (connection.getGroupSet().groupSet != null) {
            if (input.getFiltergroup() == null) {
                input.setFiltergroup(new ArrayList<>(connection.getGroupSet().groupSet));
            } else {
                input.getFiltergroup().addAll(connection.getGroupSet().groupSet);
            }
        }
        return input;
    }

    public static DataFeed createApiDataFeed(AbstractConnection connection) {
        if (connection.getConnectionType() != ProtocolProfiles.ConnectionType.DATAFEED) {
            throw new RuntimeException(("Cannot generate a datafeed for connection '" + connection.getConsistentUniqueReadableIdentifier() + "' because it is not an datafeed type!"));
        }
        DataFeed dataFeed = new DataFeed();
        dataFeed.setName(connection.getConsistentUniqueReadableIdentifier());
        dataFeed.setProtocol(connection.getProtocol().getValue());
        dataFeed.setPort(connection.getPort());
        dataFeed.setAuth(DataFeed.AuthEnum.valueOf(connection.getAuthType().name()));
        dataFeed.setGroup(connection.getMCastGroup());
        dataFeed.setAnongroup(connection.getRawAnonAccessFlag());
        dataFeed.setType(connection.getType());
        Integer networkVersion = connection.getProtocol().getCoreNetworkVersion();
        if (networkVersion != null) {
            dataFeed.setCoreVersion(networkVersion);
        }
        if (connection.getGroupSet().groupSet != null) {
            if (dataFeed.getFiltergroup() == null) {
                dataFeed.setFiltergroup(new ArrayList<>(connection.getGroupSet().groupSet));
            } else {
                dataFeed.getFiltergroup().addAll(connection.getGroupSet().groupSet);
            }
        }
        return dataFeed;
    }

    private TakserverClient client;
    private SubmissionApiApi submissionApi;

    private AbstractServerProfile server;

    public OnlineInputModule() {
    }

    @NotNull
    @Override
    public TakclRunMode[] getRunModes() {
        return new TakclRunMode[]{TakclRunMode.REMOTE_SERVER_INTERACTION};
    }

    @Override
    public ServerState getRequiredServerState() {
        return ServerState.RUNNING;
    }

    @Override
    public String getCommandDescription() {
        return "Manipulate network inputs.";
    }

    @Override
    public synchronized void init(@NotNull AbstractServerProfile server) {
        this.server = server;
        this.client = TakserverClient.getInstance(server);
        this.submissionApi = this.client.getSubmissionApi();
    }

    @Override
    public void halt() {
    }

    /**
     * Adds a predefined input to the server. The following can be parsed from the predefined input names:
     * <p>
     * The format of the input names is "&lt;getConsistentUniqueReadableIdentifier&gt;&lt;groups&gt;&lt;anonEnabled&gt;"
     * <p>
     * The getConsistentUniqueReadableIdentifier is a general identifier for input and specifies the protocol in use.
     * The groups indicate the group identifiers it belongs to. Each number represents a group it belongs to with the format "group#".
     * The anonEnabled indicates if anon is specifically enabled or disabled. If it is not there, the default behavior is applied.
     * <p>
     * For example, streamtcp_67t would be a streaming tcp connection belonging to the groups "group6" and "group7" with anon enabled
     *
     * @param input The predefined input to add
     */
    @Command(description = "Adds a predefined input to the server. The getConsistentUniqueReadableIdentifier formatting is as follows: " +
        "\n\t<getConsistentUniqueReadableIdentifier><groups><anonEnabled>" +
        "\n\t\tgetConsistentUniqueReadableIdentifier: The getConsistentUniqueReadableIdentifier of the input. It also specifies the protocol in use." +
        "\n\t\tgroups: The group identifiers in use. each digit is a group with the format \"group#\"" +
        "\n\t\tanonEnabled: Indicates if anon group access is enabled.  If missing, the default behavior is applied.")
    public void add(BaseConnections input) {
        ImmutableConnectionsTemplate_471E9257 connection = new ImmutableConnectionsTemplate_471E9257(input.name(), ImmutableServerProfiles.SERVER_CLI, input, input.getPort());
        add(connection);
    }

    public void add(AbstractConnection networkInput) {
        Input input = createApiInput(networkInput);
        try {
            submissionApi.createInput(input).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addDataFeed(AbstractConnection networkInput) {
        DataFeed dataFeed = createApiDataFeed(networkInput);
        try {
            submissionApi.createDataFeed(dataFeed).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes a predefined input from the server
     *
     * @param input The input to remove
     */
    @Command
    public void remove(BaseConnections input) {
        try {
            submissionApi.deoleteInput(input.name()).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Prints out the predefined inputs that can be added using the {@link #add(BaseConnections)} command.
     */
    @Command(description = "Prints out the predefined inputs that can be added through this tool.")
    public void listPredefinedInputs() {
        for (BaseConnections input : BaseConnections.values()) {
            System.out.println(input.displayString());
        }
    }

    /**
     * Removes the input with the specified getConsistentUniqueReadableIdentifier
     *
     * @param name The getConsistentUniqueReadableIdentifier of the input to remove
     */
    @Command(description = "Removes the input with the specified getConsistentUniqueReadableIdentifier.")
    public void remove(String name) {
        try {
            submissionApi.deoleteInput(name).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes the dataFeed with the specified getConsistentUniqueReadableIdentifier
     *
     * @param name The getConsistentUniqueReadableIdentifier of the data feed to remove
     */
    @Command(description = "Removes the data feed with the specified getConsistentUniqueReadableIdentifier.")
    public void removeDataFeed(String name) {
        try {
            submissionApi.deleteDataFeed(name).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ApiResponseSortedSetInputMetric getInputMetricList() {
        try {
            return submissionApi.getInputMetrics(false).execute().body();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the list of currently enabled inputs
     *
     * @return The list of inputs
     */
    @Command(description = "Gets a list of the currently enabled inputs.")
    public String getList() {
        ApiResponseSortedSetInputMetric inputMetrics = getInputMetricList();
        String printString = "";

        for (tak.server.api.client.models.InputMetric inputMetric : inputMetrics.getData()) {
            tak.server.api.client.models.Input input = inputMetric.getInput();
            String groupString = null;

            for (String group : input.getFiltergroup()) {
                groupString = (groupString == null ? ("groups=" + group) : (groupString + "," + group));
            }

            printString += ("getConsistentUniqueReadableIdentifier=" + input.getName() + ", protocol=" + input.getProtocol() + ", port=" + input.getPort() + (groupString == null ? "" : groupString) + "received: " + inputMetric.getMessagesReceived() + "\n");
        }
        return printString;
    }

    //    /**
//     * Changes the archive flag for the provided input, if not already set to the requested value
//     * @param inputName The input identifier to modify the archive flag of
//     * @param archiveFlag The desired setting of the archive flag
//     */
//    @Command(description = "Changes the archive flag of the provided inputName.")
//    public void changeInputArchiveFlag(@NotNull String inputName, @NotNull Boolean archiveFlag) {
//        try {
//            submissionInterface.changeInputArchiveFlag(inputName, archiveFlag);
//        } catch (RemoteException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
    public void tryAddInputToGroup(@NotNull String inputIdentifier, @NotNull String groupName) {
        try {
            ApiResponseSortedSetInputMetric inputMetrics = getInputMetricList();
            tak.server.api.client.models.Input modInput = null;

            for (tak.server.api.client.models.InputMetric im : inputMetrics.getData()) {
                if (im.getInput().getName().equals(inputIdentifier)) {
                    modInput = im.getInput();
                }
            }

            // It's unclear if this is ideal, but since the prior version's response wasn't checked, this
            // provides consistent behavior
            if (modInput == null) {
                return;
            }

            modInput.getFiltergroup().add(groupName);
            submissionApi.modifyInput(inputIdentifier, modInput).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void tryRemoveInputFromGroup(@NotNull String inputIdentifier, @NotNull String groupName) {
        try {
            ApiResponseSortedSetInputMetric inputMetrics = getInputMetricList();
            tak.server.api.client.models.Input modInput = null;

            for (tak.server.api.client.models.InputMetric im : inputMetrics.getData()) {
                if (im.getInput().getName().equals(inputIdentifier)) {
                    modInput = im.getInput();
                }
            }

            // It's unclear if this is ideal, but since the prior version's response wasn't checked, this
            // provides consistent behavior
            if (modInput == null) {
                return;
            }

            if (modInput.getFiltergroup().contains(groupName)) {
                modInput.getFiltergroup().remove(groupName);
            }

            submissionApi.modifyInput(inputIdentifier, modInput).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
