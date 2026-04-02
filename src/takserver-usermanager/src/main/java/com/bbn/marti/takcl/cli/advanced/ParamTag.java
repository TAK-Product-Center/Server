package com.bbn.marti.takcl.cli.advanced;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created on 8/18/17.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ParamTag {
	/**
	 * @return A description of the parameter
	 */
	String description();

	/**
	 * @return A long specifier for a parameter (such as "--add-user")
	 */
	String longSpecifier() default "";

	/**
	 * @return A long specifier for a parameter (such as "-a")
	 */
	String shortSpecifier() default "";

	/**
	 * Indicates this parameter has no value.
	 * <p>
	 * If it is present, true is assumed. Otherwise, false should be assumed
	 *
	 * @return If it is a toggle parameter
	 */
	boolean isToggle() default false;

	/**
	 * @return Whether or not this parameter is optional
	 */
	boolean optional();

    boolean allowedMultiple() default false;
}
