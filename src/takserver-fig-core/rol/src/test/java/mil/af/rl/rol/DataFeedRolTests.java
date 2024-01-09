package mil.af.rl.rol;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;

import mil.af.rl.rol.value.Parameters;

import tak.server.federation.rol.MissionRolVisitor;

public class DataFeedRolTests {
		  
	    private static final String ROL_RESOURCE_DATA_FEED_PATH = File.separator + "rol" + File.separator + "datafeed" + File.separator;
	  
	    @Test
	    public void execute() throws Exception {
	        // parse and visit test programs
	        for (File rolFile : com.google.common.io.Files.fileTraverser().depthFirstPreOrder(new File(getClass().getResource(ROL_RESOURCE_DATA_FEED_PATH).toURI()))) {
	            if (rolFile.isFile() && (rolFile.getName().toLowerCase().endsWith(".rol"))) {

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
	                        System.out.println(" evaluating " + operation + " on " + resource + " given " + parameters);
	                        
	                        return resource;
	                    }
	                }).visit(programParseTree); // execute the ROL
	            }
	        }       
	    }
	}
