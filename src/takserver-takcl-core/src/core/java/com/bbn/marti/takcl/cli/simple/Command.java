package com.bbn.marti.takcl.cli.simple;

import java.lang.annotation.*;

/**
 * Created on 8/18/17.
 */ // Annotation used to mark a method as something that should show up as a command
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Command {
	boolean isCommand() default true;

	boolean isDev() default false;

	String description() default "";
}
