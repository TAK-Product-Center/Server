package mil.af.rl.rol;
import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import mil.af.rl.rol.AbstractConstrainedAttributeEvaluator;
import mil.af.rl.rol.Attribute;
import mil.af.rl.rol.CompoundRoleAssignmentRolVisitor;

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

import static org.junit.Assert.*;

/*
 *
 * Tests for CompoundRoleAssignmentRolVisitor
 * 
 */
public class CompoundRoleAssignmentVisitorTests {

    public static final Logger logger = LoggerFactory.getLogger(CompoundRoleAssignmentVisitorTests.class);
    
    private static final String ASSERTION_EXP_ROL_PATH = RolTests.ROL_RESOURCE_PATH + File.separator + "assign_revoke" + File.separator;

    /*
     * Execute ROL programs using a test implementation of CompoundRoleAssignmentRolVisitor
     */
    @Test
    public void execute() throws Exception {
        
        // parse and visit test programs
        for (File rolFile : com.google.common.io.Files.fileTreeTraverser().preOrderTraversal(new File(getClass().getResource(ASSERTION_EXP_ROL_PATH).toURI()))) {
            if (rolFile.isFile() && (rolFile.getName().toLowerCase().equals("role_assign_compound_safety_inspector__or.rol")) ||
                    (rolFile.getName().toLowerCase().equals("role_assign_compound_safety_inspector__and.rol")) ||
                    (rolFile.getName().toLowerCase().equals("role_assign_compound_safety_inspector__paren_or.rol")) ||
                    (rolFile.getName().toLowerCase().equals("role_assign_client_attributes.rol"))) {
                
                logger.debug("Testing ROL program: " + rolFile.getName() + " " + Files.toString(rolFile, Charsets.UTF_8));
                
                RolLexer lexer = new RolLexer(new ANTLRInputStream(new FileInputStream(rolFile)));

                CommonTokenStream tokens = new CommonTokenStream(lexer);

                RolParser parser = new RolParser(tokens);
                parser.setErrorHandler(new BailErrorStrategy());

                // parse the ROL program
                ParseTree programParseTree = parser.program();

                assertNotNull(programParseTree);
                
                AtomicInteger assignmentCounter = new AtomicInteger();
                
                // simulate a client attribute store - store a map of attributes, per client.
                final Map<String, Map<String, String>> clientAttributeStore = new ConcurrentHashMap<>();
                
                clientAttributeStore.put("client_a", new ConcurrentHashMap<>());
                Map<String, String> clientAstore = clientAttributeStore.get("client_a");
                
                clientAstore.put("dn", "CN=HomerJay, OU=DEV, O=AFRL, ST=NY, C=US");
                clientAstore.put("planet", "earth");
                
                clientAttributeStore.put("client_b", new ConcurrentHashMap<>());
                Map<String, String> clientBstore = clientAttributeStore.get("client_b");
                
                clientBstore.put("dn", "CN=ClientB");
                clientBstore.put("company", "xyz");
                clientBstore.put("planet", "earth");
                
                for (String client : clientAttributeStore.keySet()) {
                
                // visit the parse tree - with a dummy attribute evaluator callback that always returns true
                new CompoundRoleAssignmentRolVisitor<String>(new AbstractConstrainedAttributeEvaluator<String>(assignmentCounter, client) {

                    @Override
                    public String evaluate(Attribute attribute) {

                        logger.debug("evaluating attribute " + attribute);

                        logger.debug("checking client " + client + " " + clientAttributeStore.get(client).get(attribute.getKey()));

                        if (clientAttributeStore.get(client).get(attribute.getKey()) == null) {
                            return null;
                        }

                        if (clientAttributeStore.get(client).get(attribute.getKey()).equals(attribute.getValue())) {
                            logger.debug("attribute " + attribute + " found for client " + client);

                            return client;  // match found
                        }

                        logger.debug("no match found for client " + client);
                        return null; // found no match for any client
                    }

                    @Override
                    public void assign(String role) {
                        logger.debug("assigning role: " + role + " to client: " + client);
                        
                        Map<String, String> perClientStore = clientAttributeStore.get(client);
                        
                        if (perClientStore == null) {
                            throw new RuntimeException("client " + client + " is not tracked");
                        }
                        
                        perClientStore.put("role", role);

                        super.assign(role);
                    }

                    @Override
                    public void setConstraint(int constraint) { }
                }).visit(programParseTree);
                
//                assertTrue(assignmentCounter.get() > 0);
                
                logger.debug("role assignment count: " + assignmentCounter);
                
                logger.debug("client attribute store: " + clientAttributeStore);
                }
            }
        }       
    }
}
