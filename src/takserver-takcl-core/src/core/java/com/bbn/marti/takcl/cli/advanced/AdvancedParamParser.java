package com.bbn.marti.takcl.cli.advanced;

import com.bbn.marti.takcl.AppModules.generic.BaseAppModuleInterface;
import com.bbn.marti.takcl.cli.CommandCommon;
import com.bbn.marti.takcl.cli.InvalidArgumentValueException;
import com.bbn.marti.takcl.cli.NoSuchCommandArgumentException;
import com.bbn.marti.takcl.cli.NoSuchCommandException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created on 8/25/17.
 */
public class AdvancedParamParser {

	private static final int HELP_LEFT_SIDE_LENGTH = 30;

	private final Method commandMethod;
	private final Method implementationMethod;
	private final BaseAppModuleInterface module;
	private final Parameter[] parameters;
	private final HashMap<String, Parameter> paramNameMap;
	private final HashMap<String, Parameter> argumentCliFlagMap;
	private Object[] finalParameterValues;
	private final String[] originalArgs;

	public AdvancedParamParser(BaseAppModuleInterface instance, String[] args) throws NoSuchCommandException {
		String methodName = args[0];
		module = instance;
		Class<? extends BaseAppModuleInterface> clazz = instance.getClass();

		originalArgs = Arrays.copyOfRange(args, 1, args.length);

		Method choosenMethod = null;
		Method[] methods = clazz.getMethods();
		for (Method m : methods) {
			if (m.getName().equals(methodName)) {
				choosenMethod = m;
				break;
			}
		}

		if (choosenMethod == null) {
			throw new NoSuchCommandException(methodName);
		} else {
			implementationMethod = choosenMethod;
		}

		Class<?>[] paramTypes = implementationMethod.getParameterTypes();

		Map<String, Method> classCommandMethods = new HashMap<>();
		CommandCommon.gatherAnnotatedMethods(clazz, ParameterizedCommand.class, classCommandMethods);
		commandMethod = classCommandMethods.get(methodName);

		Annotation[][] paramAnnotations = commandMethod.getParameterAnnotations();
		String[] paramNames = CommandCommon.discoverer.getParameterNames(implementationMethod);

		if (paramTypes == null && paramAnnotations == null) {
			parameters = new Parameter[0];

		} else if (paramTypes == null || paramAnnotations == null || paramTypes.length != paramAnnotations.length) {
			throw new RuntimeException("Parameter missmatch for method " + commandMethod.toString());

		} else {
			Parameter[] params = new Parameter[paramTypes.length];

			for (int i = 0; i < paramTypes.length; i++) {

				ParamTag pt = null;
				for (Annotation a : paramAnnotations[i]) {
					if (a instanceof ParamTag) {
						pt = (ParamTag) a;
						break;
					}
				}

				if (pt == null) {
					throw new RuntimeException("All parameters must be annotated!");
				} else {
					params[i] = new Parameter(clazz, paramTypes[i], paramNames[i], pt);
				}
			}
			parameters = params;
		}

		paramNameMap = new HashMap<>();
		argumentCliFlagMap = new HashMap<>();

		for (Parameter p : parameters) {
			paramNameMap.put(p.name, p);
			argumentCliFlagMap.put(p.paramTag.shortSpecifier(), p);
			argumentCliFlagMap.put(p.paramTag.longSpecifier(), p);
		}
	}

	public void processParameters() throws
			NoSuchCommandException, NoSuchCommandArgumentException, InvalidArgumentValueException {

		String[] args = Arrays.copyOf(originalArgs, originalArgs.length);

		Parameter mandatoryParam = null;

		for (Parameter p : parameters) {
			if (!p.paramTag.optional()) {
				// If there is a mandatory parameter
				if (mandatoryParam == null) {
					// And it is not null, set it
					mandatoryParam = p;
				} else {
					// Otherwise, raise an exception because only one is supported at this time.
					// But only one is supported at this time
					throw new RuntimeException("Multiple non-optional parameters are currently not supported!");
				}
			}
		}

		if (mandatoryParam != null) {
			// If there is a mandatory parameter, save the last value in the command to it (assumed to be the case)
			mandatoryParam.setValue(args[args.length - 1]);

			// And remove it from the args to be processed
			args = Arrays.copyOfRange(args, 0, args.length - 1);
		}

		Iterator<String> argIter = Arrays.asList(args).iterator();

		while (argIter.hasNext()) {
			// For each remaining argument, get the value
			String value = argIter.next();

			if (!argumentCliFlagMap.containsKey(value)) {
				// And if it doesn't match an argument specifier, throw an exception
				throw new NoSuchCommandArgumentException(commandMethod.getName(), value);
			} else {
				Parameter p = argumentCliFlagMap.get(value);

				// Otherwise, if the value is unset
				if (p.getValue() == null) {
					if (p.paramTag.isToggle()) {
						// If it is a toggle, set it true
						p.setValue(true);
					} else if (p.type.isArray()) {
						Object newValue = CommandCommon.parseInnerValue(p, argIter.next());

						if (newValue instanceof String) {
							p.setValue(new String[]{
									(String) newValue
							});

						} else {
							p.setValue(new Object[]{
									newValue
							});
						}

					} else {
						Object newValue = CommandCommon.parseInnerValue(p, argIter.next());
						p.setValue(newValue);
					}

				} else {
					// Otherwise, determine the proper value
					if (p.type.isArray()) {
						Object newValue = CommandCommon.parseInnerValue(p, argIter.next());
						Object oldValue = p.getValue();

						if (oldValue instanceof String[]) {
							String[] oldArr = (String[]) oldValue;
							String[] arr = Arrays.copyOf((String[]) oldValue, oldArr.length + 1, String[].class);
							arr[oldArr.length] = (String) newValue;
							p.clearValue();
							p.setValue(arr);

						} else if (oldValue instanceof Object[]) {

							Object[] oldArr = (Object[]) oldValue;
							Object[] arr = Arrays.copyOf((Object[]) oldValue, oldArr.length + 1);
							arr[oldArr.length] = newValue;
							p.clearValue();
							p.setValue(arr);


						} else {
							p.clearValue();
							if (oldValue instanceof String && newValue instanceof String) {
								p.setValue(new String[]{
										(String) oldValue,
										(String) newValue
								});

							} else {
								p.setValue(new Object[]{
										oldValue,
										newValue
								});
							}
						}

					} else {
						throw new RuntimeException("The " + p.paramTag.longSpecifier() + " or " +
								p.paramTag.shortSpecifier() + " argument can only be used once!");
					}
				}

			}
		}

		finalParameterValues = new Object[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			finalParameterValues[i] = parameters[i].getValue();
		}

	}

	public List<String> getDisplayableMethod() {
		// Spacing
		String sp = "  ";
		List<String> lines = new LinkedList<>();

		Parameter mandatoryParam = null;

		for (Parameter p : paramNameMap.values()) {
			if (!p.paramTag.optional()) {
				if (mandatoryParam == null) {
					mandatoryParam = p;
				} else {
					throw new RuntimeException("Multiple non-optional parameters are currently not supported!");
				}
			}
		}

		lines.add(commandMethod.getName() + " [OPTIONS] " + (mandatoryParam == null ? "" : mandatoryParam.name));
		lines.add("");

		Annotation[] mAnnotations = commandMethod.getDeclaredAnnotations();

		for (Annotation a : mAnnotations) {
			if (a instanceof ParameterizedCommand) {
				lines.add(sp + ((ParameterizedCommand) a).description());
			}
		}

		lines.add("");
		lines.add(sp + "Options:");

		List<String> tagLabels = new LinkedList<>(paramNameMap.keySet());
		Collections.sort(tagLabels);

		for (String tagLabel : tagLabels) {
			Parameter p = paramNameMap.get(tagLabel);
			ParamTag pt = p.paramTag;
			if (pt.optional()) {

				String leftPart = pt.shortSpecifier() + ", " + pt.longSpecifier();
				if (!pt.isToggle()) {
					leftPart += (" " + p.name.toUpperCase());
				}

				if (leftPart.length() >= HELP_LEFT_SIDE_LENGTH) {
					leftPart += ("\n" + padRight("", HELP_LEFT_SIDE_LENGTH));

				} else {
					leftPart = padRight(leftPart, HELP_LEFT_SIDE_LENGTH);
				}

				lines.add(sp + sp + leftPart + pt.description());
			}
		}
		return lines;
	}

	private static String padRight(String string, int length) {
		return String.format("%1$-" + length + "s", string);
	}

	public String execute() {
		return CommandCommon.invokeMethodFromModuleWithParams(implementationMethod, module, finalParameterValues);
	}
}
