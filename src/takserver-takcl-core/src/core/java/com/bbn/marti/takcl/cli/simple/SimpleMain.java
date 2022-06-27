package com.bbn.marti.takcl.cli.simple;

import com.bbn.marti.takcl.AppModules.generic.BaseAppModuleInterface;
import com.bbn.marti.takcl.cli.CommandCommon;
import com.bbn.marti.takcl.cli.EndUserReadableException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.bbn.marti.takcl.cli.CommandCommon.output;

/**
 * Created on 8/29/18.
 */
public class SimpleMain {

	public final Map<String, BaseAppModuleInterface> moduleMap = new HashMap<>();

	public boolean showDevCommands = false;

	public final void addModule(String identifier, BaseAppModuleInterface module) {
		moduleMap.put(identifier, module);
	}

	private void printHelp(boolean alwaysShowCommandNames) {
		int moduleCount = moduleMap.size();

		if (moduleCount > 1 || alwaysShowCommandNames) {
			output.println("Command Groups:\n");
			for (String moduleKey : moduleMap.keySet()) {
				BaseAppModuleInterface module = moduleMap.get(moduleKey);
				output.println("\t" + moduleKey + "\n\t\t" + module.getCommandDescription() + "\n");
			}
		} else {
			output.println("Available Commands:\n");
			for (String moduleKey : moduleMap.keySet()) {
				BaseAppModuleInterface module = moduleMap.get(moduleKey);
				output.println("\n\t" + module.getCommandDescription() + "\n");
			}
		}

		output.println("Please provide the command group as a parameter to see usage instructions.");
	}

	/**
	 * The expected command structure is as follows:
	 * <p>
	 * If only a single module is loaded:
	 * executable moduleMethodName params
	 * <p>
	 * If multiple modules are loaded:
	 * executable moduleIdentifier moduleMethodName params
	 *
	 * @param args The arguments to pass in from the main method
	 */
	public final void run(String[] args) {
		run(false, args);
	}

	/**
	 * The expected command structure is as follows:
	 * <p>
	 * If only a single module is loaded:
	 * executable moduleMethodName params
	 * <p>
	 * If multiple modules are loaded:
	 * executable moduleIdentifier moduleMethodName params
	 *
	 * @param alwaysShowCommandNames Whether or not the command is shown and needed if only one module is present.
	 *                               If true, The command will be shown and will be necessary to view and execute
	 *                               command functions. If false, The command view will be skipped and the single
	 *                               module will appear as the executable
	 * @param args                   The arguments to pass in from the main method
	 */
	public final void run(boolean alwaysShowCommandNames, String[] args) {
;
		try {
			boolean singleModuleMode = moduleMap.size() == 1 && !alwaysShowCommandNames;

			String moduleName = singleModuleMode ? moduleMap.keySet().iterator().next() : args.length > 0 ? args[0] : null;

			// If no module name was provided print the top level help
			if (moduleName == null) {
				printHelp(alwaysShowCommandNames);
				System.exit(1);
			}

			BaseAppModuleInterface module = moduleMap.get(moduleName);

			// If the module name isn't valid indicate so and print the top level help
			if (module == null) {
				output.println("No module found with the identifier \"" + args[0] + "\"");
				output.println();
				printHelp(alwaysShowCommandNames);
				System.exit(1);
			}

			SimpleCommandParser parser = new SimpleCommandParser(false, showDevCommands, module);

			String command;

			if (singleModuleMode) {
				if (args.length == 0) {
					//  If no parameters for single module mode print the help for the command and exit
					parser.printUsage();
					System.exit(1);

				} else {
					command = args[0];
				}
			} else {
				if (args.length == 1) {
					//  If no additional for multi-module mode print the help for the command and exit
					parser.printUsage();
					System.exit(1);
				} else {
					command = args[1];
				}
			}

			String[] paramArray;

			if (singleModuleMode) {
				paramArray = (args.length > 1 ? Arrays.copyOfRange(args, 2, args.length) : new String[0]);
			} else {
				paramArray = (args.length > 2 ? Arrays.copyOfRange(args, 2, args.length) : new String[0]);
			}

			Method method = SimpleCommandParser.getMethod(args[0], args[1], paramArray, moduleMap, true);
			Object[] params = SimpleCommandParser.preprocessParams(method, paramArray);

			if (method == null) {
				output.println("The module " + args[0] + " does not contain the provided method.");
				output.println();
				printHelp(alwaysShowCommandNames);
				System.exit(1);
			}

			Object returnVal = CommandCommon.invokeMethodFromModuleWithParams(method, module, params);

			if (returnVal != null) {
				output.println(returnVal);
			}

		} catch (EndUserReadableException e) {
			output.println(e.getMessage());
			System.exit(1);

		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			printHelp(alwaysShowCommandNames);
			System.exit(1);
		}
	}

	public static void main(String[] args) {

	}
}
