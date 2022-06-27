package com.bbn.marti.takcl;

//import com.bbn.marti.takcl.AppModules.generic.AppModuleInterface;

import com.bbn.marti.takcl.AppModules.generic.BaseAppModuleInterface;
import com.bbn.marti.takcl.cli.simple.Command;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.DefaultParameterNameDiscoverer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Used to autogenerate a bash completion script
 * Created on 11/3/15.
 */
public class BashCompletionHelper {

	// Unlikely and doesn't require a shift to autocomplete
	private static final String PARAM_START = "[";
	private static final String PARAM_END = "]";
	private static final String PARAM_SEPARATOR = ",";
//    private static final String PARAM_PRECURSOR = "With,Param=";

	private static final DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

	public static String getBashableMethodLabel(Method method) {
		Annotation annotation = method.getAnnotation(Command.class);
		if (annotation == null) {
			return null;
		}

		String returnString = null;

		String[] parameterNames = discoverer.getParameterNames(method);
		Class[] clazz = method.getParameterTypes();

		if (parameterNames != null && parameterNames.length > 0) {
			returnString = "[";

			for (int i = 0; i < parameterNames.length; i++) {
				returnString += parameterNames[i];

				if (i + 1 < parameterNames.length) {
					returnString += ",";
				}
			}
			returnString += "]";
		}

		return method.getName() + (returnString == null ? "" : returnString);
	}

	@NotNull
	public static String[] getBashableParam(@NotNull Method method, int paramIdx) {
		String[] parameterArray;

		Class[] parameterClasses = method.getParameterTypes();
		if (parameterClasses == null || parameterClasses.length <= paramIdx) {
			return new String[0];
		}

		Class parameterClass = parameterClasses[paramIdx];

		if (parameterClass.getName().equals("java.lang.Boolean")) {
			parameterArray = new String[]{"true", "false"};

		} else if (parameterClass.getSuperclass().getName().equals("java.lang.Enum")) {
			Enum[] enumList = (Enum[]) parameterClass.getEnumConstants();
			parameterArray = new String[enumList.length];

			for (int i = 0; i < enumList.length; i++) {
				parameterArray[i] = enumList[i].name();
			}

		} else if (parameterClass.getName().equals("java.lang.Integer") || parameterClass.getName().equals("java.lang.String") || parameterClass.getName().equals("[Ljava.lang.String;")) {
			return new String[0];

		} else {
			throw new RuntimeException("Unexpected Class type \"" + parameterClass.getName() +
					"\" used in CLI-compatible method!");
		}

		return parameterArray;

	}


	@NotNull
	public static String generateBashCompletionFileContents(@NotNull Map<String, BaseAppModuleInterface> moduleMap, @Nullable String takclLocation) {
		String outputString = "#!/bin/bash\n\n_takcl()\n{\n\n";

		String[] level1CompletionStrings = generateModuleIdentifierCompletionLines(moduleMap);
		for (String str : level1CompletionStrings) {
			outputString += ("\n\t" + str);
		}

		List<String> functionCompletionLines = generateModuleFunctionCompletionLines(moduleMap);

		for (String str : functionCompletionLines) {
			outputString += ("\n" + str);
		}

		List<String> parameterCompletionLines = generateParameterCompletionLines(moduleMap);


		for (String str : parameterCompletionLines) {
			outputString += ("\n\t" + str);
		}

		if (takclLocation != null) {
			outputString += "\n\nalias takcl=\"java -jar " + takclLocation + "\"";
		}

		outputString += "\n\tfi\n}\n\ncomplete -F _takcl takcl";
		return outputString;
	}

	public static String[] generateModuleIdentifierCompletionLines(Map<String, BaseAppModuleInterface> moduleMap) {
		String level1CompletionListString = null;

		for (String str : moduleMap.keySet()) {
			if (level1CompletionListString == null) {
				level1CompletionListString = str;
			} else {
				level1CompletionListString += (" " + str);
			}
		}

		return new String[]{
				"if [ $COMP_CWORD == 1 ];then",
				"\tlocal cur=${COMP_WORDS[COMP_CWORD]}",
				"\tCOMPREPLY=( $(compgen -W \"" + level1CompletionListString + "\" -- $cur ) )",
		};
	}

	@NotNull
	private static String[] generateFunctionSelectionStatements(@NotNull String className, @NotNull List<String> methods) {

		String methodListString = null;
		for (String method : methods) {
			if (methodListString == null) {
				methodListString = method;
			} else {
				methodListString += (" " + method);
			}
		}

		String line0 = "if [ \"${COMP_WORDS[1]}\" == \"" + className + "\" ];then";
		String line1 = "\tlocal cur=${COMP_WORDS[COMP_CWORD]}";
		String line2 = "\tCOMPREPLY=( $(compgen -W \"" + methodListString + "\" -- $cur) )";
		return new String[]{line0, line1, line2};
	}

	@NotNull
	private static List<String> generateModuleFunctionCompletionLines(@NotNull Map<String, BaseAppModuleInterface> moduleMap) {
		List<String> level2CompletionLines = new LinkedList<>();

		level2CompletionLines.add("\n\telif [ $COMP_CWORD == 2 ];then");

		boolean firstClass = true;

		for (String moduleIdentifier : moduleMap.keySet()) {
			List<String> level2CompletionList = new LinkedList();

			BaseAppModuleInterface module = moduleMap.get(moduleIdentifier);
			Method[] methods = module.getClass().getDeclaredMethods();

			for (Method method : methods) {
				if (method.getAnnotation(Command.class) != null) {
					String bashableMethodName = getBashableMethodLabel(method);
					level2CompletionList.add(bashableMethodName);
				}
			}

			String[] subLines = generateFunctionSelectionStatements(moduleIdentifier, level2CompletionList);

			for (int i = 0; i < subLines.length; i++) {
				if (i == 0) {
					if (firstClass) {
						level2CompletionLines.add("\t\t" + subLines[i]);
						firstClass = false;
					} else {
						level2CompletionLines.add("\t\tel" + subLines[i]);
					}
				} else {
					level2CompletionLines.add("\t\t" + subLines[i]);
				}
			}
		}

		level2CompletionLines.add("\t\tfi");

		return level2CompletionLines;
	}

	@NotNull
	private static List<String> generateParameterCompletionLines(@NotNull Map<String, BaseAppModuleInterface> moduleMap) {

		final int parameterIndexOffset = 3;

		// A multilevel map to aid in processing of the parameter data to bash autocompletion statements
		//  Map<parameterIndex>,
		//      Map<autocompletionModuleIdentifier,
		//          Map<autocompletionFunctionIdentifier,
		//              possibleParameterValues
		Map<Integer, Map<String, Map<String, String[]>>> parameterIndexMap = new HashMap<>();

		Iterator<String> moduleMapKeyIterator = moduleMap.keySet().iterator();

		while (moduleMapKeyIterator.hasNext()) {
			String moduleIdentifier = moduleMapKeyIterator.next();

			BaseAppModuleInterface module = moduleMap.get(moduleIdentifier);

			Method[] methods = module.getClass().getDeclaredMethods();

			for (Method method : methods) {
				if (method.getAnnotation(Command.class) != null) {
					String functionAutocompletionIdentifier = getBashableMethodLabel(method);

					int paramIdx = 0;
					String[] bashableParams;

					while ((bashableParams = getBashableParam(method, paramIdx)).length > 0) {

						Map<String, Map<String, String[]>> currentIndexMap = parameterIndexMap.get(paramIdx);
						if (currentIndexMap == null) {
							currentIndexMap = new HashMap();
							parameterIndexMap.put(paramIdx, currentIndexMap);
						}

						Map<String, String[]> currentModuleMap = currentIndexMap.get(moduleIdentifier);
						if (currentModuleMap == null) {
							currentModuleMap = new HashMap<>();
							currentIndexMap.put(moduleIdentifier, currentModuleMap);
						}
						currentModuleMap.put(functionAutocompletionIdentifier, bashableParams);

						paramIdx++;
					}
				}
			}
		}

		List<String> parameterCompletionLines = new LinkedList<>();
		for (int parameterIndex : parameterIndexMap.keySet()) {
			boolean firstModuleStatement = true;
			boolean firstFunctionStatement = true;

			parameterCompletionLines.add("elif [ $COMP_CWORD == " + String.valueOf(parameterIndexOffset + parameterIndex) + " ];then");

			for (String moduleIdentifier : parameterIndexMap.get(parameterIndex).keySet()) {


				parameterCompletionLines.add((firstModuleStatement ? "\tif" : "\telif") + " [ \"${COMP_WORDS[1]}\" == \"" + moduleIdentifier + "\" ];then");
				firstModuleStatement = false;

				firstFunctionStatement = true;
				for (String functionIdentifier : parameterIndexMap.get(parameterIndex).get(moduleIdentifier).keySet()) {
					parameterCompletionLines.add((firstFunctionStatement ? "\t\tif" : "\t\telif") + " [ \"${COMP_WORDS[2]}\" == \"" + functionIdentifier + "\" ];then");
					firstFunctionStatement = false;

					parameterCompletionLines.add("\t\t\tlocal cur=${COMP_WORDS[COMP_CWORD]}");

					String paramListString = null;
					for (String str : parameterIndexMap.get(parameterIndex).get(moduleIdentifier).get(functionIdentifier)) {
						if (paramListString == null) {
							paramListString = str;
						} else {
							paramListString += (" " + str);
						}
					}
					parameterCompletionLines.add("\t\t\tCOMPREPLY=( $(compgen -W \"" + paramListString + "\" -- $cur) )");

				}
				parameterCompletionLines.add("\t\tfi");

			}
			parameterCompletionLines.add("\tfi");

		}

		return parameterCompletionLines;
	}
}
