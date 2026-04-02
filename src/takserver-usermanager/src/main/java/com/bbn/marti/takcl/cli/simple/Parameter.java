package com.bbn.marti.takcl.cli.simple;

import com.bbn.marti.takcl.cli.CommandCommon;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Created on 8/18/17.
 */
public class Parameter {
	static Parameter[] fromMethod(Method m) {
		Class<?>[] paramTypes = m.getParameterTypes();
		Annotation[][] paramAnnotations = m.getParameterAnnotations();
		String[] paramNames = CommandCommon.discoverer.getParameterNames(m);

		if (paramTypes == null && paramAnnotations == null) {
			return new Parameter[0];

		} else if (paramTypes == null || paramAnnotations == null || paramTypes.length != paramAnnotations.length) {
			throw new RuntimeException("Parameter missmatch for method " + m.toString());

		} else {
			Parameter[] parameters = new Parameter[paramTypes.length];

			for (int i = 0; i < paramTypes.length; i++) {
				parameters[i] = new Parameter(paramTypes[i], paramNames[i]);
			}
			return parameters;
		}
	}

	public final String name;
	public final Class<?> type;

	private Parameter(@NotNull Class<?> type, @NotNull String name) {
		this.type = type;
		this.name = name;
	}

}
