package com.bbn.marti.takcl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class TAKCLogging {
	private static final String LOGGING_PREFIX = "logging.level.";

	private static final ConcurrentSkipListMap<String, String> logLevels = new ConcurrentSkipListMap<>();

	private static BiFunction<Class<?>, String, Logger> classLoggerBuilder;
	private static BiFunction<String, String, Logger> stringLoggerBuilder;

	static {
		Map<String, String> logLevelMap = System.getProperties().stringPropertyNames().stream()
				.filter(x -> x.startsWith(LOGGING_PREFIX)).collect(Collectors.toMap(
						x -> x.replace(LOGGING_PREFIX, ""), x -> x
				));
		if (logLevelMap.size() == 0) {
			disableAccessWarnings();
			disableStdout();
		}
		for (Map.Entry<String, String> entry : logLevelMap.entrySet()) {
			logLevels.put(entry.getKey(), System.getProperty(entry.getValue()));
		}
	}


	@SuppressWarnings("unchecked")
	public static void disableAccessWarnings() {
		try {
			Class unsafeClass = Class.forName("sun.misc.Unsafe");
			Field field = unsafeClass.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			Object unsafe = field.get(null);

			Method putObjectVolatile = unsafeClass.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class);
			Method staticFieldOffset = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class);

			Class loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
			Field loggerField = loggerClass.getDeclaredField("logger");
			Long offset = (Long) staticFieldOffset.invoke(unsafe, loggerField);
			putObjectVolatile.invoke(unsafe, loggerClass, offset, null);
		} catch (Exception ignored) {
		}
	}

	public static void disableStdout() {
		System.setOut(new java.io.PrintStream(new java.io.OutputStream() {
			@Override
			public void write(int b) {
			}
		}) {
			@Override
			public void flush() {
			}

			@Override
			public void close() {
			}

			@Override
			public void write(int b) {
			}

			@Override
			public void write(byte[] b) {
			}

			@Override
			public void write(byte[] buf, int off, int len) {
			}

			@Override
			public void print(boolean b) {
			}

			@Override
			public void print(char c) {
			}

			@Override
			public void print(int i) {
			}

			@Override
			public void print(long l) {
			}

			@Override
			public void print(float f) {
			}

			@Override
			public void print(double d) {
			}

			@Override
			public void print(char[] s) {
			}

			@Override
			public void print(String s) {
			}

			@Override
			public void print(Object obj) {
			}

			@Override
			public void println() {
			}

			@Override
			public void println(boolean x) {
			}

			@Override
			public void println(char x) {
			}

			@Override
			public void println(int x) {
			}

			@Override
			public void println(long x) {
			}

			@Override
			public void println(float x) {
			}

			@Override
			public void println(double x) {
			}

			@Override
			public void println(char[] x) {
			}

			@Override
			public void println(String x) {
			}

			@Override
			public void println(Object x) {
			}

			@Override
			public java.io.PrintStream printf(String format, Object... args) {
				return this;
			}

			@Override
			public java.io.PrintStream printf(java.util.Locale l, String format, Object... args) {
				return this;
			}

			@Override
			public java.io.PrintStream format(String format, Object... args) {
				return this;
			}

			@Override
			public java.io.PrintStream format(java.util.Locale l, String format, Object... args) {
				return this;
			}

			@Override
			public java.io.PrintStream append(CharSequence csq) {
				return this;
			}

			@Override
			public java.io.PrintStream append(CharSequence csq, int start, int end) {
				return this;
			}

			@Override
			public java.io.PrintStream append(char c) {
				return this;
			}
		});
	}

	public static void setClassLoggerBuilder(BiFunction<Class<?>, String, Logger> loggerBuilder, Set<String> validLevels) {
		for (String value : new HashSet<>(logLevels.values())) {
			if (!validLevels.contains(value)) {
				throw new RuntimeException("Invalid logging level '" + value + "' provided in logging parameter!");
			}
		}
		TAKCLogging.classLoggerBuilder = loggerBuilder;
	}

	public static void setStringLoggerBuilder(BiFunction<String, String, Logger> loggerBuilder, Set<String> validLevels) {
		for (String value : new HashSet<>(logLevels.values())) {
			if (!validLevels.contains(value)) {
				throw new RuntimeException("Invalid logging level '" + value + "' provided in logging parameter!");
			}
		}
		TAKCLogging.stringLoggerBuilder = loggerBuilder;
	}

	public synchronized static Logger getLogger(Class<?> clazz) {
		for (String packagePrefix : logLevels.descendingKeySet()) {
			if (clazz.getCanonicalName().startsWith(packagePrefix)) {
				if (classLoggerBuilder == null) {
					System.err.println("No logger set for executable! Custom log level for class '" + clazz.getCanonicalName() + "' will be ignored!");
					return LoggerFactory.getLogger(clazz);
				} else {
					return classLoggerBuilder.apply(clazz, logLevels.get(packagePrefix));
				}
			}
		}
		if (classLoggerBuilder == null) {
			return LoggerFactory.getLogger(clazz);
		} else {
			return classLoggerBuilder.apply(clazz, null);
		}
	}

	public synchronized static Logger getLogger(String classOrPackage) {
		for (String packagePrefix : logLevels.descendingKeySet()) {
			if (classOrPackage.startsWith(packagePrefix)) {
				if (stringLoggerBuilder == null) {
					System.err.println("No logger set for executable! Custom log level for class '" + classOrPackage + "' will be ignored!");
					return LoggerFactory.getLogger(classOrPackage);
				} else {
					return stringLoggerBuilder.apply(classOrPackage, logLevels.get(packagePrefix));
				}
			}
		}
		if (stringLoggerBuilder == null) {
			return LoggerFactory.getLogger(classOrPackage);
		} else {
			return stringLoggerBuilder.apply(classOrPackage, null);
		}
	}

	public static void main(String[] args) {
		getLogger(ConcurrentSkipListMap.class);
		getLogger(com.bbn.marti.takcl.AppModules.TAKCLConfigModule.class);
		getLogger(com.bbn.marti.takcl.TakclIgniteHelper.class);
		getLogger(org.apache.ignite.Ignite.class);
	}
}
