package tak.server;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import com.bbn.marti.remote.config.CoreConfigFacade;

// Checks CoreConfig to see if cloudwatch is enabled. Used to control bean creation for cloudwatch beans.
public class IsCloudWatchCondition implements Condition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		return CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().isCloudwatchEnable();
	}

}
