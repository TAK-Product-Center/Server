package tak.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;


@Controller
public class ErrorController {

    private static final Logger logger = LoggerFactory.getLogger(ErrorController.class);

    private static String errorHtml = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
            "<html>\n" +
            "<head>\n" +
            "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n" +
            "<title>TAK Server resource unavailable or not allowed.</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "<h3>TAK Server resource unavailable or not allowed.</h3>\n" +
            "</body>\n" +
            "</html>";

    private static String error404Html = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
            "<html>\n" +
            "<head>\n" +
            "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n" +
            "<title>404 TAK Server resource not found</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "<h3>404 TAK Server resource not found</h3>\n" +
            "</body>\n" +
            "</html>";

    @RequestMapping(value = "/error")
    public ResponseEntity<String> handleError(HttpServletRequest request) {
        Integer statusCode = 500;
        String errorPage = errorHtml;

        try {
            if (request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) != null) {
                statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
                if (statusCode == HttpStatus.NOT_FOUND.value()) {
                    errorPage = error404Html;
                }
            }

            logger.error("{} error returned from {} {}, {}", statusCode, request.getMethod(),
                    (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI),
                    (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE));

        } catch (Exception e) {
            logger.error("exception in handleError", e);
        }

        return new ResponseEntity<>(errorPage, HttpStatus.valueOf(statusCode));
    }
}