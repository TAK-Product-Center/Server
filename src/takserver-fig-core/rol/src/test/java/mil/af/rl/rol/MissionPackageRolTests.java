package mil.af.rl.rol;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;

import mil.af.rl.rol.value.Parameters;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/*
 *
 * Mission package ROL tests
 * 
 */
public class MissionPackageRolTests {

    public static final Logger logger = LoggerFactory.getLogger(MissionPackageRolTests.class);

    private static final String ROL_RESOURCE_MP_PATH = File.separator + "rol" + File.separator + "mp" + File.separator;

    /*
     * Execute ROL programs using a test implementation of CompoundRoleAssignmentRolVisitor
     */
    @Test
    public void execute() throws Exception {

        // parse and visit test programs
        for (File rolFile : com.google.common.io.Files.fileTreeTraverser().preOrderTraversal(new File(getClass().getResource(ROL_RESOURCE_MP_PATH).toURI()))) {
            if (rolFile.isFile() && (rolFile.getName().toLowerCase().endsWith(".rol"))) {

                logger.debug("Testing ROL program: " + rolFile.getName() + " " + Files.toString(rolFile, Charsets.UTF_8));

                RolLexer lexer = new RolLexer(new ANTLRInputStream(new FileInputStream(rolFile)));

                CommonTokenStream tokens = new CommonTokenStream(lexer);

                RolParser parser = new RolParser(tokens);
                parser.setErrorHandler(new BailErrorStrategy());

                // parse the ROL program
                ParseTree programParseTree = parser.program();

                assertNotNull(programParseTree);

                new MissionRolVisitor(new ResourceOperationParameterEvaluator<Parameters, String>() {

                    @Override
                    public String evaluate(String resource, String operation, Parameters parameters) {
                        logger.debug(" evaluating " + operation + " on " + resource + " given " + parameters);
                        return resource;
                    }
                }).visit(programParseTree); // execute the ROL
            }
        }       
    }
}
