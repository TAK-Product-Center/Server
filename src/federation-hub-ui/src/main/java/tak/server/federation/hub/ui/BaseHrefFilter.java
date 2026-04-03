package tak.server.federation.hub.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.ResourceLoader;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class BaseHrefFilter implements Filter {

	private final String baseHref;
	    
    // Pattern to find/replace <base href="...">
    private static final Pattern BASE_HREF_PATTERN = Pattern.compile(
            "(?i)<base\\s+[^>]*href\\s*=\\s*['\"][^'\"]*['\"][^>]*>",
            Pattern.CASE_INSENSITIVE
    );

    public BaseHrefFilter(FederationHubUIConfig fedHubConfig) {
        String path = fedHubConfig.getBaseHref();
        this.baseHref = path.endsWith("/") ? path : path + "/";
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI().substring(request.getContextPath().length());
               
        if (shouldServeIndex(path)) {
            ServletContext servletContext = ((HttpServletRequest) req).getServletContext();
            String indexPath = "/index.html";

            try (InputStream is = servletContext.getResourceAsStream(indexPath)) {
                if (is == null) {
                    System.out.println("index.html not found in webapp root");
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                StringBuilder html = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        html.append(line).append("\n");
                    }
                }

                String modifiedHtml = html.toString();

                Matcher matcher = BASE_HREF_PATTERN.matcher(modifiedHtml);
                if (matcher.find()) {
                    modifiedHtml = matcher.replaceFirst("<base href=\"" + baseHref + "\">");
                } else {
                    modifiedHtml = modifiedHtml.replaceFirst("(?i)(<head[^>]*>)", "$1\n  <base href=\"" + baseHref + "\">");
                }

                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write(modifiedHtml);
                return;
            } catch (IOException e) {
                System.err.println("Error loading index.html: " + e);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        }

        chain.doFilter(req, res);
    }

    private boolean shouldServeIndex(String path) {
    	// Allow API routes
        if (path.startsWith("/api/") || path.startsWith("/actuator/") || path.startsWith("/ws/")) {
            return false;
        }
        // Allow static files through
        if (path.contains(".")) {
            return false;
        }
        // SPA routes only
        return path.equals("/") || path.equals("/index.html") || path.startsWith("/");
    }

    @Override
    public void init(FilterConfig filterConfig) {}
    @Override
    public void destroy() {}
}
