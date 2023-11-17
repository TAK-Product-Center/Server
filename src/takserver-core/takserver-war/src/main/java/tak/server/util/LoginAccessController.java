package tak.server.util;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.InternalResourceView;

// This controller enforces role-based access to the OAuth2 login page
@Controller
public class LoginAccessController {

	@PreAuthorize("hasRole('ROLE_NO_CLIENT_CERT')")
	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public ModelAndView getLoginPage() {
		return new ModelAndView(new InternalResourceView("/Marti/login/index.html"));
	}
}