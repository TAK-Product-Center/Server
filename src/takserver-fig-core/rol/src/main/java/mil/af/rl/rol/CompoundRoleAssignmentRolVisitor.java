package mil.af.rl.rol;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

/*
 * 
 * ROL visitor implementation that handles compound assertions expression evaluation, as well as tracking the operation, role and constraints.
 * 
 * CT is a generic class, which represents a client or client identifier.
 * 
 */
public class CompoundRoleAssignmentRolVisitor<CT> extends RolBaseVisitor<CT> {

    Logger logger = Logger.getLogger(CompoundRoleAssignmentRolVisitor.class);
    
    private final RoleAttributeEvaluator<CT> evaluator;
    
    private String role;
    
    private String operation;
        
    private CT lastClientResult = null;
    
    public CompoundRoleAssignmentRolVisitor(RoleAttributeEvaluator<CT> evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public CT visitProgram(RolParser.ProgramContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public CT visitStatement(RolParser.StatementContext ctx) {
        return visitChildren(ctx);
    }

    // an "assertions" production can contain just one assertion, or multiple assertions connected by logical operators
    @Override
    public CT visitAssertions(RolParser.AssertionsContext ctx) {

        logger.trace("visit assertions: " + ctx.getText());

        return processAssertions(ctx);
    }

    private CT processAssertions(ParserRuleContext ctx) {
        
        boolean isRootAssertions = ctx.getParent().getClass().getSimpleName().equals("StatementContext");
        
        String lastBinOp = null;

        if (ctx == null || ctx.children == null) {
            logger.debug("null ctx or children");
            return null;
        }

        logger.trace("processing assertions - childCount: " + ctx.getChildCount());

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            String childType = child.getClass().getSimpleName();
            switch (childType) {
            case "AssertionContext":
                logger.trace("processAssertions childType: " + childType);
                
                CT ct = processAssertion((RolParser.AssertionContext) child);
                
                if (isRootAssertions && ctx.getChildCount() == 1 && ct != null) {
                    evaluator.assign(role);
                    return ct;
                }
                
                logger.debug("client result: " + ct);
                
                lastClientResult = ct;
                
                if (lastBinOp != null && lastBinOp.equals("and")) {
                    if (lastClientResult == null) {
                        // We are done - the left side of this and was false. Stop evaluating children.
                        return null;
                    } 
                    
                    // is this the last child? then we are doing evaluating and parts
                    if (i == ctx.getChildCount() - 1) {
                        logger.debug("last assertions child");

                        
                        if (lastClientResult != null && isRootAssertions) {
                            evaluator.assign(role);
                        }
                        
                        return lastClientResult;
                    }
                }
             
            break;
            case "BinaryOpContext":
                String binaryOp = child.getChild(0).getText();
                
                lastBinOp = binaryOp;
                
                // TODO: do something here to return the CT
                logger.debug("binaryOpContext: child count: " + child.getChildCount() + " operator: " + binaryOp);
                
                // The left operand has already been evaluated. So, take that into account by short-circuiting.
                if (binaryOp.equals("or")) {
                    if (lastClientResult != null) { // need to check for being the top-level
                        evaluator.assign(role);
                    } 
                } 
                
                // true and case, and left-side false or case - keep going.
                
                break;
            default:
                logger.debug("Unexpected assertions child context type: " + child.getClass().getSimpleName());    
            }
            
            if (isRootAssertions && lastBinOp != null && lastBinOp.equals("or") && lastClientResult != null) {
                evaluator.assign(role);
            }
            
            logger.debug("finished recursing through children of AssertionsContext. isRootAssertions: " + isRootAssertions + " lastClientResult: " + lastClientResult + " lastBinOp: " + lastBinOp);
        }
        
        return null;
    }

    private CT processAssertion(ParserRuleContext ctx) {
        logger.trace("processAssertion - childCount: " + ctx.getChildCount());
        
        // the parent is always an AssertionsContext, due to the grammar structure
//        logger.debug("assertion parent type: " + ctx.getParent().getClass().getSimpleName());
//        ctx.get
        

        // match attribute assertion
        if (ctx.getChild(0).getText().equals("match attribute")) {
            logger.debug("match attribute");

            Attribute attribute = processParameter((RolParser.ParameterContext) ctx.getChild(1));

            logger.debug("attribute: " + attribute);
            
            CT client = evaluator.evaluate(attribute);
            
            lastClientResult = client;
            
            if (role == null) {
                throw new RoleNotDefinedException("role not parseable from ROL statement");
            }
            
            logger.debug("attribute eval client result: " + client);
            
            return client;
            
        }  else if (ctx.getChild(0).getText().equals("(") && ctx.getChild(2).getText().equals(")")) { // parenthetical expression
            // could support parentheses without operator here
            logger.debug("parenthetical expression");
            
            CT parenResult = processParenAssertions((ParserRuleContext) ctx.getChild(1));
            
            logger.debug("parenthetical expression result: " + parenResult);

            return parenResult;
        } else if (ctx.getChild(0).getClass().getSimpleName().equals("UnaryOpContext"))  { // unary operation (not is the only one, for now)
            logger.debug("not operation");
            
            // TODO: sort this out

            return processAssertions((ParserRuleContext) ctx.getChild(1));
        }
        
        else if (ctx.getChild(0).getClass().getSimpleName().equals("matchrole"))  { // match role assertion
            logger.debug("match role assertion - role: " + ctx.getChild(1).getText());

            return processAssertions((ParserRuleContext) ctx.getChild(1));
        }

        logger.warn("unexpected assertion " + ctx.getChild(0).getClass().getSimpleName());
        
        return null;
    }

    private Attribute processParameter(RolParser.ParameterContext ctx) {

        logger.trace("processing attribute");

        Attribute result = null;

        if (ctx.children.size() == 3) {

            String key = ctx.getChild(0).getText();
            
            if (key != null && key.contains("\"")) {

                // try to parse as JSON, using Jackson, to unescape and remove quotes. This works because the IDENT and STRING productions in ROL are compatible with JSON.
                try {
                    key = (String) new ObjectMapper().readValue(key, String.class);
                } catch (Exception e) {
                    logger.debug("exception parsing key", e);
                }
            }

            String value = ctx.getChild(2).getChild(0).getText();

            if (value != null && value.contains("\"")) {
                try {
                    value = (String) new ObjectMapper().readValue(value, String.class);
                } catch (Exception e) {
                    logger.debug("exception parsing key", e);
                }
            }

            result = new Attribute(key, value);
        }

        return result;
    }

    private CT processParenAssertions(ParserRuleContext ctx) {

        // if only one child, no new container is needed - just process child
        if (ctx.getChildCount() == 1 && ctx.getChild(0).getClass().getSimpleName().equals("AssertionContext")) {
            logger.debug("assertions context containing single assertion - recursing");
            return processAssertion((ParserRuleContext) ctx.getChild(0));
        }

        for (ParseTree child : ctx.children) {

            String childType = child.getClass().getSimpleName();

            switch(childType) {
            case "AssertionContext":
                return processAssertion((ParserRuleContext) child);
            case "BinaryOpContext":
                // set the binary operation type
                logger.debug("paren binary operator: " + ((ParserRuleContext) child).getChild(0).getText());
                break;
            default:
                throw new RuntimeException("Unexpected assertions child context type: " + childType);
            }
        }
        
        return null;
    }

    // constraints-related
    @Override
    public CT visitConstraintsClause(RolParser.ConstraintsClauseContext ctx) {

        logger.trace("visit constraints clause " + ctx.getText() + " child count " + ctx.getChildCount());
        
        CT result = null;

        for (ParseTree child : ctx.children) {

            result = visit(child);             
        }

        // return the result of the last child
        return result;
    }

    @Override
    public CT visitSimpleLeafConstraint(RolParser.SimpleLeafConstraintContext ctx) {

        logger.trace("visit simple constraint op. text: " + ctx.getText() + " tree: " + ctx.toStringTree() + " child count: " + ctx.getChildCount());

        if (ctx.getChildCount() != 2) {
            throw new IllegalStateException("invalid constraint: " + ctx.getText());
        }

        String constraintOp = ctx.getChild(0).getText();
        String constraintVal = ctx.getChild(1).getText();
        
        logger.debug("constraint op: " + constraintOp + " value: " + constraintVal);
        
        // set the constraint in the evaluator, so that it can be used appropriately at runtime
        try {
        	evaluator.setConstraint(Integer.parseInt(constraintVal));
        } catch (Exception e) {
        	logger.debug("exception parsing constrint value " + constraintVal, e);
        }

        return null;
    }

    @Override
    public CT visitConstraints(RolParser.ConstraintsContext ctx) {

        logger.trace("visit constraints. child count: " + ctx.getChildCount());
        
        CT result = null;

        for (ParseTree child : ctx.children) {

            logger.trace("constraint child type: " + child.getClass().getName());

            result = visit(child); 
        }

        return result;
    }  

    @Override
    public CT visitBinaryOp(RolParser.BinaryOpContext ctx) {
        return null;
    }
    
    // resource - operation related
    @Override
    public CT visitOperation(RolParser.OperationContext ctx) {
        logger.trace("operation - childCount: " + ctx.getChildCount());
        
        operation = ctx.getChild(0).getText();
        
        logger.debug("operation: " + operation);
        
        return visitChildren(ctx);
    }
    
    @Override
    public CT visitResource(RolParser.ResourceContext ctx) {
        logger.trace("resource - childCount: " + ctx.getChildCount());
        
        if (ctx.getChildCount() == 2) {
            String resource = ctx.getChild(0).getText();
            
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
}
