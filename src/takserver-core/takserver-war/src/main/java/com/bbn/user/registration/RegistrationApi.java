package com.bbn.user.registration;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.hibernate.validator.constraints.Email;
import org.jetbrains.annotations.NotNull;
import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.config.Whitelist;
import com.bbn.security.web.MartiValidator;
import com.bbn.security.web.MartiValidatorConstants;
import com.bbn.user.registration.model.TAKUser;
import com.bbn.user.registration.repository.TAKUserRepository;
import com.bbn.user.registration.service.UserRegistrationService;

@Validated
@RestController
public class RegistrationApi {

    Logger logger = LoggerFactory.getLogger(RegistrationApi.class);

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    @Autowired
    private UserRegistrationService userManagementService;

    @Autowired
    private TAKUserRepository takUserRepository;

    @Autowired
    private Validator validator = new MartiValidator();


    @RequestMapping(value = "/register/user", method = RequestMethod.POST)
    public void signUp(
            @Email @RequestParam(value = "emailAddress", required = true) String emailAddress,
            @RequestParam(value = "token") String token) throws ValidationException {

        try {
            // validate input
            validator.getValidInput("RegistrationApi", token,
                    MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, true);
            validator.getValidInput("RegistrationApi", emailAddress,
            		MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, false);

            String[] groupNames = null;
            com.bbn.marti.config.Email email = userManagementService.getEmailConfig();
            if (email != null && token != null && token.length() != 0) {
                for (Whitelist whitelist : email.getWhitelist()) {
                    if (whitelist.getToken() != null && whitelist.getToken().compareTo(token) == 0) {
                        groupNames = new String[]{whitelist.getDomain()};
                        break;
                    }
                }
            }

            boolean status = userManagementService.addUser(
                    emailAddress, request.getServerName(), request.getServerPort(), groupNames, false);
            response.setStatus(status ? HttpServletResponse.SC_OK : HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        } catch (Exception e) {
            logger.error("exception in signUp!", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/register/token/{token:.+}", method = RequestMethod.GET)
    public void confirm(@PathVariable("token") @NotNull String token) throws ValidationException {

        try {
            String firstName = request.getParameter("firstname");
            String lastName = request.getParameter("lastname");
            String phoneNumber = request.getParameter("phonenumber");
            String organization = request.getParameter("organization");

            // validate inputs
            validator.getValidInput("RegistrationApi", token,
            		MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, false);
            validator.getValidInput("RegistrationApi", firstName,
            		MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, false);
            validator.getValidInput("RegistrationApi", lastName,
            		MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, false);
            validator.getValidInput("RegistrationApi", phoneNumber,
            		MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, false);
            validator.getValidInput("RegistrationApi", organization,
            		MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, false);

            boolean status = userManagementService.activateUser(
                    token, firstName, lastName, phoneNumber, organization, "",
                    request.getServerName(), request.getServerPort());

            response.setStatus(status ? HttpServletResponse.SC_OK : HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        } catch (Exception e) {
            logger.error("exception in confirm!", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/register/admin/users", method = RequestMethod.GET)
    public List<TAKUser> getAllUsers() {
        return takUserRepository.findAll();
    }

    @RequestMapping(value = "/register/admin/invite", method = RequestMethod.POST)
    public void invite(
            @Email @RequestParam(value = "emailAddress", required = true) String emailAddress,
            @RequestParam(value = "group", defaultValue = "__ANON__") String[] groupNames) throws ValidationException {

        // validate input
        validator.getValidInput("RegistrationApi", emailAddress,
        		MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, false);
        for (String group : groupNames) {
            validator.getValidInput("RegistrationApi", group,
            		MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, false);
        }

        boolean status = userManagementService.inviteUser(emailAddress,
                request.getServerName(), groupNames);

        response.setStatus(status ? HttpServletResponse.SC_OK : HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
