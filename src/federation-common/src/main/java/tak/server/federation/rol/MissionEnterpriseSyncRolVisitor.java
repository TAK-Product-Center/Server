package tak.server.federation.rol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.remote.sync.MissionHierarchy;
import com.bbn.marti.remote.sync.MissionUpdateDetails;
import com.bbn.marti.sync.model.Resource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import mil.af.rl.rol.ResourceOperationParameterEvaluator;
import mil.af.rl.rol.RolBaseVisitor;
import mil.af.rl.rol.RolParser;
import mil.af.rl.rol.value.Parameters;

/*
 * 
 * ROL visitor implementation that handles compound assertions expression evaluation, as well as tracking the operation, role and constraints.
 * 
 * CT is a generic class, which represents a client or client identifier.
 * 
 */
public class MissionEnterpriseSyncRolVisitor extends RolBaseVisitor<String> {

    Logger logger = LoggerFactory.getLogger("ROL");
    
    private final ResourceOperationParameterEvaluator<Object, String> evaluator;
    
    private String role;
    
    private String operation;
    
    private String resource;
    
    private Object parameters;
        
    public MissionEnterpriseSyncRolVisitor(ResourceOperationParameterEvaluator<Object, String> evaluator) {
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
        
        operation = ctx.getChild(0).getText();

        if (logger.isDebugEnabled()) {
        	logger.debug("operation: " + operation);
        }
        
        return visitChildren(ctx);
    }
    
    @Override
    public String visitResource(RolParser.ResourceContext ctx) {
        
        resource = ctx.getText();
        
        if (logger.isDebugEnabled()) {
        	logger.debug("resource: " + resource);
        }
        
        if (ctx.getChildCount() == 2) {
            String resource = ctx.getChild(0).getText();
            
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
        
        if (!Strings.isNullOrEmpty(paramsText)) {
            try {
                parameters = new ObjectMapper().readValue(paramsText, Parameters.class);
                if (logger.isDebugEnabled()) {
                	logger.debug("parsed parameters: " + parameters);
                }
            } catch (Exception e) {
            	if (logger.isDebugEnabled()) {
            		logger.debug("ROL JSON is not a Parameters object");
            	}
            }
            
            if (parameters == null) {
                try {
                	
                	ObjectMapper objectMapper = new ObjectMapper();

					parameters = objectMapper.readValue(paramsText, Resource.class);
					
					if (logger.isDebugEnabled()) {
	                	logger.debug("parsed resource: " + parameters);
	                }
				} catch (Exception e) {
					if (logger.isDebugEnabled()) {
						logger.debug("ROL JSON is not a Resource object");
					}
				}
            }
            
            if (parameters == null) {
                try {
                	
                	ObjectMapper objectMapper = new ObjectMapper();

					parameters = objectMapper.readValue(paramsText, MissionUpdateDetails.class);
					
					if (logger.isDebugEnabled()) {
	                	logger.debug("parsed mission update details: " + parameters);
	                }
				} catch (Exception e) {
					if (logger.isDebugEnabled()) {
						logger.debug("ROL JSON is not a MissionUpdateDetails object");
					}
				}
            }

            if (parameters == null) {
                try {
                	if (logger.isDebugEnabled()) {
                		logger.debug("trying to parse as MissionHierarchy");
                	}

                    ObjectMapper objectMapper = new ObjectMapper();

                    parameters = objectMapper.readValue(paramsText, MissionHierarchy.class);

                    if (logger.isDebugEnabled()) {
                        logger.debug("parsed mission hierarchy details: " + parameters);
                    }
                } catch (Exception e) {
                	
                	if (logger.isDebugEnabled()) {
                		logger.debug("ROL JSON is not a MissionHierarchy object ");
                	}
                }
            }

        }
        
        if (logger.isDebugEnabled()) {
        	logger.debug("parameters: " + paramsText + " parsed parameters: " + parameters);
        }
        
        evaluator.evaluate(resource, operation, parameters);
        
        return visitChildren(ctx);
    }
}
