

package com.bbn.marti.remote.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/*
 * 
 * Bean to hold a reference to the active spring context
 * 
 */
public class SpringContextBeanForApi implements ApplicationContextAware {
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
