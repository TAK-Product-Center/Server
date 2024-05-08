package com.bbn.marti.takcl.connectivity.server;

import com.bbn.marti.config.Input;
import com.bbn.marti.config.Repository;
import com.bbn.marti.takcl.AppModules.OfflineConfigModule;
import com.bbn.marti.takcl.AppModules.OfflineFileAuthModule;
import com.bbn.marti.takcl.AppModules.OnlineFileAuthModule;
import com.bbn.marti.takcl.AppModules.OnlineInputModule;
import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.takcl.TakclIgniteHelper;
import com.bbn.marti.takcl.TestConfiguration;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.tests.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 *
 */
public abstract class AbstractRunnableServer {

    public static abstract class AbstractServerProcess {
        private boolean enabled;
        public final ServerProcessDefinition definition;

        public AbstractServerProcess(ServerProcessDefinition definition) {
            this.definition = definition;
            this.enabled = ServerProcessConfiguration.DefaultConfigMessagingApi.isProcessEnabled(definition);
        }

        public final String getIdentifier() {
            return definition.identifier;
        }

        void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public final boolean isEnabled() {
            return enabled;
        }

        public abstract void start(boolean enableReomteDebug);

        public ServerProcessDefinition getDefinition() {
            return definition;
        }

        public abstract boolean isRunning();

        public abstract void stop();

        public abstract void kill();

        public abstract List<String> waitForMissingLogStatements(int maxWaitDuration);
    }

    public enum ServerState {
        CONFIGURING,
        DEPLOYING,
        RUNNING,
        STOPPING,
        STOPPED,
    }

    protected final Logger logger;
    protected static final List<String> LOGGING_ARGUMENTS;

    static {
        if (TAKCLCore.serverLogLevelOverrides == null) {
            LOGGING_ARGUMENTS = Arrays.asList(
                "--logging.level.com.bbn=TRACE",
                "--logging.level.org.apache.ignite=INFO",
                "--logging.level.tak=TRACE"
            );
        } else {
            String[] logLevelEntries = TAKCLCore.serverLogLevelOverrides.split(" ");
            LOGGING_ARGUMENTS = new ArrayList<>(logLevelEntries.length);

            String logLevel;
            for (int i = 0; i < logLevelEntries.length; i++) {
                logLevel = logLevelEntries[i];
                LOGGING_ARGUMENTS.add("--logging.level." + logLevel);
            }
        }
    }

    protected final AbstractServerProfile serverIdentifier;

    public static Integer debuggeeIdentifier = null;

    private ServerState serverState = ServerState.STOPPED;

    private final OnlineInputModule onlineInputModule = new OnlineInputModule();
    private final OnlineFileAuthModule onlineFileAuthModule = new OnlineFileAuthModule();
    private final OfflineConfigModule offlineConfigModule = new OfflineConfigModule();
    private final OfflineFileAuthModule offlineFileAuthtModule = new OfflineFileAuthModule();

    protected Path logPath;

    protected static String logDirectory;

    protected final List<? extends AbstractServerProcess> processes;


    public final OfflineConfigModule getOfflineConfigModule() {
        checkServerState(false);
        return offlineConfigModule;
    }

    public final OnlineInputModule getOnlineInputModule() {
        checkServerState(true);
        return onlineInputModule;
    }

    public final OfflineFileAuthModule getOfflineFileAuthModule() {
        checkServerState(false);
        return offlineFileAuthtModule;
    }

    public final OnlineFileAuthModule getOnlineFileAuthModule() {
        checkServerState(true);
        return onlineFileAuthModule;
    }

    public static synchronized void setDebuggee(@Nullable Integer serverIdentifier) {
        debuggeeIdentifier = serverIdentifier;

    }

    public static void setLogDirectory(@NotNull String newLogDirectory) {
        logDirectory = newLogDirectory;
    }

    protected AbstractRunnableServer(AbstractServerProfile serverIdentifier) {
        this.logger = LoggerFactory.getLogger(serverIdentifier.toString());
        this.serverIdentifier = serverIdentifier;
        this.offlineConfigModule.init(serverIdentifier);
        this.offlineFileAuthtModule.init(serverIdentifier);

        ServerProcessDefinition[] definitions = ServerProcessDefinition.values();
        ArrayList<AbstractServerProcess> containers = new ArrayList<>(definitions.length);
        for (ServerProcessDefinition definition : ServerProcessDefinition.values()) {
            containers.add(createServerProcess(definition));
        }
        this.processes = Collections.unmodifiableList(containers);
    }

    public void overrideDefaultServerConfiguration(ServerProcessConfiguration serverProcessConfiguration) {
        for (AbstractServerProcess serverProcess : processes) {
            serverProcess.setEnabled(serverProcessConfiguration.isProcessEnabled(serverProcess.definition));
        }
    }

    public final synchronized void stopServer(long serverKillDelayMS) {
        if (serverState == ServerState.STOPPING || serverState == ServerState.STOPPED) {
            logger.warn("Server '" + serverIdentifier.toString() + "' Stop requested even though it is already stopped!");
        }

        logger.info("Stopping server " + serverIdentifier + "...");

        updateEnabledProcessStates();

        try {
            // Since stopping the process will kill the container logs must be collected before shutdown
            if (TAKCLCore.k8sMode) {
                collectFinalLogs();
            }

            serverState = ServerState.STOPPING;

            Exception igniteException = null;
            try {
                TakclIgniteHelper.closeAssociatedIgniteInstance(serverIdentifier);
            } catch (Exception e) {
                igniteException = e;
            }

            if (!TAKCLCore.keepServersRunning) {
                Timer killTimer = null;

                if (serverKillDelayMS > 0) {
                    TimerTask tt = new TimerTask() {
                        @Override
                        public void run() {
                            innerKillServer();
                        }
                    };

                    killTimer = new Timer(false);
                    killTimer.schedule(tt, serverKillDelayMS);
                }
                innerStopServer();
                if (killTimer != null) {
                    killTimer.cancel();
                }
            }

            onlineInputModule.halt();
            onlineFileAuthModule.halt();
            offlineConfigModule.halt();
            offlineFileAuthtModule.halt();

            serverState = ServerState.STOPPED;
            updateEnabledProcessStates();

            if (igniteException != null) {
                throw new RuntimeException(igniteException);
            }
        } finally {
            serverIdentifier.rerollIgnitePorts();
            if (!TAKCLCore.k8sMode) {
                collectFinalLogs();
            }
        }
    }

    public final synchronized void startServer(@NotNull String sessionIdentifier, int maxWaitMs, boolean failTestOnStartupFailure) {
        if (serverState != ServerState.STOPPED) {
            logger.warn("Server '" + serverIdentifier.toString() + "' Start requested even though it is already running or starting!!");
        }

        boolean isFileAuthEnabled = offlineConfigModule.isFileAuthEnabled();

        long startTimeMs = System.currentTimeMillis();

        serverState = ServerState.CONFIGURING;

        Repository repository = this.offlineConfigModule.getRepository();
        // Set the enabled state
        repository.setEnable(TestConfiguration.getInstance().dbEnabled);

        if (TestConfiguration.getInstance().dbEnabled) {
            String dbHost = TestConfiguration.getInstance().getDbHost(serverIdentifier);

            // If the DB Host is set add the credentials
            if (dbHost != null) {
                repository.getConnection().setUrl("jdbc:postgresql://" + dbHost + ":5432/cot");
                repository.getConnection().setUsername("martiuser");
                repository.getConnection().setPassword(serverIdentifier.getDbPassword());
            }
        }

        this.offlineConfigModule.enableSwagger();
        this.offlineConfigModule.setSSLSecuritySettings();
        this.offlineConfigModule.saveChanges();

        if (TestConfiguration.getInstance().dbEnabled)
            TestConfiguration.getInstance().configureDatabase(serverIdentifier);

        // Changing the flow tag to match the server ID
        this.offlineConfigModule.setFlowTag(serverIdentifier.getConsistentUniqueReadableIdentifier());

        String debugServerStr = System.getProperty("com.bbn.marti.takcl.takserver.debug");

        boolean enableRemoteDebug = (debugServerStr != null && debugServerStr.equalsIgnoreCase("true"));

        // Removing default inputs from servers other than SERVER_0 since they will cause bind conflicts
        if (!serverIdentifier.getConsistentUniqueReadableIdentifier().equals(ImmutableServerProfiles.SERVER_0.getConsistentUniqueReadableIdentifier())) {
            List<Input> inputList = new LinkedList<>(this.getOfflineConfigModule().getInputs());
            for (Input input : inputList) {
                if ((input.getPort() == 8088 && input.getName().equals("streamtcp")) ||
                    (input.getPort() == 8087 && (input.getName().equals("stdudp") || input.getName().equals("stdtcp")))) {
                    this.getOfflineConfigModule().removeInput(input.getName());
                }
            }
        } else {
            for (Input input : this.getOfflineConfigModule().getInputs()) {
                Integer networkVersion = ProtocolProfiles.getInputByValue(input.getProtocol()).getCoreNetworkVersion();
                if (networkVersion != null) {
                    input.setCoreVersion(networkVersion);
                }
            }
        }

        this.getOfflineConfigModule().setCertHttpsPort(serverIdentifier.getCertHttpsPort());
        this.getOfflineConfigModule().setFedHttpsPort(serverIdentifier.getFedHttpsPort());
        this.getOfflineConfigModule().sethttpPlaintextPort(serverIdentifier.getHttpPlaintextPort());
        this.getOfflineConfigModule().setHttpsPort(serverIdentifier.getHttpsPort());
        this.getOfflineConfigModule().setIgnitePortRange(serverIdentifier.getIgniteDiscoveryPort(), serverIdentifier.getIgniteDiscoveryPortCount());
        this.getOfflineConfigModule().setSSLSecuritySettings();

        serverState = ServerState.DEPLOYING;

        innerDeployServer(sessionIdentifier, enableRemoteDebug);
        System.out.println(serverIdentifier.getConsistentUniqueReadableIdentifier() + "' started. Waiting for successful initialization.");
        serverState = ServerState.RUNNING;

        waitForServerReady(maxWaitMs, failTestOnStartupFailure);

        System.out.println("Server initialized successfully after " + ((System.currentTimeMillis() - startTimeMs) / 1000) + " seconds.");
        if (!isRunning()) {
            throw new RuntimeException("Server '" + serverIdentifier.getConsistentUniqueReadableIdentifier() + "' appears to have shutdown immediately after starting. Please ensure another server isn't already running and your config is valid!");
        }

        // ignore online input module for fedhub
        if (!serverIdentifier.getConsistentUniqueReadableIdentifier().contains("FEDHUB")) {
            onlineInputModule.init(serverIdentifier);

            if (isFileAuthEnabled) {
                onlineFileAuthModule.init(serverIdentifier);
            }
        }
    }

    private void waitForServerReady(int maxWaitTimeMs, boolean failTestOnStartupFailure) {
        List<? extends AbstractServerProcess> processes = Collections.synchronizedList(getEnabledServerProcesses());
        Map<AbstractServerProcess, List<String>> results = new ConcurrentHashMap<>();

//		try {
//			Thread.sleep(30000);
//		} catch (InterruptedException e) {
//			throw new RuntimeException(e);
//		}
        processes.parallelStream().forEach(p -> results.put(p, p.waitForMissingLogStatements(maxWaitTimeMs)));

        if (results.values().stream().mapToInt(List::size).sum() > 0 && failTestOnStartupFailure) {
            StringBuilder errorBuilder = new StringBuilder();
            for (AbstractServerProcess process : results.keySet()) {
                List<String> failures = results.get(process);
                if (failures.size() > 0) {
                    errorBuilder.append("Server init timeout of " + maxWaitTimeMs + " ms reached for process " +
                        process.getIdentifier() + ".The following log statements were not seen:\n\t" +
                        String.join("\"\n\t\"", failures) + "\n There is a good chance the tests may fail!");
                }
            }
            TAKCLCore.defaultStderr.println(errorBuilder);
            Assert.fail(errorBuilder.toString());
        }
    }

    public synchronized boolean isRunning() {
        return serverState == ServerState.RUNNING;
    }

    public synchronized void watchdogPoll() {
        if (isRunning()) {
            checkServerState(true);
        }
    }

    private synchronized void checkServerState(boolean shouldBeOnline) {
        boolean currentStateValid;
        boolean serverProcessRunning = isServerProcessRunning(shouldBeOnline);
        logger.trace("STATE: serverProcessRunning=" + serverProcessRunning);
        logger.trace("STATE: serverState=" + serverState);
        logger.trace("STATE: shouldBeOnline=" + shouldBeOnline);

        switch (serverState) {
            case CONFIGURING:
            case STOPPED:
                currentStateValid = !serverProcessRunning && !shouldBeOnline;
                break;

            case RUNNING:
                currentStateValid = shouldBeOnline && (serverProcessRunning);
                break;

            case DEPLOYING:
            case STOPPING:
                currentStateValid = false;
                break;

            default:
                throw new RuntimeException("Unexpected state " + serverState + "!");
        }

        if (!currentStateValid) {
            String callingMethod = Thread.currentThread().getStackTrace()[2].getMethodName();
            String msg = "Cannot call " + callingMethod + " on server '" +
                serverIdentifier.getConsistentUniqueReadableIdentifier() + "' while it its state is " +
                serverState.name() + " and the server process is " + (serverProcessRunning ? "" : "not") + " running!!";
            logger.error(msg);
            throw new RuntimeException(msg);
        }
    }

    private final TreeMap<String, Boolean> lastKnownEnabledProcessStates = new TreeMap<>();

    public TreeMap<String, Boolean> updateEnabledProcessStates() {
        synchronized (lastKnownEnabledProcessStates) {
            boolean print = false;
            StringBuilder sb = new StringBuilder("Process States:\n\t");

            // Get the state of all enabled processes
            TreeMap<String, Boolean> currentEnabledProcessStates = new TreeMap<>(processes.stream().filter(
                AbstractServerProcess::isEnabled).collect(Collectors.toMap(
                AbstractServerProcess::getIdentifier, AbstractServerProcess::isRunning)));

            for (AbstractServerProcess process : processes) {
                String processIdentifier = process.getIdentifier();

                if (currentEnabledProcessStates.containsKey(processIdentifier)) {
                    boolean isRunning = currentEnabledProcessStates.get(processIdentifier);

                    if (!lastKnownEnabledProcessStates.containsKey(processIdentifier) ||
                        lastKnownEnabledProcessStates.get(processIdentifier) != isRunning) {
                        print = true;
                    }

                    sb.append("\n\t").append(processIdentifier).append(": ").append(isRunning ? "RUNNING" : "NOT RUNNING");
                } else {
                    sb.append("\n\t").append(processIdentifier).append(": DISABLED");
                }
            }

            if (print) {
                logger.info(sb.toString());
            }
            lastKnownEnabledProcessStates.clear();
            lastKnownEnabledProcessStates.putAll(currentEnabledProcessStates);
            return lastKnownEnabledProcessStates;
        }
    }


    protected void offlineFactoryResetServer() {
        // TODO: This "offline factory reset" should probably be cleanly removed to bring parity to test deployments and be replaced with server "destruction"
        if (!TAKCLCore.k8sMode) {
            logger.error("offlineFactoryResetServer");
            checkServerState(false);
            offlineConfigModule.resetConfig();
            offlineFileAuthtModule.resetConfig();
            logger.error("offlineFactoryResetServer-end");
        }
    }

    public final void killServer() {
        if (!TAKCLCore.keepServersRunning) {
            logger.info("Killing server " + serverIdentifier + "...");
            innerKillServer();
        }
    }

    protected boolean isServerProcessRunning(boolean shouldBeOnline) {
        // If they aren't all the same, raise an exception indicating the difference
        Boolean sharedState = null;

        // Get the state of all enabled processes
        Map<String, Boolean> enabledProcessStates = updateEnabledProcessStates();

        // If no processes have been enabled, the test has not started yet, and things are effectively not running
        if (enabledProcessStates.isEmpty()) {
            return false;
        }

        for (String processName : enabledProcessStates.keySet()) {
            boolean state = enabledProcessStates.get(processName);

            if (shouldBeOnline && !state) {

                if (TAKCLCore.k8sMode) {
                    logger.error("The server process " + processName + " Should be running but it is not!");
                } else {

                    logger.error("The server process " + processName + " Should be running but it is not!  `ps -aux` output:");
                    try {
                        File f = File.createTempFile("PsOutput", ".txt");
                        ProcessBuilder pb = new ProcessBuilder().command("ps", "-aux").redirectErrorStream(true).redirectOutput(f);
                        Process p = pb.start();
                        p.waitFor();
                        String psResults = Files.readString(f.toPath());
                        System.out.println(psResults);

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

            } else if (!shouldBeOnline && state) {
                logger.error("The server process " + processName + " Should not be running but it is!");
            }

            if (sharedState == null) {
                sharedState = state;
            }
            if (state != sharedState) {
                StringBuilder sb = new StringBuilder("Inconsistent process states for " + serverIdentifier + ":");
                for (String processName2 : enabledProcessStates.keySet()) {
                    sb.append(" ").append(processName2).append(".isRunning=").append(enabledProcessStates.get(processName2));
                }
                logger.error(sb.toString());
                throw new RuntimeException(sb.toString());
            }
        }
        return sharedState;
    }

    protected abstract void innerStopServer();

    protected abstract void innerDeployServer(@NotNull String sessionIdentifier, boolean enableRemoteDebug);

    protected abstract void innerKillServer();

    public abstract List<? extends AbstractServerProcess> getEnabledServerProcesses();

    protected abstract void collectFinalLogs();

    protected abstract AbstractServerProcess createServerProcess(ServerProcessDefinition serverProcessDefinition);
}
