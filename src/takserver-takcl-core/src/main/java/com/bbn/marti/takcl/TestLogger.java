package com.bbn.marti.takcl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;


/**
 * This class allows comparison of test runs. The original intent of this was to validate a significant change to the
 * test system matched the previous behavior.  It could be used to monitor behavior changes over time that don't
 * necessarily result in test failures.
 * <p>
 * It isn't directly written to a file until {@link TestLogger#flushLog()} is called so that unsorted data can be
 * sorted for easier visual comparison
 * <p>
 * Created on 9/15/16.
 */
public class TestLogger {
	private static TestLog currentTestLog = null;
	private static Gson gson;
	private static String loggingDirectory = null;
	private static Thread exitThread;

	/**
	 * Changes the logging directory when the current static logger is replaced with {@link TestLogger#startTestWithIdentifier(String)}
	 *
	 * @param targetDirectory The target directory for new logs
	 */
	public static void setFileLogging(@NotNull String targetDirectory) {
		System.out.println("TestLogger setFileLogging is set to " + targetDirectory);
		loggingDirectory = targetDirectory;
	}

	/**
	 * Starts a new test log file
	 *
	 * @param testIdentifier The identifier for the test
	 */
	public static void startTestWithIdentifier(@NotNull String testIdentifier) {
		System.out.println("TestLogger startTestWithIdentifier " + testIdentifier + "...");
		flushLog();

		if (exitThread == null) {
			exitThread = new Thread() {
				@Override
				public void run() {
					flushLog();
				}
			};
			Runtime.getRuntime().addShutdownHook(exitThread);
		}

		currentTestLog = new TestLog(testIdentifier);
	}

	/**
	 * Flushes the current logging data out to a file. This method sorts unsorted data to aid in visual analysis
	 * and then writes it to the file
	 */
	private static void flushLog() {
		try {
			if (currentTestLog != null) {
				if (gson == null) {
					gson = new GsonBuilder().setPrettyPrinting().create();
				}

				currentTestLog.tidyUp();

				Path filepath = Paths.get(loggingDirectory).resolve(currentTestLog.testIdentifier).toAbsolutePath();
				if (!Files.exists(filepath)) {
					Files.createDirectory(filepath);
				}
				filepath = filepath.resolve("activityLog.json");
				System.out.println("TestLogger writing to: " + filepath);
				JsonWriter writer = new JsonWriter(new FileWriter(filepath.toFile()));
				writer.setIndent("    ");
				gson.toJson(currentTestLog, TestLog.class, writer);
				writer.flush();
				writer.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	/**
	 * Starts a new engine execution sequence
	 *
	 * @param commandIdentifier The identifier for the command
	 */
	public static void executeEngineCommand(@NotNull String commandIdentifier) {
		currentTestLog.startCommandLog(commandIdentifier);
	}

	/**
	 * Logs a user send event after verification
	 *
	 * @param sender        The sender
	 * @param count         The number of messages sent
	 * @param targetUser    The target user
	 * @param justification The justification
	 */
	public static void logUserSend(@NotNull String sender, int count, @NotNull String targetUser, @Nullable String justification) {
		LinkedList<String> recipientSet = currentTestLog.currentCommandLog.userSendMap.get(sender);
		if (recipientSet == null) {
			recipientSet = new LinkedList<>();
			currentTestLog.currentCommandLog.userSendMap.put(sender, recipientSet);
		}

		for (int i = 0; i < count; i++) {
			recipientSet.add(targetUser);
		}

		System.out.println("TestLogger: logUserSend: "+ sender + " >-" + count + "-> " + targetUser + " - Justification: " + justification);
	}

	/**
	 * Logs a user connection event
	 *
	 * @param userIdentifier The identifier for the user
	 * @param justification  The justification for the connection
	 */
	public static void logUserConnected(@NotNull String userIdentifier, @Nullable String justification) {
		currentTestLog.currentCommandLog.connectedUsers.add(userIdentifier);
		System.out.println("TestLogger: " + userIdentifier + " connected. Justification: " + justification);
	}

	public static void logUserAuthenticated(@NotNull String userIdentifier, @Nullable String justification) {
		currentTestLog.currentCommandLog.authenticatedUsers.add(userIdentifier);
		System.out.println("TestLogger: " + userIdentifier + " authenticated. Justification: " + justification);
	}


	/**
	 * Logs a user disconnection event
	 *
	 * @param userIdentifier The identifier for the user
	 * @param justification  The justification for the connection
	 */
	public static void logUserDisconnected(@NotNull String userIdentifier, @Nullable String justification) {
		currentTestLog.currentCommandLog.disconnectedUsers.add(userIdentifier);
		System.out.println("TestLogger: " + userIdentifier + " disconnected. Justification: " + justification);
	}

	/**
	 * Tracks the sequence of engine executions for a single test
	 */
	static class TestLog {
		private final String testIdentifier;
		private transient EngineCommandLog currentCommandLog;
		private final LinkedList<EngineCommandLog> commandLogs;

		TestLog(@NotNull String testIdentifier) {
			this.testIdentifier = testIdentifier;
			commandLogs = new LinkedList<>();
		}

		/**
		 * Sorts contained items so they are easier to diff when output to Json
		 */
		void tidyUp() {
			for (EngineCommandLog ecl : commandLogs) {
				ecl.tidyUp();
			}

		}

		/**
		 * Starts a new engine execution sequence
		 *
		 * @param commandIdentifier The identifier for the command
		 */
		void startCommandLog(@NotNull String commandIdentifier) {
			currentCommandLog = new EngineCommandLog(commandIdentifier);
			commandLogs.add(currentCommandLog);
		}

		/**
		 * Compares the provided {@link TestLog} object to this one
		 *
		 * @param obj the {@link TestLog} to compare
		 * @return A list of error strings if they differ.
		 */
		List<String> equalityErrorList(TestLog obj) {
			LinkedList<String> errorList = new LinkedList<>();
			if (obj == null) {
				errorList.add(testIdentifier + ".TestLogB(isNull)");
				return errorList;
			}


			if (!testIdentifier.equals(obj.testIdentifier)) {
				System.out.println(testIdentifier + "!=" + obj.testIdentifier);
				errorList.add("testIdentifier(\"" + testIdentifier + "\" != \"" + obj.testIdentifier + "\")");
			}

			if (commandLogs.size() != obj.commandLogs.size()) {
				errorList.add(testIdentifier + ".commandLogs.size(" + commandLogs.size() + " != " + obj.commandLogs.size() + ")");
			}

			int iterationCount = (commandLogs.size() <= obj.commandLogs.size() ? commandLogs.size() : obj.commandLogs.size());

			for (int i = 0; i < iterationCount; i++) {
				EngineCommandLog eclA = commandLogs.get(i);
				EngineCommandLog eclB = obj.commandLogs.get(i);
				List<String> inequalityList = eclA.equalityErrorList(eclB);
				if (inequalityList != null) {
					for (String value : inequalityList) {
						errorList.add(testIdentifier + "." + value);
					}
				}
			}

			return errorList;
		}
	}

	/**
	 * Tracks a single engine execution flow.
	 * <p>
	 * Each command is listed in {@link com.bbn.marti.test.shared.engines.EngineInterface}.
	 * <p>
	 * {@link com.bbn.marti.test.shared.engines.TestEngine} executes the commands in the sequence each instance
	 * of this object is meant to record.
	 */
	public static class EngineCommandLog {
		private final TreeMap<String, LinkedList<String>> userSendMap = new TreeMap<>();
		private final String commandIdentifier;
		private final LinkedList<String> connectedUsers = new LinkedList<>();
		private final LinkedList<String> authenticatedUsers = new LinkedList<>();
		private final LinkedList<String> disconnectedUsers = new LinkedList<>();

		/**
		 * The object for a single engine execution flow
		 *
		 * @param commandIdentifier The identifier for the engine command being executed
		 */
		EngineCommandLog(@NotNull String commandIdentifier) {
			this.commandIdentifier = commandIdentifier;
		}


		/**
		 * Sorts contained items so they are easier to diff when output to Json
		 */
		public void tidyUp() {
			Collections.sort(connectedUsers);
			Collections.sort(authenticatedUsers);
			Collections.sort(disconnectedUsers);
			for (List users : userSendMap.values()) {
				Collections.sort(users);
			}
		}


		/**
		 * Compares the value of the log objects
		 *
		 * @param obj the object to compare to
		 * @return a list of strings indicating any inequalities detected
		 */
		public List<String> equalityErrorList(Object obj) {
			LinkedList<String> errorList = new LinkedList<>();
			if (obj == null) {
				errorList.add(commandIdentifier + ".EngineCommandLogB(isNull)");
				return errorList;
			} else if (!(obj instanceof EngineCommandLog)) {
				errorList.add(commandIdentifier + ".EngineCommandLogB(!EngineCommandLog)");
				return errorList;
			}

			EngineCommandLog o = (EngineCommandLog) obj;

			if (!commandIdentifier.equals(o.commandIdentifier)) {
				errorList.add("commandIdentifier(\"" + commandIdentifier + "\" != + \"" + o.commandIdentifier + "\"]");
			}

			Collections.sort(connectedUsers);
			Collections.sort(o.connectedUsers);
			if (!connectedUsers.equals(o.connectedUsers)) {
				errorList.add(commandIdentifier + ".connectedUsers(!=");
			}

			Collections.sort(authenticatedUsers);
			Collections.sort(o.authenticatedUsers);
			if (!authenticatedUsers.equals(o.authenticatedUsers)) {
				errorList.add(commandIdentifier + ".authenticatedUsers(!=");
			}

			Collections.sort(disconnectedUsers);
			Collections.sort(o.disconnectedUsers);
			if (!disconnectedUsers.equals(o.disconnectedUsers)) {
				errorList.add(commandIdentifier + ".disconnectedUsers(!=)");
			}

			Set<String> usmKs = userSendMap.keySet();
			Set<String> usm2Ks = o.userSendMap.keySet();
			if (!usmKs.equals(usm2Ks)) {
				errorList.add(commandIdentifier + ".userSendMap.keySet(!=)");
			}

			for (String sendingUser : usmKs) {
				LinkedList<String> recipientUsersA = userSendMap.get(sendingUser);
				LinkedList<String> recipientUsersB = o.userSendMap.get(sendingUser);
				if (recipientUsersA != null) {
					Collections.sort(recipientUsersA);
				}

				if (recipientUsersB != null) {
					Collections.sort(recipientUsersB);
				}

				if ((recipientUsersA != null && !recipientUsersA.equals(recipientUsersB)) ||
						(recipientUsersB != null && !recipientUsersB.equals(recipientUsersA))) {
					errorList.add(commandIdentifier + ".sendingUser[\"" + sendingUser + "\"].recipients(!=)");

				}
			}

			return errorList;
		}
	}

	/**
	 * Performs a comparison of two directories containing TestLog pojos
	 *
	 * @param dirpathA A directory containing test logs
	 * @param dirpathB A directory containing test logs
	 * @return A list of inequalities if any were found
	 */
	public static List<String> compareTestArtifactDirectoriesJsonFiles(@NotNull String dirpathA, @NotNull String dirpathB) {
		LinkedList<String> errorList = new LinkedList<>();

		String[] extensions = {"json"};
		TreeSet<File> filesetA = new TreeSet<>(FileUtils.listFiles(new File(dirpathA), extensions, true));
		TreeSet<File> filesetB = new TreeSet<>(FileUtils.listFiles(new File(dirpathB), extensions, true));

		TreeMap<String, File> fileMapA = new TreeMap<>();
		for (File f : filesetA) {
			String filepath = f.getPath();
			filepath = filepath.substring(filepath.indexOf("/") + 1);
			fileMapA.put(filepath, f);
		}

		TreeMap<String, File> fileMapB = new TreeMap<>();
		for (File f : filesetB) {
			String filepath = f.getPath();
			filepath = filepath.substring(filepath.indexOf("/") + 1);
			fileMapB.put(filepath, f);
		}

		for (String fileA : fileMapA.keySet()) {
			File fileB = fileMapB.remove(fileMapB.firstKey());
			if (fileB != null) {
				List<String> errors = compareJsonFiles(fileMapA.get(fileA).getAbsolutePath(), fileB.getAbsolutePath());
				if (errors != null) {
					errorList.addAll(errors);
				}
			} else {
				errorList.add("The file '" + fileA + "' was not found in '" + dirpathB + "'!");
			}
		}

		if (!fileMapB.isEmpty()) {
			for (String filepath : fileMapB.keySet()) {
				errorList.add("The file '" + filepath + "' was not found in '" + dirpathA + "'!");
			}

		}
		return errorList;
	}

	/**
	 * Performs a comparison of two JSON-formatted TestLog objects
	 *
	 * @param filepathA The directory path to a {@link TestLog} JSON pojo
	 * @param filepathB The directory path to a {@link TestLog} JSON pojo
	 * @return A list of Strings indicating any inequalities found
	 */
	public static List<String> compareJsonFiles(@NotNull String filepathA, @NotNull String filepathB) {
		if (gson == null) {
			gson = new GsonBuilder().setPrettyPrinting().create();
		}

		TestLog testLogA, testLogB;

		try {
			FileReader fr = new FileReader(filepathA);
			testLogA = gson.fromJson(fr, TestLog.class);
			fr.close();
			fr = new FileReader(filepathB);
			testLogB = gson.fromJson(fr, TestLog.class);
			fr.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return testLogA.equalityErrorList(testLogB);
	}
}
