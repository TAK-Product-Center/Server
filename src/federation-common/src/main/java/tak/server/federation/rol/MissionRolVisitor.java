package tak.server.federation.rol;

import java.io.IOException;

import mil.af.rl.rol.ResourceOperationParameterEvaluator;
import mil.af.rl.rol.RolBaseVisitor;
import mil.af.rl.rol.RolParser;
import mil.af.rl.rol.value.Parameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

/*
 * 
 * ROL visitor implementation that handles compound assertions expression evaluation, as well as tracking the operation, role and constraints.
 * 
 * CT is a generic class, which represents a client or client identifier.
 * 
 */
public class MissionRolVisitor extends RolBaseVisitor<String> {

    Logger logger = LoggerFactory.getLogger("ROL");
    
    private final ResourceOperationParameterEvaluator<Parameters, String> evaluator;
    
    private String role;
    
    private String operation;
    
    private String resource;
    
    private Parameters parameters;
        
    public MissionRolVisitor(ResourceOperationParameterEvaluator<Parameters, String> evaluator) {
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
        logger.trace("operation - childCount: " + ctx.getChildCount());
        
        operation = ctx.getChild(0).getText();
        
        logger.debug("operation: " + operation);
        
        return visitChildren(ctx);
    }
    
    @Override
    public String visitResource(RolParser.ResourceContext ctx) {
        
        logger.debug("resource - childCount: " + ctx.getChildCount());
        
        resource = ctx.getText();
        logger.debug("resource: " + resource);
        
        
        if (ctx.getChildCount() == 2) {
            String resource = ctx.getChild(0).getText();
            
            logger.debug("resource: " + resource);
            
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
            
            logger.debug(resource + " " + (role != null ? role : ""));
        }
        
        return visitChildren(ctx);
    }
    
    @Override
    public String visitParameters(RolParser.ParametersContext ctx) {
        String paramsText = ctx.getText();
        
        if (!Strings.isNullOrEmpty(paramsText)) {
            try {
                parameters = new ObjectMapper().readValue(paramsText, Parameters.class);
            } catch (IOException e) {
                logger.debug("exception parsing ROL parameters text " + e.getMessage());
            }
        }
        
        logger.debug("parameters: " + paramsText + " parsed parameters: " + parameters);
        
        evaluator.evaluate(resource, operation, parameters);
        
        return visitChildren(ctx);
    }
}
