package mil.af.rl.rol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.af.rl.rol.RolBaseVisitor;
import mil.af.rl.rol.RolParser;

/*
 * 
 * Simple visitor dealing only with ROL constraints.
 * 
 * It ignores everything except constraint clauses, traverses the constraint clause expression tree, gets all the pieces out, and puts them back together again.
 * 
 * This is an example of how to get at the salient parts of the clauses, and know what they are, so that they can be processed or inserted in an appropriate
 * data structure in the execution domain.
 * 
 */
public class OperationConstraintsVisitor extends RolBaseVisitor<String> {
    
    private static final Logger logger = LoggerFactory.getLogger(OperationConstraintsVisitor.class);
    
    // only visit constraints, not other statement sub-parts.
    @Override public String visitStatement(RolParser.StatementContext ctx) {
        
        // this is necessary because visitChildren can return null in some cases. So be a little more selective by checking the type of the child.
        if (ctx.children != null) {
            for (ParseTree child : ctx.children) {
                if (child != null && child instanceof RolParser.ConstraintsClauseContext) {
                    return visit(child);
                }
            }
        }
        
        throw new IllegalStateException("no constraints clause present in statement: " + ctx.getText());
     }
    
    @Override public String visitConstraintsClause(RolParser.ConstraintsClauseContext ctx) {
        
        logger.debug("visit constraints clause " + ctx.getText() + " child count " + ctx.getChildCount());
        
        StringBuilder result = new StringBuilder("constraint is ");
        
        for (ParseTree child : ctx.children) {
            
            String childResult = visit(child);             
            
            if (childResult != null) {
                result.append(' ');
                result.append(childResult);
            }
        }
        
        return result.toString();
    }
    
    @Override public String visitSimpleLeafConstraint(RolParser.SimpleLeafConstraintContext ctx) {

        logger.debug("visit simple constraint op. text: " + ctx.getText() + " tree: " + ctx.toStringTree() + " child count: " + ctx.getChildCount());

        if (ctx.getChildCount() != 2) {
            throw new IllegalStateException("invalid constraint: " + ctx.getText());
        }

        String constraintOp = ctx.getChild(0).getText();
        String constraintVal = ctx.getChild(1).getText();

        // At this point, we know what the constraint is. So something could be done to make to apply it, like save it off in an appropriate data structure, and use that when main resource operation execution.

        // return something marginally meaningful
        return constraintOp + " " + constraintVal;
    }
    
    @Override public String visitConstraints(RolParser.ConstraintsContext ctx) {
        
        logger.debug("visit constraints. child count: " + ctx.getChildCount());
        
        StringBuilder result = new StringBuilder();
        
        for (ParseTree child : ctx.children) {
            
            logger.debug("constraint child type: " + child.getClass().getName());
            
            String childResult = visit(child); 

            
            if (childResult != null) {
                result.append(' ');
                result.append(childResult);
            }
        }
        
        return result.toString();
    }  
    
    @Override public String visitBinaryOp(RolParser.BinaryOpContext ctx) {
        return ctx.getText();
    }

}
