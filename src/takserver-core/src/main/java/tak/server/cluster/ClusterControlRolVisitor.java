package tak.server.cluster;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.config.Network.Input;
import com.bbn.marti.remote.exception.TakException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import mil.af.rl.rol.ResourceOperationParameterEvaluator;
import mil.af.rl.rol.RolBaseVisitor;
import mil.af.rl.rol.RolParser;

/*
 * 
 * ROL visitor implementation that handles compound assertions expression evaluation, as well as tracking the operation, role and constraints.
 * 
 * CT is a generic class, which represents a client or client identifier.
 * 
 */
public class ClusterControlRolVisitor extends RolBaseVisitor<String> {

    Logger logger = LoggerFactory.getLogger(ClusterControlRolVisitor.class);
    
    private final ResourceOperationParameterEvaluator<Object, String> evaluator;
    
    private String role;
    
    private String operation;
    
    private String resource;
    
    private Object parameters;
    
    public ClusterControlRolVisitor(ResourceOperationParameterEvaluator<Object, String> evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public String visitProgram(RolParser.ProgramContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public String visitStatement(RolParser.StatementContext ctx) {
        return visitChildren(ctx);
    }
    
    @Override
    public String visitOperation(RolParser.OperationContext ctx) {
    	if (logger.isDebugEnabled()) {
    		logger.debug("operation - childCount: " + ctx.getChildCount());
    	}
        
        operation = ctx.getChild(0).getText();
        
        if (logger.isDebugEnabled()) {
        	logger.debug("operation: " + operation);
        }
        
        return visitChildren(ctx);
    }
    
    @Override
    public String visitResource(RolParser.ResourceContext ctx) {
        
    	if (logger.isDebugEnabled()) {
    		logger.debug("resource - childCount: " + ctx.getChildCount());
    	}
        
        resource = ctx.getText();
        
        if (logger.isDebugEnabled()) {
        	logger.debug("resource: " + resource);
        }
        
        
        if (ctx.getChildCount() == 2) {
            String resource = ctx.getChild(0).getText();
            
            if (logger.isDebugEnabled()) {
            	logger.debug("resource: " + resource);
            }
            
            this.resource = resource;
            
            if (resource.equals("role")) {
                role = ctx.getChild(1).getText();
                
                if (role != null && role.contains("\"")) {

                    // try to parse as JSON, using Jackson, to unescape and remove quotes. This works because the IDENT and STRING productions in ROL are compatible with JSON.
                    try {
                        role = (String) new ObjectMapper().readValue(role, String.class);
                    } catch (Exception e) {
                        logger.debug("exception parsing key", e);
                    }
                }
            }
            
            if (logger.isDebugEnabled()) {
            	logger.debug(resource + " " + (role != null ? role : ""));
            }
        }
        
        return visitChildren(ctx);
    }
    
    @Override
    public String visitParameters(RolParser.ParametersContext ctx) {
        String paramsText = ctx.getText();
        
        parameters = paramsText;
        
        String evalResult = evaluator.evaluate(resource, operation, parameters);
        
        if (logger.isDebugEnabled()) {
        	logger.debug("eval complete - result: " + evalResult + " ctx: " + ctx + " childCount: " + ctx.getChildCount());
        }
        
        return evalResult;
    }
}
