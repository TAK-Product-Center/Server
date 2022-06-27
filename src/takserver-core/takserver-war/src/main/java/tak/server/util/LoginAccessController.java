package tak.server.util;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.InternalResourceView;

// This controller enforces role-based access to the OAuth2 login page 
@Controller
@RequestMapping(value = "/login")
public class LoginAccessController {

	@GetMapping
	@PreAuthorize("hasRole('ROLE_NO_CLIENT_CERT')")
	public ModelAndView getTestData() {
		return new ModelAndView(new InternalResourceView("/Marti/login/login.html"));
	}
}