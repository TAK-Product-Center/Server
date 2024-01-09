package com.bbn.user.registration.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import jakarta.mail.internet.MimeMessage;

import com.bbn.marti.remote.config.CoreConfigFacade;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import com.bbn.marti.config.Auth;
import com.bbn.marti.config.Email;
import com.bbn.marti.config.Whitelist;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.user.registration.model.TAKUser;
import com.bbn.user.registration.repository.TAKUserRepository;
import com.google.common.base.Strings;

import tak.server.util.PasswordUtils;


public class UserRegistrationService {

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(com.bbn.user.registration.service.UserRegistrationService.class);

    @Autowired
    private TAKUserRepository takUserRepository;

    @Autowired
    private GroupManager groupManager;

    @Autowired
    private RemoteUtil remoteUtil;

    private static JavaMailSenderImpl mailSender;
    private static Email emailConfig;
    private static Auth.Ldap ldapConfig;


    public boolean addUser(String emailAddress, String serverName, int serverPort, String[] groupNames, boolean invite) {
        try {
            // make sure we have config
            if (getEmailConfig() == null || getEmailConfig().getWhitelist() == null) {
                logger.error("missing email configuration!");
                return false;
            }

			// only check the whitelist if the user was not added via an invite and not registering with a private link
            if (!invite && groupNames == null) {
                // bail if not a whitelisted domain
                boolean valid = false;
                for (Whitelist whitelist : getEmailConfig().getWhitelist()) {
                    if (emailAddress.endsWith(whitelist.getDomain())) {
                        valid = true;
                        break;
                    }
                }

                if (!valid) {
                    return false;
                }
            }

            // set username to username from email + random 5 digit number
            String username = emailAddress.substring(0, emailAddress.indexOf("@")).toLowerCase();
            username = StringUtils.substring(username, 0, 15);
            Random r = new Random(System.currentTimeMillis());
            int suffix = ((1 + r.nextInt(2)) * 10000 + r.nextInt(10000));
            username += new Integer(suffix).toString();

            // create the new user
            TAKUser takUser = new TAKUser();
            takUser.setEmailAddress(emailAddress);
            takUser.setUserName(username);

            // create a new token
            takUser.setToken(UUID.randomUUID().toString());
            takUser.setActivated(false);

            if (groupNames == null) {
                takUserRepository.create(takUser.getToken(), takUser.getUserName(), takUser.getEmailAddress(),
                        takUser.getFirstName(), takUser.getLastName(), takUser.getPhoneNumber(), takUser.getOrganization(),
                        takUser.getState(),takUser.getActivated());
            } else {
                // create groups if needed
                for (String groupName : groupNames) {
                    groupManager.hydrateGroup(new Group(groupName, Direction.IN));
                    groupManager.hydrateGroup(new Group(groupName, Direction.OUT));
                }

                Set<Group> groups = groupManager.findGroups(Arrays.asList(groupNames));

                String groupVector = remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups));
                takUserRepository.create(takUser.getToken(), takUser.getUserName(), takUser.getEmailAddress(),
                        takUser.getFirstName(), takUser.getLastName(), takUser.getPhoneNumber(), takUser.getOrganization(),
                        takUser.getState(),takUser.getActivated(), groupVector);
            }

            // create the validation email
            StringBuilder text = new StringBuilder();
            String url = "https://" + serverName;
            if (serverPort != 443) {
                url += ":" + serverPort;
            }
            url += "/register/profile.html?token=" + takUser.getToken();

            text.append("<!DOCTYPE html>");
            text.append("<html lang=\"en\"><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/></head>");

            if (invite) {
                text.append("You have been invited to join the TAK Server at " + serverName + "<br><br>");
            }

            text.append("Activate your account at ");
            text.append("<a href=\"" + url + " \">" + url + "</a><br><br>");
            text.append("After completing the activation, you will receive an email with your credentials and instructions to connect.<br><br>");
            text.append("Thanks,<br><br>");
            text.append(getEmailConfig().getSupportName());
            text.append("<br>");
            text.append(getEmailConfig().getSupportEmail());
            text.append("</html>");

            final String html = text.toString();
            final String subject = invite ? "TAK Server Invitation" : "TAK Server Account Activation";

            // send the validation email in a thread so the request can return quickly
            Thread emailThread = new Thread(new Runnable() {
                public void run() {
                    sendEmail(emailAddress, subject, html);
                }
            });

            emailThread.start();;
            return true;

        } catch (Exception e) {
            logger.error("exception in addUser!", e);
            return false;
        }
    }

    public boolean activateUser(
            String token, String firstName, String lastName, String phoneNumber, String organization, String state,
            String serverName, int serverPort) {
        try {

            // make sure we have config
            if (getEmailConfig() == null || getEmailConfig().getWhitelist() == null) {
                logger.error("missing email configuration!");
                return false;
            }

            // get the user
            TAKUser takUser = takUserRepository.findByToken(token);
            if (takUser == null) {
                logger.error("searching for unknown token!");
                return false;
            }

            Whitelist privateGroup = null;
            for (Whitelist whitelist : getEmailConfig().getWhitelist()) {
                if (whitelist.isPrivateGroup() && takUser.getEmailAddress().endsWith(whitelist.getDomain())) {
                    privateGroup = whitelist;
                    break;
                }
            }

            // mark the user as activated
            takUser.setActivated(true);

            // record their profile info
            takUser.setFirstName(firstName);
            takUser.setLastName(lastName);
            takUser.setPhoneNumber(phoneNumber);
            takUser.setOrganization(organization);
            takUser.setState(state);

            // save the user
            takUserRepository.update(takUser.getToken(), takUser.getUserName(), takUser.getEmailAddress(),
                    takUser.getFirstName(), takUser.getLastName(), takUser.getPhoneNumber(), takUser.getOrganization(),
                    takUser.getState(),takUser.getActivated(), takUser.getId());

            // create their password
            String password = PasswordUtils.generatePassword();

            // determine the user's group assignment
            String[] groupNames = null;
            if (takUser.getGroupVector() != null) {
                groupNames = RemoteUtil.getInstance().getGroupNamesForBitVectorString(
                        takUser.getGroupVector(), groupManager.getAllGroups()).toArray(new String[0]);
            } else if  (privateGroup != null) {
                String privateGroupName = !Strings.isNullOrEmpty(privateGroup.getGroup()) ? privateGroup.getGroup()
                        : privateGroup.getDomain();

                List<String> groups = new ArrayList<>();
                groups.add(privateGroupName);
                if (privateGroup.getGroups() != null) {
                    for (String groupz : privateGroup.getGroups()) {
                        groups.add(groupz);
                    }
                }
                groupNames = groups.toArray(new String[0]);

            } else {
                groupNames = new String[] { "__ANON__" };
            }

            // create their ldap user in the requested group
            groupManager.addLdapUser(takUser.getEmailAddress(), takUser.getUserName(), password, groupNames);

            // create the credentials email
            StringBuilder text = new StringBuilder();
            String host = serverName;

            String webtak = "https://" + host;
            if (serverPort != 443) {
                webtak += ":" + serverPort;
            }
            webtak += "/webtak/index.html";

            String username = takUser.getUserName();
            if (getLdapConfig() != null && getLdapConfig().isLoginWithEmail()) {
                username = takUser.getEmailAddress();
            }

            text.append("<!DOCTYPE html>");
            text.append("<html lang=\"en\"><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/></head>");
            text.append("Your account is now active!<br><br>");
            text.append("Connect to WebTAK at <a href=\"" + webtak + "\">" + webtak + "</a>.<br><br>");
            text.append("To connect with ATAK or WinTAK, add a new TAK Server connection to " + host + " and check 'Enroll For Client Certificate'.<br><br>");
            text.append("Your credentials are: <br>");
            text.append("Username: " + username + "<br>");
            text.append("Password: " + password + "<br><br>");
            text.append("Thanks,<br><br>");
            text.append(getEmailConfig().getSupportName());
            text.append("<br>");
            text.append(getEmailConfig().getSupportEmail());
            text.append("</html>");

            // send the credentials email in a thread so the request can return quickly
            Thread emailThread = new Thread(new Runnable() {
                public void run() {
                    sendEmail(takUser.getEmailAddress(), "Account Activated", text.toString());
                }
            });

            emailThread.start();
            return true;

        } catch (Exception e) {
            logger.error("exception in activateUser!", e);
            return false;
        }
    }

    public boolean inviteUser(String emailAddress, String serverName, String[] groupNames) {
        int serverPort = getEmailConfig().getRegistrationPort();
        return addUser(emailAddress, serverName, serverPort, groupNames, true);
    }

    private void sendEmail(String to, String subject, String text)  {
        try {
            JavaMailSender mailSender = getMailSender();
            if (mailSender == null) {
                logger.error("error getting mailSender!");
                return;
            }

            if (emailConfig == null) {
                logger.error("missing email configuration!");
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            message.setContent(text, "text/html");

            MimeMessageHelper helper = new MimeMessageHelper(message, false, "utf-8");
            helper.setFrom(emailConfig.getFrom());
            helper.setTo(to);
            helper.setSubject(subject);

            mailSender.send(message);
        } catch (Exception e) {
            logger.error("exception in sendValidationEmail!", e);
        }
    }

    public synchronized Email getEmailConfig() {
        if (emailConfig == null) {
            emailConfig = CoreConfigFacade.getInstance().getRemoteConfiguration().getEmail();
        }

        return emailConfig;
    }

    private synchronized Auth.Ldap getLdapConfig() {
        if (ldapConfig == null) {
            ldapConfig = CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().getLdap();
        }

        return ldapConfig;
    }

    private synchronized JavaMailSender getMailSender() {
        if (mailSender == null) {
            mailSender = new JavaMailSenderImpl();
            mailSender.setHost(getEmailConfig().getHost());
            mailSender.setPort(getEmailConfig().getPort());
            mailSender.setUsername(getEmailConfig().getUsername());
            mailSender.setPassword(getEmailConfig().getPassword());

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.debug", "true");
        }

        return mailSender;
    }
}
