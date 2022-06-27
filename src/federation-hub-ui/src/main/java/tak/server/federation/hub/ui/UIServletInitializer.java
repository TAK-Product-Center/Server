package tak.server.federation.hub.ui;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;
import java.io.File;

public class UIServletInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
    private int maxUploadSize = 5 * 1024 * 1024; // 5 MB.

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return null;
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[] { WebMvcConfig.class, SSLConfig.class, AuthenticationConfig.class };
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        // Uploaded temp file will be put here.
        File uploadDirectory = new File(System.getProperty("java.io.tmpdir"));

        // Register a MultipartConfigElement.
        MultipartConfigElement multipartConfigElement =
            new MultipartConfigElement(uploadDirectory.getAbsolutePath(),
                maxUploadSize, maxUploadSize * 2, maxUploadSize / 2);

        registration.setMultipartConfig(multipartConfigElement);
    }
}
