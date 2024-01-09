package mil.af.rl.rol;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;

import mil.af.rl.rol.OperationConstraintsVisitor;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.af.rl.rol.RolLexer;
import mil.af.rl.rol.RolParser;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

/*
 *
 * Tests for OperationConstraintsVisitor
 * 
 */
public class OperationConstraintsVisitorTests {

    public static final Logger logger = LoggerFactory.getLogger(OperationConstraintsVisitorTests.class);
    
    private static final String CONSTRAINTS_ROL_PATH = RolTests.ROL_RESOURCE_PATH + File.separator + "constraints" + File.separator;

    /*
     * Execute ROL programs with the constraints visitor
     */
    @Test
    public void execute() throws Exception {
        
        // parse and visit test programs
        for (File rolFile : com.google.common.io.Files.fileTraverser().depthFirstPreOrder(new File(getClass().getResource(CONSTRAINTS_ROL_PATH).toURI()))) {
            if (rolFile.isFile() && rolFile.getName().toLowerCase().endsWith(".rol")) {
                
                logger.debug("Testing ROL program: " + rolFile.getName() + " " + Files.toString(rolFile, Charsets.UTF_8));
                
                RolLexer lexer = new RolLexer(new ANTLRInputStream(new FileInputStream(rolFile)));

                CommonTokenStream tokens = new CommonTokenStream(lexer);

                RolParser parser = new RolParser(tokens);
                parser.setErrorHandler(new BailErrorStrategy());

                // parse the ROL program
                ParseTree programParseTree = parser.program();

                assertNotNull(programParseTree);

                // visit the parse tree
                String result = new OperationConstraintsVisitor().visit(programParseTree);
                
                logger.debug("constraint visitor result: " + result);
            }
        }       
    }
}
