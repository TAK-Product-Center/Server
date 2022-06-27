package mil.af.rl.rol;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import mil.af.rl.rol.RolBuilder;
import mil.af.rl.rol.RolBuilder.StringParameter;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.af.rl.rol.RolLexer;
import mil.af.rl.rol.RolParser;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;

import static org.junit.Assert.*;

public class RolBuilderTests {
    
    private static final String SPARQL_DATE_QUERY = "SELECT  ?io\n" + 
            "WHERE\n" + 
            "  { ?producer  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  <http://ontology.ihmc.us/IMS/IMSActor.owl#IMSClient> .\n" + 
            "    ?io       <http://ontology.ihmc.us/IMS/IMSEntity.owl#hasPublisher>  ?producer ;\n" + 
            "              <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  <http://ontology.ihmc.us/IMS/IMSEntity.owl#InformationObject> .\n" + 
            "    ?io     <http://ontology.ihmc.us/IMS/IMSEntity.owl#hasPublicationTime>  ?var_1 .\n" + 
            "    ?var_1  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  <http://www.w3.org/2001/XMLSchema#dateTime> .\n" + 
            "  }";
    
    Logger logger = LoggerFactory.getLogger(RolBuilderTests.class);
    
    @Test
    public void sparqlParameters() {
        RolBuilder builder = new RolBuilder();
        
        String result = builder.parameters(Lists.newArrayList(
                new StringParameter("type", "SPARQL"),
                new StringParameter("filter", SPARQL_DATE_QUERY),
                new StringParameter("name", "date_query")
                ));
        
        logger.debug("parameters: " + result);
        
        assertNotNull(result);
    }
    
    // validate that the builder creates parsable semantic subscription creation ROL
    @Test
    public void createAndParseSemanticSubscriptionRolCreateAndRemove() throws IOException {
        
        RolBuilder rb = new RolBuilder();
        
        String rolCreateSub = rb.createSemanticSubscription("date_sub", SPARQL_DATE_QUERY);
        
        logger.debug("generated create semantic ROL program: " + rolCreateSub);
        
        assertNotNull(rolCreateSub);
        
        RolLexer lexer = new RolLexer(new ANTLRInputStream((new ByteArrayInputStream(rolCreateSub.getBytes(Charsets.UTF_8)))));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        RolParser parser = new RolParser(tokens);

        // parse the ROL program
        ParseTree program = parser.program();

        assertNotNull(program.toStringTree());
        
        String rolRemoveSub = rb.removeSubscription("date_sub");
        
        logger.debug("generated delete subscription ROL program: " + rolRemoveSub);
        
        assertNotNull(rolRemoveSub);
        
        lexer = new RolLexer(new ANTLRInputStream((new ByteArrayInputStream(rolRemoveSub.getBytes(Charsets.UTF_8)))));
        tokens = new CommonTokenStream(lexer);

        parser = new RolParser(tokens);

        // parse the ROL program
        program = parser.program();

        assertNotNull(program.toStringTree());
     
        logger.debug("sparql rol parse tree: " + program.toStringTree());
        
    }
    
    @Test
    public void createAndParseTypeBasedSubscription() throws IOException {
        
        RolBuilder rb = new RolBuilder();
        
        String rolCreateTypeSub = rb.createTypeBasedSubscription("tb_sub", "JTAC", Lists.newArrayList("BLUE_FORCE_TRACKING"));
        
        assertNotNull(rolCreateTypeSub);
        
        logger.debug("type-based sub: " + rolCreateTypeSub);
        
        RolLexer lexer = new RolLexer(new ANTLRInputStream((new ByteArrayInputStream(rolCreateTypeSub.getBytes(Charsets.UTF_8)))));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        RolParser parser = new RolParser(tokens);

        // parse the ROL program
        ParseTree program = parser.program();

        assertNotNull(program.toStringTree());
    }
}
