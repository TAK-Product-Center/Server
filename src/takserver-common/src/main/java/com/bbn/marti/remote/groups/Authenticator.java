

package com.bbn.marti.remote.groups;



/* 
 * Generic authentication interface.
 * 
 * 
 */
public interface Authenticator<T> {
    
    /*
     * authenticate aysnchronously
     */
    void authenticateAsync(final T t, final AuthCallback cb);

    /*
     * authenticate synchonously with callback
     */
    void authenticate(T t, AuthCallback cb);

    /*
     * authenticate synchronously, returning a result
     */
    AuthResult authenticate(User user);

}
