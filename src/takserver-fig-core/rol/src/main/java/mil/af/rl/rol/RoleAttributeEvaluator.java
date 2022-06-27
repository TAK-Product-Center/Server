package mil.af.rl.rol;

/*
 * 
 * Implementations of this interface will encapsulate logic for performing attribute evaluation and role assignment for connected clients.
 * 
 * CT is a generic type referencing a client object, or identifier.
 * 
 * Such implementations can be injected into ROL visitors, which will then call these methods as appropriate, so that the visitor and the implementation code can cleanly interact.
 * 
 */
public interface RoleAttributeEvaluator<CT> {
    
    /*
     * Evaluate a client for the given attribute
     * 
     * If any matches for the attribute are found, return a
     * 
     */
    CT evaluate(Attribute attribute);
    
    /*
     * Assign a role to a client
     * 
     */
    void assign(String role);
    
    void setConstraint(int constraint);

}
