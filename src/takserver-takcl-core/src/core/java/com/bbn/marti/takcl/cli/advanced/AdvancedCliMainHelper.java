package com.bbn.marti.takcl.cli.advanced;

import com.bbn.marti.config.Configuration;
import com.bbn.marti.takcl.AppModules.generic.AppModuleInterface;
import com.bbn.marti.takcl.AppModules.generic.BaseAppModuleInterface;
import com.bbn.marti.takcl.AppModules.generic.ServerAppModuleInterface;
import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.takcl.TakclIgniteHelper;
import com.bbn.marti.takcl.Util;
import com.bbn.marti.takcl.cli.EndUserReadableException;
import com.bbn.marti.takcl.cli.InvalidArgumentValueException;
import com.bbn.marti.takcl.cli.NoSuchCommandArgumentException;
import com.bbn.marti.takcl.cli.NoSuchCommandException;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.bbn.marti.takcl.cli.CommandCommon.output;

/**
 * Created on 10/27/17.
 */
public class AdvancedCliMainHelper {

	private static Timer failsafeTimer = null;
	private static BaseAppModuleInterface moduleInstance = null;

	private static void halt(@Nullable String endUserReadableErrorMessage, int exitCode) {
		if (endUserReadableErrorMessage != null) {
			System.err.println(endUserReadableErrorMessage);
		}
		if (failsafeTimer != null) {
			failsafeTimer.cancel();
		}
		if (moduleInstance instanceof ServerAppModuleInterface) {
			((ServerAppModuleInterface) moduleInstance).halt();
		}
		System.exit(exitCode);
	}

	private static void halt(EndUserReadableException e) {
		halt(e.getMessage(), 1);
	}

	public static void main(String formalTitle, String jarIdentifier, Long failsafeTimeout, BaseAppModuleInterface module, String[] methodIdentifiers, String[] args) {
		System.setProperty("java.net.preferIPv4Stack", "true");

		moduleInstance = module;

		if (failsafeTimeout != null && failsafeTimeout > 0) {
			TimerTask tt = new TimerTask() {
				@Override
				public void run() {
					System.err.println("Hit timeout trying to execute the command. Are you sure the server is running on this machine?");
					System.exit(-1);
				}
			};
			failsafeTimer = new Timer();
			failsafeTimer.schedule(tt, failsafeTimeout);
		}


		if (args.length == 0) {
			String[] params = new String[1];
			try {
				String ident = "  ";

				Map<String, List<String>> usageLines = new HashMap<>();
				for (String methodIdentifier : methodIdentifiers) {
					params[0] = methodIdentifier;
					List<String> newLines = new AdvancedParamParser(module, params).getDisplayableMethod();
					newLines.add("\n");
					usageLines.put(methodIdentifier, newLines);
				}

				output.println(formalTitle + " Usage Instructions:\n");
				List<String> sortedMethodIdentifiers = Arrays.asList(methodIdentifiers);
				Collections.sort(sortedMethodIdentifiers);

				for (String identifier : sortedMethodIdentifiers) {
					output.println(ident + "java -jar " + jarIdentifier + " " + identifier);
					for (String line : usageLines.get(identifier)) {
						output.println(ident + ident + line);
					}
					output.println("\n");
				}
			} catch (EndUserReadableException e) {
				halt(e);
			}
			halt(null, 0);
		}

		try {
			AdvancedParamParser mpm = new AdvancedParamParser(module, args);

			// If no arguments, display usage info
			if (args.length == 1) {
				for (String line : mpm.getDisplayableMethod()) {
					output.println(line);
				}
				System.exit(0);
			}

			// Otherwise, try parsing the arguments
			mpm.processParameters();

			// Construct server instance
			AbstractServerProfile spi = ImmutableServerProfiles.DEFAULT_LOCAL_SERVER;

			Configuration localConfig = Util.getCoreConfig();
			if (localConfig != null && !TAKCLCore.cliIgnoreCoreConfig) {
				TakclIgniteHelper.overrideServerConfigurationFromCoreConfig(spi, localConfig);
			} else {
				System.err.println("WARNING: No CoreConfig could be found. The default server configuration details will be used instead!");
			}

			if (module instanceof ServerAppModuleInterface) {
				((ServerAppModuleInterface) module).init(spi);
			} else if (module instanceof AppModuleInterface) {
				((AppModuleInterface) module).init();
			} else {
				throw new RuntimeException("Only classes of types 'ServerAppModuleInterface' and 'AppModuleInterface' can be used with the CLI interfaces!");
			}

			String message = mpm.execute();
			if (message != null && !message.equals("")) {
				output.println(message);
			}

		} catch (EndUserReadableException e) {
			halt(e);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
			halt(e.getMessage(), 1);
		}
		halt(null, 0);
	}
}
