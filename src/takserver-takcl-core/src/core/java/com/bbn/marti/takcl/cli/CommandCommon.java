package com.bbn.marti.takcl.cli;

import com.bbn.marti.takcl.AppModules.generic.AppModuleInterface;
import com.bbn.marti.takcl.AppModules.generic.BaseAppModuleInterface;
import com.bbn.marti.takcl.AppModules.generic.ServerAppModuleInterface;
import com.bbn.marti.takcl.cli.advanced.Parameter;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.DefaultParameterNameDiscoverer;

import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * Created on 8/18/17.
 */
public class CommandCommon {
	public static final DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

	public static PrintStream output = System.err;

	public static String invokeMethodFromModuleWithParams(
			@NotNull Method method, @NotNull BaseAppModuleInterface module, @Nullable Object[] params) throws
			EndUserReadableException, InvalidArgumentValueException, NoSuchCommandArgumentException, NoSuchCommandException {

		Class<?>[] paramTypes = method.getParameterTypes();

		try {
			if (module instanceof ServerAppModuleInterface) {
				ServerAppModuleInterface serverAppModule = (ServerAppModuleInterface) module;
				// TODO: This should probably be handled better...
				serverAppModule.init(ImmutableServerProfiles.DEFAULT_LOCAL_SERVER);
			} else {
				AppModuleInterface appModule = (AppModuleInterface) module;
				appModule.init();
			}

			Object o;

			if (params == null) {
				o = method.invoke(module);
			} else if (params.length < paramTypes.length) {
				throw new EndUserReadableException("Not all arguments were provided!");
			} else {
				o = method.invoke(module, params);
			}

			if (o instanceof String) {
				return (String) o;

			} else if (o instanceof Boolean) {
				Boolean val = (Boolean) o;
				return (val ? "true" : "false");

			} else if (o instanceof Enum) {
				return o.toString();

			} else if (o == null) {
				return "";

			} else {
				throw new RuntimeException("Unexpected return value from method!");
			}

		} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof EndUserReadableException) {
				throw (EndUserReadableException) cause;
			} else if (cause instanceof InvalidArgumentValueException) {
				throw (InvalidArgumentValueException) cause;

			} else if (cause instanceof NoSuchCommandArgumentException) {
				throw (NoSuchCommandArgumentException) cause;

			} else if (cause instanceof NoSuchCommandException) {
				throw (NoSuchCommandException) cause;
			} else {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Iterates through the extended and implemented classes to get the methods with any annotations attached to them
	 * of the provided type since they are not inherited in the implementing class
	 *
	 * @param clazz          The class to scan for methods
	 * @param annotationType Tye type of annotation to extract
	 * @param methodMap      A methodname:method map of the annotated methods
	 */
	public static <T extends Annotation> void gatherAnnotatedMethods(@NotNull Class<?> clazz, Class<T> annotationType, Map<String, Method> methodMap) {

		Method[] methods = clazz.getMethods();
		for (Method m : methods) {
			T c = m.getAnnotation(annotationType);
			if (c != null && !methodMap.containsKey(m.getName())) {
				methodMap.put(m.getName(), m);
			}
		}

		Class<?>[] interfaces = clazz.getInterfaces();
		if (interfaces.length > 0) {
			for (Class<?> i : interfaces) {
				gatherAnnotatedMethods(i, annotationType, methodMap);
			}
		}

		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null && superclass != Object.class) {
			gatherAnnotatedMethods(superclass, annotationType, methodMap);
		}
	}

	public static Object parseInnerValue(Parameter param, String value) throws InvalidArgumentValueException {
		Object rval = null;
		String typeName = param.type.getName();
		if (typeName.endsWith("[]")) {
			typeName = typeName.substring(0, typeName.length() - 2);
		} else if (typeName.startsWith("[L") && typeName.endsWith(";")) {
			typeName = typeName.substring(2, typeName.length() - 1);
		}

		if (typeName.equals("java.lang.String")) {
			rval = value;
		} else if (typeName.equals("java.lang.Boolean")) {
			rval = Boolean.valueOf(value);
		} else if (typeName.equals("java.lang.Integer")) {
			rval = Integer.valueOf(value);
		} else if (param.type.getSuperclass().getName().equals("java.lang.Enum")) {
			Enum<?>[] enumList = (Enum<?>[]) param.type.getEnumConstants();
			boolean parameterSet = false;
			for (Enum<?> enumm : enumList) {
				if (enumm.name().equals(value)) {
					rval = enumm;
					parameterSet = true;
				}
			}

			if (!parameterSet) {
				throw new InvalidArgumentValueException(param.parentClass.getName(), param.name, value);
			}

		} else {
			throw new RuntimeException("Unexpected Class type \"" + param.type.getName() +
					"\" used in CLI-compatible method!");
		}
		return rval;
	}
}

