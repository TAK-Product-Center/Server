package com.bbn.marti.takcl.AppModules;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.bbn.marti.config.Input;
import com.bbn.marti.remote.InputMetric;
import com.bbn.marti.remote.groups.ConnectionModifyResult;
import com.bbn.marti.remote.groups.NetworkInputAddResult;
import com.bbn.marti.remote.service.InputManager;
import com.bbn.marti.takcl.TakclIgniteHelper;
import com.bbn.marti.takcl.AppModules.generic.ServerAppModuleInterface;
import com.bbn.marti.takcl.cli.simple.Command;
import com.bbn.marti.takcl.config.common.TakclRunMode;
import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.connections.BaseConnections;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.data.templates.ImmutableConnectionsTemplate_471E9257;

/**
 * Used to modify the server's inputs through an online remote interface. Will not work if there is no server to connect to.
 */
public class OnlineInputModule implements ServerAppModuleInterface {

    InputManager inputManager = null;

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
        if (inputManager == null) {
            inputManager = TakclIgniteHelper.getInputManager(server);
        }
    }

    @Override
    public void halt() {
        if (server != null) {
            TakclIgniteHelper.closeAssociatedIgniteInstance(server);
        }
    }

    /**
     * Adds a network interface to the server with the specified parameters
     *
     * @param protocol The protocol to add
     * @param port     The port to add
     * @param name     The getConsistentUniqueReadableIdentifier of the input
     * @return The result of the add attempt
     */
    @Command(description = "Adds a network input with the specified getConsistentUniqueReadableIdentifier, protocol, and port to the server.")
    public NetworkInputAddResult add(@NotNull ProtocolProfiles protocol, @NotNull String port, @NotNull String name) {
        Input input = new Input();
        input.setName(name);
        input.setProtocol(protocol.getValue());
        input.setPort(Integer.parseInt(port));
        Integer protocolVersion = protocol.getCoreNetworkVersion();
        if (protocolVersion != null) {
            input.setCoreVersion(protocolVersion);
        }

        return inputManager.createInput(input, true);
    }

    public NetworkInputAddResult add(Input input) {
        return inputManager.createInput(input, true);
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
        inputManager.createInput(networkInput.getConfigInput(), true);
    }

    public void addDataFeed(AbstractConnection networkInput) {
        inputManager.createDataFeed(networkInput.getConfigDataFeed(), true);
    }

    /**
     * Removes a predefined input from the server
     *
     * @param input The input to remove
     */
    @Command
    public void remove(BaseConnections input) {
        inputManager.deleteInput(input.name(), true);
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
        inputManager.deleteInput(name, true);
    }

    /**
     * Removes the dataFeed with the specified getConsistentUniqueReadableIdentifier
     *
     * @param name The getConsistentUniqueReadableIdentifier of the data feed to remove
     */
    @Command(description = "Removes the data feed with the specified getConsistentUniqueReadableIdentifier.")
    public void removeDataFeed(String name) {
        inputManager.deleteDataFeed(name, true);
    }

    public Collection<InputMetric> getInputMetricList() {
        return inputManager.getInputMetrics(false);
    }

    /**
     * Gets the list of currently enabled inputs
     *
     * @return The list of inputs
     */
    @Command(description = "Gets a list of the currently enabled inputs.")
    public String getList() {
        Collection<InputMetric> inputMetrics = getInputMetricList();
        String printString = "";

        for (InputMetric inputMetric : inputMetrics) {
            Input input = inputMetric.getInput();
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
    public ConnectionModifyResult addInputToGroup(@NotNull String inputIdentifier, @NotNull String groupName) {
        Collection<InputMetric> inputMetrics = inputManager.getInputMetrics(false);
        Input modInput = null;

        for (InputMetric im : inputMetrics) {
            if (im.getInput().getName().equals(inputIdentifier)) {
                modInput = im.getInput();
            }
        }

        if (modInput == null) {
            return ConnectionModifyResult.FAIL_NONEXISTENT;
        }

        modInput.getFiltergroup().add(groupName);
        return inputManager.modifyInput(inputIdentifier, modInput, true);
    }

    public ConnectionModifyResult removeInputFromGroup(@NotNull String inputIdentifier, @NotNull String groupName) {
        Collection<InputMetric> inputMetrics = inputManager.getInputMetrics(false);
        Input modInput = null;

        for (InputMetric im : inputMetrics) {
            if (im.getInput().getName().equals(inputIdentifier)) {
                modInput = im.getInput();
            }
        }

        if (modInput == null) {
            return ConnectionModifyResult.FAIL_NONEXISTENT;
        }

        if (modInput.getFiltergroup().contains(groupName)) {
            modInput.getFiltergroup().remove(groupName);
        }


        return inputManager.modifyInput(inputIdentifier, modInput, true);
    }
}
