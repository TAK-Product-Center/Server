

package com.bbn.marti.remote.groups;

public interface AuthCallback
{
  void authenticationReturned(User user, AuthStatus result);
}
