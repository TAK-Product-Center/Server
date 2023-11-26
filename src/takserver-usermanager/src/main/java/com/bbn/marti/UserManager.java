package com.bbn.marti;

import java.util.Arrays;
import java.util.HashSet;

import org.slf4j.LoggerFactory;

import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.takcl.TAKCLogging;
import com.bbn.marti.takcl.TestExceptions;
import com.bbn.marti.takcl.AppModules.OfflineFileAuthModule;
import com.bbn.marti.takcl.AppModules.OnlineFileAuthModule;
import com.bbn.marti.takcl.AppModules.generic.ServerAppModuleInterface;
import com.bbn.marti.takcl.cli.advanced.AdvancedCliMainHelper;
import tak.server.util.JavaVersionChecker;


import ch.qos.logback.classic.Level;

/**
 * Created on 1/19/16.
 */
public class UserManager extends AdvancedCliMainHelper {

	public static void main(String[] args) {
		JavaVersionChecker.check();
		System.setProperty("java.net.preferIPv4Stack", "true");

		TestExceptions.DO_NOT_CLOSE_IGNITE_INSTANCES = false;

		TAKCLogging.setClassLoggerBuilder((aClass, s) -> {
			ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(aClass);
			if (s != null) {
				System.err.println("Setting logging level for '" + aClass.getCanonicalName() + "' to '" + s + "'.");
				logger.setLevel(Level.valueOf(s));
			}
			return logger;
		}, new HashSet<>(Arrays.asList("ALL", "DEBUG", "ERROR", "INFO", "OFF", "TRACE", "WARN")));

		TAKCLogging.setStringLoggerBuilder((aClassName, s) -> {
			ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(aClassName);
			if (s != null) {
				System.err.println("Setting logging level for '" + aClassName + "' to '" + s + "'.");
				logger.setLevel(Level.valueOf(s));
			}
			return logger;
		}, new HashSet<>(Arrays.asList("ALL", "DEBUG", "ERROR", "INFO", "OFF", "TRACE", "WARN")));

		TAKCLCore.TEST_MODE = false;
		ServerAppModuleInterface module;
		if (args.length >= 1 && args[0].equals("--offline")) {
			System.err.println("Offline flag detected. Running in offline mode.");
			args = Arrays.copyOfRange(args, 1, args.length);
			module = new OfflineFileAuthModule();

		} else {
			module = new OnlineFileAuthModule();
		}

		String[] methods = {"usermod", "certmod"};
		AdvancedCliMainHelper.main("TAKServer UserManager", "UserManager.jar", TAKCLCore.userManagerTimeout, module, methods, args);
	}
}
