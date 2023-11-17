package tak.server.federation.hub.ui.jwt;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

  @Autowired PasswordEncoder passwordEncoder;

  
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
	  // to be used for plain username + password jwt login. will have to pull the pw for a DB or file
	  return new User(username, passwordEncoder.encode(null), new ArrayList<GrantedAuthority>());
  }
}