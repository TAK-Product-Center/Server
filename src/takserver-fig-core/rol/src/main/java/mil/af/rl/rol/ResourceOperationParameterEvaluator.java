package mil.af.rl.rol;

/*
 * 
 */
public interface ResourceOperationParameterEvaluator<P, R> {
    
    /*
     * perform generic evaluation according to the specificated resource, operation and parameters provided
     * 
     */
    R evaluate(String resource, String operation, P parameters);
}
