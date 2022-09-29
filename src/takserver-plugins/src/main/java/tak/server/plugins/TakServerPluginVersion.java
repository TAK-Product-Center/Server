package tak.server.plugins;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface TakServerPluginVersion {
	int major() default -1;
	int minor() default -1;
	int patch() default -1;
	String commitHash() default "";
	String tag() default "";
}