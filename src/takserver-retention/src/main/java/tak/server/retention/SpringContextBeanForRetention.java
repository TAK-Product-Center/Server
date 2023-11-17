package tak.server.retention;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;


public class SpringContextBeanForRetention implements ApplicationContextAware {
    private static ApplicationContext springContext;

    public static ApplicationContext getSpringContext() {
        return springContext;
    }

    @SuppressWarnings("static-access")
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.springContext = context;
    }
}