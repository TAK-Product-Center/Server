package mil.af.rl.rol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.af.rl.rol.RolBaseVisitor;
import mil.af.rl.rol.RolParser;
import com.google.gson.Gson;

import java.util.Locale;

// Simple ROL visitor that understands parameters and bind / unbind operations
public class SimpleRolVisitor extends RolBaseVisitor<String> {
    
    public static final Logger logger = LoggerFactory.getLogger(SimpleRolVisitor.class);
    
    private String operation = "";

    @Override
    public String visitProgram(RolParser.ProgramContext programCtx) {

        String programResult = "";

        for (ParseTree statement : programCtx.children) {

            String result = visit(statement);

            if (result != null && !result.isEmpty()) {
                programResult = result;
            }
        }

        logger.debug("ROL program execution complete. operation: " + operation);

        // for the overall ROL program, always return a result of 0 for now, to indicate success
        return "0";
    }
    
    @Override
    public String visitAssertion(RolParser.AssertionContext ctx) {
        logger.info("assertion: " + ctx.getText());
        
        return visitChildren(ctx);
    }

    @Override
    public String visitStringParamValue(RolParser.StringParamValueContext ctx) {

        String paramText = ctx.getText();

        logger.debug("visiting parameter " + paramText);

        if (ctx.children != null && ctx.children.size() > 0) {
            String key = ctx.children.get(0).getText();
//            paramKeys.add(key);
            return key;
        }

        return "";
    }

    @Override public String visitOperation(RolParser.OperationContext ctx) {

        String operationText = ctx.getText();

        logger.trace("visiting operation " + operationText);

        // if this is a bind role operation, perform binding logic here. Just detecting the operation, roleUri, and displaying it for this test.
        if (ctx.children != null && ctx.children.size() > 0) {

            logger.trace("operation children size: " + ctx.children.size());

            ParseTree operation = ctx.children.get(0);

            logger.debug("operation: " + operation.getText());
            
            this.operation = operation.getText();
        }

        // in the case of any other operations besides bind, proceed as normal

        return super.visitOperation(ctx);
    }

    private String getUid(ParseTree parameters) {
        if (parameters.getChildCount() > 1) {
            ParseTree param1 = parameters.getChild(1);

            if (param1.getChildCount() > 2 && param1.getChild(0).getText().toLowerCase(Locale.ENGLISH).equals("uid")) {

                ParseTree uidNode = param1.getChild(2);

                logger.trace("uidNode child count: " + uidNode.getChildCount());

                return parseParameterStringOrIdent(uidNode.getChild(0).getText());
            }
        }

        logger.warn("uid not found");
        return "";
    }

    private String parseParameterStringOrIdent(String text) {
        // STRING case
        if (text.startsWith("\"")) {

            // parse the UID string using the Gson JSON library, to get the string value
            return new Gson().fromJson(text, String.class);
        }

        // IDENT case
        return text;
    }
}