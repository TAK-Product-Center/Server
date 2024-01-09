package com.bbn.marti.takcl.cli.advanced;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created on 8/18/17.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface ParameterizedCommand {
	boolean isDev() default false;

	String description() default "";
}
