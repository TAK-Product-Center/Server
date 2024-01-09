package tak.server;

import com.atakmap.Tak.ROL;
import com.bbn.marti.remote.sync.MissionHierarchy;
import com.fasterxml.jackson.databind.ObjectMapper;

import mil.af.rl.rol.RolLexer;
import mil.af.rl.rol.RolParser;
import tak.server.federation.rol.MissionEnterpriseSyncRolVisitor;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.XmlMappingException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TakServerTestApplicationConfig.class})
public class MissionFederationROLTests {

    private static final Logger logger = LoggerFactory.getLogger(MissionFederationROLTests.class);

    private	static final String SET_PARENT_ROL = "assign mission\n";
    private	static final String PARENT_MISSION_NAME = "parentMission";
    private	static final String CHILD_MISSION_NAME = "childMission";

    @Test
    public void setMissionParentTest() throws XmlMappingException, IOException {

        MissionHierarchy missionHierarchy = new MissionHierarchy();
        missionHierarchy.setMissionName(CHILD_MISSION_NAME);
        missionHierarchy.setParentMissionName(PARENT_MISSION_NAME);

        String rolProgramSetParent = SET_PARENT_ROL + new ObjectMapper().writeValueAsString(missionHierarchy) + ";";
        logger.debug("ROL set parent: " + rolProgramSetParent);

        ROL originalRol = ROL.newBuilder().setProgram(rolProgramSetParent).build();
        ROL rolDeserialized = ROL.parseFrom(originalRol.toByteArray());
        RolLexer lexer = new RolLexer(new ANTLRInputStream(rolDeserialized.getProgram()));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        RolParser parser = new RolParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());

        // parse the ROL program
        ParseTree rolParseTree = parser.program();
        requireNonNull(rolParseTree, "parsed ROL program");

        logger.debug("about to parse and visit ROL program "  + rolProgramSetParent);
        AtomicReference<MissionHierarchy> missionHierarchyRef = new AtomicReference<>();
        String parseResult = new MissionEnterpriseSyncRolVisitor((resource, operation, parameters) -> {
            logger.debug(" evaluating " + operation + " on " + resource + " given " + parameters);

            if (parameters instanceof MissionHierarchy) {
                missionHierarchyRef.set((MissionHierarchy)parameters);
            }

            return resource;
        }).visit(rolParseTree);

        logger.debug("parseResult: " + parseResult);
        assertEquals(missionHierarchyRef.get().getParentMissionName(), PARENT_MISSION_NAME);
        assertEquals(missionHierarchyRef.get().getMissionName(), CHILD_MISSION_NAME);
    }
}
