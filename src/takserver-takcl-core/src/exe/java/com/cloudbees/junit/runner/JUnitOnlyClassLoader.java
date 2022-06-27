package com.cloudbees.junit.runner;

/**
 * Just loads JUnit classes from another classloader.
 */
public class JUnitOnlyClassLoader extends ClassLoader {
	private final ClassLoader junitLoader;

	public JUnitOnlyClassLoader(ClassLoader parent, ClassLoader junitLoader) {
		super(parent);
		this.junitLoader = junitLoader;
	}

	@Override
	protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (name.startsWith("junit.")
				|| name.startsWith("org.junit.")
				|| name.startsWith("org.hamcrest."))
			return junitLoader.loadClass(name);

		return super.loadClass(name, resolve);
	}
}
