package tak.server.config.websocket;

import java.security.cert.X509Certificate;

import org.springframework.security.core.userdetails.UserDetails;

/*
 * 
 * Extend the UserDetails interface so that there is a method for accessing the client cert
 * 
 */
public interface MartiSocketUserDetails extends UserDetails {

    X509Certificate getCert();

}
