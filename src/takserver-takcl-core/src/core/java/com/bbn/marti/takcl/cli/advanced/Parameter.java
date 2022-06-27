package com.bbn.marti.takcl.cli.advanced;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created on 8/18/17.
 */
public class Parameter {
	public final String name;
	final ParamTag paramTag;
	public final Class<?> type;
	private Object value;
	public final Class<?> parentClass;

	Parameter(@NotNull Class<?> parentClass, @NotNull Class<?> type, @NotNull String name, @Nullable ParamTag paramTag) {
		this.type = type;
		this.name = name;
		this.paramTag = paramTag;
		this.parentClass = parentClass;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	void clearValue() {
		this.value = null;
	}

	public Object getValue() {
		return value;
	}
}
