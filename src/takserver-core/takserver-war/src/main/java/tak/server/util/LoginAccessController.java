package tak.server.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.InternalResourceView;

// This controller enforces role-based access to the OAuth2 login page
@Controller
public class LoginAccessController {

	@Autowired
	private HttpServletRequest request;

	@PreAuthorize("hasRole('ROLE_NO_CLIENT_CERT') and hasRole('ROLE_ALLOW_LOGIN')")
	@RequestMapping(value = "/login")
	public ModelAndView getLoginPage() {

		if (!HttpMethod.GET.matches(request.getMethod())) {
			return null;
		}

		return new ModelAndView(new InternalResourceView("/Marti/login/index.html"));
	}
}