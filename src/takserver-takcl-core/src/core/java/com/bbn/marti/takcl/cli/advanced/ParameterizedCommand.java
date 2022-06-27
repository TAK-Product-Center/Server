package com.bbn.marti.takcl.cli.advanced;

import java.lang.annotation.*;

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
