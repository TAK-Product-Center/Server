package com.bbn.marti.takcl.cursedtak;

import com.bbn.marti.takcl.AppModules.TAKCLConfigModule;
import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.takcl.Util;
import com.bbn.marti.takcl.config.TAKCLConfiguration;
import com.bbn.marti.takcl.connectivity.implementations.ConnectibleTakprotoClient;
import com.bbn.marti.takcl.connectivity.implementations.UnifiedClient;
import com.bbn.marti.test.shared.data.connections.MutableConnection;
import com.bbn.marti.test.shared.data.generated.CLINonvalidatingUsers;
import com.bbn.marti.test.shared.data.generated.ImmutableConnections;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.servers.MutableServerProfile;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import picocli.CommandLine;
import sun.misc.Unsafe;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.concurrent.Callable;

/**
 * Created on 2/27/18.
 */
public class CursedTak implements Callable<Void> {

	@CommandLine.Option(names = {"-u", "--user"}, required = true, description = "The user to connect as")
	private CLINonvalidatingUsers user;

	@CommandLine.Option(names = {"-h", "--help"}, usageHelp = true)
	private boolean help;

//	@CommandLine.Option(names = {"-l", "--log-to-file"}, description = "Logs to the file 'cursedtak-<port>-<user>.log' in the launch directory. Otherwise all logging will be ignored.")
//	private boolean logToFile;

	@CommandLine.Option(names = {"-c", "--connect-on-startup"}, description = "Connects the client on startup, authenticating if necessary.")
	private boolean connectOnStartup;

	@CommandLine.Option(names = {"-r", "--start-in-rx-mode"}, description = "Starts the client with the receive window open instead of the send")
	private boolean startInRxMode;

	@CommandLine.Option(names = {"-i", "--ip-address"}, description = "Overrides the default IP address the user connects to")
	private String ipAddress;

	@CommandLine.Option(names = {"-p", "--port"}, description = "Overrides the default port the user connects to")
	private Integer port;

	@CommandLine.Option(names = {"--c", "--cert"}, description = "The path to the user certificates minus the extension. So for example if the public ert is '/home/user/test.pem' and the private cert jks is '/home/user/test.jks', supply '/home/user/test'.")
	private String certRootPath;

	public static void main(String args[]) {
		CommandLine.call(new CursedTak(), args);
//		try {
//			CursedTak ct = CommandLine.populateCommand(new CursedTak(), args);
//
//			if (ct.help) {
//				CommandLine.usage(ct, System.out);
//			} else {
//
//				TAKCLCore.TEST_MODE = false;
//
//				UnifiedClient client = new UnifiedClient(ct.user.getUser());
//
//				String logFilepath = (ct.logToFile ? "cursedtak" + ct.user.getUser().getConnection().getPort() + "-" + ct.user.name() + ".log" : null);
//
////                if (args.length > 0) {
////                    client = new UnifiedClient(CLINonvalidatingUsers.valueOf(args[0]).getUser());
////                } else {
////                    client = new UnifiedClient(CLINonvalidatingUsers.s0_stcp0_anonuser_0f.getUser());
////                }
//				CursedTAKTerminal ctt = new CursedTAKTerminal(
//						client,
//						(ct.startInRxMode ? CursedTakMode.COT_RECEIVE : CursedTakMode.COT_SEND),
//						logFilepath,
//						ct.connectOnStartup);
//			}
//		} catch (CommandLine.MissingParameterException e) {
//			System.out.println(e.getMessage());
//			System.out.println();
//			CommandLine.usage(new CursedTak(), System.out);
//
//		}
	}

	@Override
	public Void call() throws Exception {
		TAKCLCore.TEST_MODE = false;

		PrintStream defaultStdOut = System.out;

			UnifiedClient client;

			String logFilePrefix = "cursedtak" + user.getUser().getConnection().getPort() + "-" + user.name();
			PrintStream stdout = new PrintStream(new FileOutputStream(logFilePrefix + "-stdout.log"));
			PrintStream stderr = new PrintStream(new FileOutputStream(logFilePrefix + "-stderr.log"));
			System.setOut(stdout);
			System.setErr(stderr);

		disableWarning();

		Path certPublicPemPath = null, certPrivateJskpath = null;
		if (certRootPath != null) {
			certPublicPemPath = Paths.get(certRootPath + ".pem").toAbsolutePath();
			certPrivateJskpath = Paths.get(certRootPath + ".jks").toAbsolutePath();
			if (Files.exists(certPublicPemPath)) {
				if (!Files.exists(certPrivateJskpath)) {
					throw new RuntimeException("File '" + certPublicPemPath + "' exists but '" + certPrivateJskpath + "' does not!");
				}
			} else {
				if (Files.exists(certPrivateJskpath)) {
					throw new RuntimeException("File '" + certPrivateJskpath + "' exists but '" + certPublicPemPath + "' does not!");
				} else {
					certPublicPemPath = null;
					certPrivateJskpath = null;
				}
			}
		}

		if (ipAddress == null && port == null && certPublicPemPath == null && certPrivateJskpath == null) {
			client = new UnifiedClient(user.getUser());
		} else {
			AbstractServerProfile userServer = user.getUser().getServer();
			ImmutableConnections userConnection = (ImmutableConnections) user.getUser().getConnection();
			MutableServerProfile serverProfileInstance;
			MutableConnection connectionInstance;

			AbstractUser userInstance;
			if (ipAddress == null) {
				serverProfileInstance = MutableServerProfile.Builder.build(userServer).create();
			} else {
				serverProfileInstance = MutableServerProfile.Builder.build(userServer).setUrl(ipAddress).create();
			}

			if (port == null) {
				connectionInstance = serverProfileInstance.createConnectionBuilder(userConnection.getConnectionModel()).create();
			} else {
				connectionInstance = serverProfileInstance.createConnectionBuilder(userConnection.getConnectionModel()).setPort(port).create();
			}

			if (certPublicPemPath == null && certPrivateJskpath == null) {
				userInstance = connectionInstance.generateConnectionUser(user.getUser().getUserModel(), false);
			} else {
				userInstance = connectionInstance.generateConnectionUser(user.getUser().getUserModel(), false, certPrivateJskpath, certPublicPemPath);
			}

			client = new UnifiedClient(userInstance);
		}


		CursedTAKTerminal ctt = new CursedTAKTerminal(
				client,
				(startInRxMode ? CursedTakMode.COT_RECEIVE : CursedTakMode.COT_SEND),
//				logFilepath,
				connectOnStartup,
				defaultStdOut);

		return null;
	}

	public static void disableWarning() {
		try {
			Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			Unsafe u = (Unsafe) theUnsafe.get(null);

			Class cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
			Field logger = cls.getDeclaredField("logger");
			u.putObjectVolatile(cls, u.staticFieldOffset(logger), null);
		} catch (Exception e) {
			// ignore
		}
	}
}
