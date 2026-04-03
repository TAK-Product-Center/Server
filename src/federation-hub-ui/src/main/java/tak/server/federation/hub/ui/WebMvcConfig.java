package tak.server.federation.hub.ui;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ComponentScan("tak.server.federation.hub.ui")
public class WebMvcConfig implements WebMvcConfigurer {
	
	private final String basePath;
	
	public WebMvcConfig(FederationHubUIConfig config) {
		basePath = config.getBaseHref();
	}

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(basePath + "**")
                .addResourceLocations("/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController(basePath + "{path:[^\\.]*}")
                .setViewName("forward:" + basePath + "index.html");

        registry.addViewController(basePath + "**/{path:[^\\.]*}")
                .setViewName("forward:" + basePath + "index.html");
    }

    
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseTrailingSlashMatch(true);
    }
}