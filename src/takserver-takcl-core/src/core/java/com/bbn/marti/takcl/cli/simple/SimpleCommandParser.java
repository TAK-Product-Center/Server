package com.bbn.marti.takcl.cli.simple;

import com.bbn.marti.takcl.AppModules.generic.BaseAppModuleInterface;
import com.bbn.marti.takcl.cli.CommandCommon;
import com.bbn.marti.takcl.cli.EndUserReadableException;
import com.bbn.marti.takcl.cli.InvalidArgumentValueException;
import com.bbn.marti.takcl.cli.NoSuchCommandArgumentException;
import com.bbn.marti.takcl.cli.NoSuchCommandException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.bbn.marti.takcl.cli.CommandCommon.output;

/**
 * Used to generate and execute commands
 * <p>
 * Created on 8/18/17.
 */
public class SimpleCommandParser {

	private final boolean showDevCommands;
	private final BaseAppModuleInterface commandClassInstance;
	private final Class clazz;
	private Method classMethod;
	private Object[] orderedArgumentList;

	public SimpleCommandParser(boolean useV2CommandInterface, boolean showDevCommands, BaseAppModuleInterface commandClassInstance) {
		this.showDevCommands = showDevCommands;
		this.commandClassInstance = commandClassInstance;
		this.clazz = commandClassInstance.getClass();
	}

	private static BaseAppModuleInterface getModule(@NotNull String moduleIdentifier, @NotNull Map<String, BaseAppModuleInterface> moduleMap) {
		BaseAppModuleInterface module;

		// Initialize the module and parse out the method getConsistentUniqueReadableIdentifier
		for (String identifier : moduleMap.keySet()) {
			String trueIdentifier = identifier.toLowerCase();
			String providedIdentifier = moduleIdentifier.toLowerCase();

			if (providedIdentifier.startsWith(trueIdentifier)) {
				module = moduleMap.get(identifier);
				return module;
			}
		}
		return null;
	}

	/**
	 * Unpacks varargs if they exist
	 * @return
	 */
	public static Object[] preprocessParams(@NotNull Method method, @Nullable Object[] methodParams) {
		Class<?>[] paramTypes = method.getParameterTypes();

		if (methodParams == null || methodParams.length == 0) {
			return methodParams;
		}

		if (!paramTypes[paramTypes.length - 1].getName().startsWith("[L")) {
			if (methodParams.length > paramTypes.length) {
				throw new EndUserReadableException("Too many arguments provided!");
			}

		} else {
			int nonVaragsLength = paramTypes.length - 1;
			int varargsLength = methodParams.length - nonVaragsLength;

			ArrayList<Object> paramList = new ArrayList<>(Arrays.asList(methodParams).subList(0, nonVaragsLength));

			Object[] newValue = (Object[]) Array.newInstance(methodParams[methodParams.length - 1].getClass(), varargsLength);

			System.arraycopy(methodParams, nonVaragsLength, newValue, 0, newValue.length);

			paramList.add(newValue);
			methodParams = paramList.toArray();
		}
		return methodParams;
	}

	public static Method getMethod(@NotNull String moduleIdentifier, @NotNull String methodName, @Nullable String[] parameters, @NotNull Map<String, BaseAppModuleInterface> moduleMap, boolean includeDev) {
		// TODO: Add case validation and parameter getConsistentUniqueReadableIdentifier validaton?


		BaseAppModuleInterface module = getModule(moduleIdentifier, moduleMap);
		String providedMethodNameLC = methodName.toLowerCase();

		if (module == null) {
			return null;
		}

		int paramIndex = providedMethodNameLC.indexOf('[');
		if (paramIndex >= 0) {
			providedMethodNameLC = providedMethodNameLC.substring(0, paramIndex);
		}

		for (Method method : module.getClass().getDeclaredMethods()) {
			String methodNameLC = method.getName().toLowerCase();


			Command command = null;
			Annotation[] annotations = method.getDeclaredAnnotations();
			for (Annotation a : annotations) {
				if (a.annotationType() == Command.class) {
					command = (Command) a;
				}
			}


			// If the method name matches, it is a command, and dev is included or it is not dev, continue
			if (providedMethodNameLC.equals(methodNameLC) &&
					command != null &&
					(includeDev || !command.isDev())) {


//            if (providedMethodNameLC.equals(methodNameLC) &&
//                    method.getAnnotation(AbstractAppModule.Command.class) != null &&
//                    (includeDev || !method.getDeclaredAnnotation(AbstractAppModule.Command.class).isDev())) {


//                if (providedMethodNameLC.equals(methodNameLC)) {

				Class[] parameterTypes = method.getParameterTypes();
				// If the parameter count matches or the last parameter is of variable length
				if (((parameters == null || parameters.length == 0) && (parameterTypes == null || parameterTypes.length == 0)) ||
						parameters.length == parameterTypes.length ||
						(parameters.length > parameterTypes.length && parameterTypes[parameterTypes.length - 1].getName().startsWith("[L"))) {
					return method;
				}
//                } else {
//                    String[] paramArray = providedMethodName.substring(methodNameLC.length()).split("With");
//                    List<String> parameters = new LinkedList<>();
//                    for (String str : paramArray) {
//                        if (!Strings.isNullOrEmpty(str)) {
//                            parameters.addUser(str);
//                        }
//                    }
//
//                    if (parameters.size() == method.getParameterCount() && (args.length - 2 == method.getParameterCount())) {
//                        return method;
//                    }
//                }
			}
		}
		return null;
	}

	public static String getDisplayableMethod(Method classMethod, Method commandMethod) {
		Command annotation = commandMethod.getAnnotation(Command.class);
		if (annotation == null) {
			return null;
		}

		String returnString = null;

		String[] parameterNames = CommandCommon.discoverer.getParameterNames(classMethod);

		Class[] clazz = classMethod.getParameterTypes();


		for (int i = 0; i < parameterNames.length; i++) {
			String displayString = null;

			// First, try getting it from an enum
			StringBuilder sb = null;

			if (clazz[i].getSuperclass() != null && clazz[i].getSuperclass().getName().equals("java.lang.Enum")) {
				Enum[] enumList = (Enum[]) clazz[i].getEnumConstants();

				for (Enum enumm : enumList) {
					if (sb == null) {
						sb = new StringBuilder("<" + enumm.name());
					} else {
						sb.append("|").append(enumm.name());
					}
					sb.append(">");
				}

			}
			displayString = sb == null ? null : sb.toString();

			// if still null, try boolean
			if (displayString == null) {
				if (clazz[i].getName().equals("java.lang.Boolean")) {
					displayString = "<true|false>";
				}
			}

			// Otherwise, string time
			if (displayString == null && clazz[i].getName().equals("java.lang.String[]")) {
				displayString = "<" + parameterNames[i] + "> ...";
			}

			returnString = (returnString == null ? "" : returnString + " ");
			returnString += (displayString == null ? "<" + parameterNames[i] + ">" : displayString);
		}
		return classMethod.getName() + " " + (returnString == null ? "" : returnString);
	}


	/**
	 * Iterates through the extended and implemented classes to get the methods with any command-related annotations
	 * attached to them since they are not inherited in the implementing class
	 *
	 * @param clazz     The class to scan for methods
	 * @param methodMap A methodname:method map of the annotated methods
	 */
	public void gatherClassCommandMethods(@NotNull Class clazz, Map<String, Method> methodMap) {

		Method[] methods = clazz.getMethods();
		for (Method m : methods) {
			Command c = m.getAnnotation(Command.class);
			if (c != null && !methodMap.containsKey(m.getName()) &&
					(showDevCommands || !c.isDev())) {
				methodMap.put(m.getName(), m);
			}
		}

		Class[] interfaces = clazz.getInterfaces();
		if (interfaces != null && interfaces.length > 0) {
			for (Class i : interfaces) {
				gatherClassCommandMethods(i, methodMap);
			}
		}

		Class superclass = clazz.getSuperclass();
		if (superclass != null && superclass != Object.class) {
			gatherClassCommandMethods(superclass, methodMap);
		}
	}

	/**
	 * Parses the input from the command line and arranges them so that they that can be supplied to the method
	 * indicated by the first parameter
	 */
	public void parseArguments(String[] args) throws
			NoSuchCommandException, NoSuchCommandArgumentException, InvalidArgumentValueException {

		// Extract the method name and remove it from the array
		String methodName = args[0];
		args = Arrays.copyOfRange(args, 1, args.length);

		Method commandMethod;

		// Get the Command methods
		Map<String, Method> methodMap = new HashMap<>();


//    public static <T extends Annotation> void gatherAnnotatedMethods(@NotNull Class clazz, Class<T> annotationType, Map<String, Method> methodMap) {
//        CommandTools.gatherAnnotatedMethods(clazz, Command.class, methodMap);
//        for (String key : methodMap) {
//            if methodMap[key].
//        }
//        

		if (methodMap.containsKey(methodName)) {
			// If the provided method name matches one, we have a match
			commandMethod = methodMap.get(methodName);

		} else {
			// Otherwise raise an exception
			throw new NoSuchCommandException(methodName);
		}

		if (args.length == 0) {
			orderedArgumentList = new String[0];
		}

		Method[] classMethods = clazz.getMethods();

		// Get the proper method in the actual class
		for (Method m : classMethods) {
			if (m.getName().equals(methodName)) {
				classMethod = m;
				break;
			}
		}

		if (args.length > 0) {
			Class[] parameterClasses = classMethod.getParameterTypes();
			Object[] parameterArray = new Object[parameterClasses.length];

			for (int i = 0; i < parameterClasses.length; i++) {
				String typeName = parameterClasses[i].getName();
				if (typeName.endsWith("[]")) {
					Object[] obj = Arrays.copyOfRange(args, i, args.length);
					parameterArray[i] = obj;

				} else if (typeName.equals("java.lang.String")) {
					parameterArray[i] = args[i];
				} else if (typeName.equals("java.lang.Boolean")) {
					parameterArray[i] = Boolean.valueOf(args[i]);
				} else if (typeName.equals("java.lang.Integer")) {
					parameterArray[i] = Integer.valueOf(args[i]);
				} else if (parameterClasses[i].getSuperclass().getName().equals("java.lang.Enum")) {
					Enum[] enumList = (Enum[]) parameterClasses[i].getEnumConstants();
					boolean parameterSet = false;
					for (Enum enumm : enumList) {
						if (enumm.name().equals(args[i])) {
							parameterArray[i] = enumm;
							parameterSet = true;
						}
					}

					if (!parameterSet) {
						throw new InvalidArgumentValueException(clazz.getName(),
								CommandCommon.discoverer.getParameterNames(classMethod)[i], args[i]);
					}

				} else {
					throw new RuntimeException("Unexpected Class type \"" + parameterClasses[i].getName() +
							"\" used in CLI-compatible method!");
				}
			}
			orderedArgumentList = parameterArray;
		}
	}

	public void execute() {
		CommandCommon.invokeMethodFromModuleWithParams(classMethod, commandClassInstance, orderedArgumentList);
//        public static String invokeMethodFromModuleWithParams(@NotNull Method method, @NotNull AbstractAppModule module, @Nullable String[] params) { 
	}

	/**
	 * Prints the usage for the class to the command line
	 */
	public void printUsage() {
		// Get the class
//        Class clazz = module.getClass();

		// Get the methods annotated (either by the class itself, an abstract class, or an interface)
		Map<String, Method> mMap = new HashMap<>();
		gatherClassCommandMethods(clazz, mMap);
		Method[] commandMethods = mMap.values().toArray(new Method[0]);

		Map<Method, Method> useMethods = new HashMap<>();

		// If the name/param signature of the class method matches the signature of one of the command methods, use it
		for (Method commandMethod : commandMethods) {
			classMethodLoop:
			for (Method classMethod : clazz.getMethods()) {
				if (commandMethod.getName().equals(classMethod.getName())) {
					Parameter[] cdParams = Parameter.fromMethod(commandMethod); // commandMethod.getParameters();
					Parameter[] csParams = Parameter.fromMethod(classMethod); //classMethod.getParameters();

					if (cdParams.length != csParams.length) {
						break;
					}

					for (int i = 0; i < cdParams.length; i++) {
						if (cdParams[i].type != csParams[i].type) {
							break classMethodLoop;
						}
					}
					useMethods.put(classMethod, commandMethod);
				}
			}
		}

		// Best to display help sorted for usability...
		SortedMap<String, Method> sortedMethods = new TreeMap<>();
		for (Method m : useMethods.keySet()) {
			sortedMethods.put(m.getName(), m);
		}

		output.println("Commands:\n");

		for (String methodName : sortedMethods.keySet()) {
			Method method = sortedMethods.get(methodName);

			if (!useMethods.get(method).getAnnotation(Command.class).isDev() || showDevCommands) {
				output.println("    " + getDisplayableMethod(method, useMethods.get(method)));
				output.println("        " + useMethods.get(method).getAnnotation(Command.class).description());
				output.println();
			}
		}
	}

}
