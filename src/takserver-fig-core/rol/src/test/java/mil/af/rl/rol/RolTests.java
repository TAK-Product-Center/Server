package mil.af.rl.rol;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.af.rl.rol.RolLexer;
import mil.af.rl.rol.RolParser;

public class RolTests {

    static final String ROL_RESOURCE_PATH = File.separator + "rol" + File.separator;

    public static final Logger logger = LoggerFactory.getLogger(RolTests.class);

    @Test
    public void parser() throws Exception {

        // get all the files the rol dir
        URL rolRootUrl = getClass().getResource(ROL_RESOURCE_PATH);

        logger.debug("rol url " + rolRootUrl);

        assertNotNull(rolRootUrl);

        File rolDir = new File(rolRootUrl.toURI());

        assertNotNull(rolDir);

        logger.debug("rol dir: " + rolDir);

        int count = 0;

        for (File rolFile : com.google.common.io.Files.fileTreeTraverser().preOrderTraversal(rolDir)) {
            if (rolFile.isFile() && rolFile.getName().toLowerCase().endsWith(".rol")) {

                RolLexer lexer = new RolLexer(new ANTLRInputStream(new FileInputStream(rolFile)));
                CommonTokenStream tokens = new CommonTokenStream(lexer);

                RolParser parser = new RolParser(tokens);
                
                parser.setErrorHandler(new BailErrorStrategy());

                // parse the ROL program
                ParseTree program = parser.program();
             
                assertNotNull(program);

                count++;

                logger.debug("rol program " + rolFile + " parse tree: " + program.toStringTree());
            }
        }
        
        logger.info("parsed " + count + " ROL programs");
    }
}
