package tak.server;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.XmlMappingException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.atakmap.Tak.ROL;
import com.bbn.marti.config.Network.Input;
import com.bbn.marti.remote.ServerInfo;
import com.bbn.marti.remote.exception.TakException;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.protobuf.InvalidProtocolBufferException;

import mil.af.rl.rol.RolLexer;
import mil.af.rl.rol.RolParser;
import tak.server.cluster.ClusterControlRolVisitor;
import tak.server.messaging.MessageConverter;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TakServerTestApplicationConfig.class})
public class InputControlMessageTests {

	private static final Logger logger = LoggerFactory.getLogger(InputControlMessageTests.class);

	// ROL resource-operation to create a TAK server input
	private	static final String CREATE_INPUT_ROL = "create input\n";
	private	static final String DELETE_INPUT_ROL = "delete input\n";
	
	@Autowired
	private MessageConverter clusterMessageConverter;
	
	@Autowired
	private ServerInfo serverInfo;
	
	private Input getInput() {
		Input input = new Input();
		
		input.setName("myinput");
		input.setPort(9999);
		input.setCoreVersion(2);
		
		return input;
	}
	
	private final String getInputJson() throws XmlMappingException, IOException {
		
		return clusterMessageConverter.inputObjectToClusterMessage(getInput());
	}
		
	@Test
	public void serializeInput() throws XmlMappingException, IOException {

		String inputJson = getInputJson();

		logger.debug("serialized clusterInputMessage: " + inputJson);

		assertNotNull(inputJson);
	}
	
	@Test
	public void serializeAndDeserializeInput() throws XmlMappingException, IOException {
		
		try {

			String clusterInputMessage = clusterMessageConverter.inputObjectToClusterMessage(getInput());

			logger.debug("serialized clusterInputMessage: " + clusterInputMessage);
			
			assertNotNull(clusterInputMessage);
			
			ImmutablePair<String, Input> dInput = clusterMessageConverter.inputClusterMessageToInput(clusterInputMessage.getBytes());
			
			logger.debug("deserialized input port: " + dInput.getRight().getPort());
			
			assertEquals(9999, dInput.getRight().getPort());
			assertEquals(2, dInput.getRight().getCoreVersion());

		} catch (Exception e) {
			logger.error("exception converting input to input cluster message", e);
			throw new RuntimeException(e);
		}
	}
	
	@Test
	public void rolSerializeDeserialize() throws InvalidProtocolBufferException {
		
		String dummyProgram = "dummy";
		
		ROL rol = ROL.newBuilder().setProgram(dummyProgram).build();
		
		ROL rolDeserialized = ROL.parseFrom(rol.toByteArray());
		
		assertEquals(rolDeserialized.getProgram(), rol.getProgram());
		
		logger.debug("rol proto string: " + new String(rol.toByteArray(), Charsets.UTF_8));
	}
	
	@Test
	public void rolAddInput() throws XmlMappingException, IOException {

		String rolProgramCreateInput = CREATE_INPUT_ROL + clusterMessageConverter.inputObjectToClusterMessage(getInput()) + ";";
		
		logger.debug("ROL create input: " + rolProgramCreateInput);

		ROL originalRol = ROL.newBuilder().setProgram(rolProgramCreateInput).build();
		
		ROL rolDeserialized = ROL.parseFrom(originalRol.toByteArray());

		// interpret and execute the ROL program
		RolLexer lexer = new RolLexer(new ANTLRInputStream(rolDeserialized.getProgram()));

		CommonTokenStream tokens = new CommonTokenStream(lexer);

		RolParser parser = new RolParser(tokens);
		parser.setErrorHandler(new BailErrorStrategy());

		// parse the ROL program
		ParseTree rolParseTree = parser.program();

		requireNonNull(rolParseTree, "parsed ROL program");
		
		logger.debug("about to parse and visit ROL program "  + rolProgramCreateInput);
	
		AtomicReference<String> resourceRef = new AtomicReference<>();
		AtomicReference<ImmutablePair<String, Input>> nodeIdInputPairRef = new AtomicReference<>();

		String parseResult = new ClusterControlRolVisitor((resource, operation, parameters) -> {
			logger.debug(" evaluating " + operation + " on " + resource + " given " + parameters);

			if (parameters instanceof String && !Strings.isNullOrEmpty((String) parameters)) {

				switch(resource.toLowerCase()) {
				case "input":
					try {
						
						resourceRef.set(resource);
						nodeIdInputPairRef.set(clusterMessageConverter.inputClusterMessageToInput(((String) parameters).getBytes()));
						
						if (logger.isDebugEnabled()) {
							logger.debug("evaluating parameters for {" + resource + ", " + operation + "} " + nodeIdInputPairRef.get());
						}
						
					} catch (IOException e) {
						throw new TakException(e);
					}
					break;
				default:
					throw new UnsupportedOperationException("invalid resource for cluster control message: " + resource);
				};
			}

			return resource;
		}).visit(rolParseTree);

		logger.debug("parseResult: " + parseResult);
		
		assertEquals(resourceRef.get(), "input");
		assertEquals(nodeIdInputPairRef.get().getRight().getPort(), 9999);
		assertEquals(nodeIdInputPairRef.get().getRight().getCoreVersion(), 2);
	}
	
	@Test
	public void rolDeleteInput() throws XmlMappingException, IOException {
		
		String rolProgramDeleteInput = DELETE_INPUT_ROL + "myinput;";

		logger.debug("ROL delete input: " + rolProgramDeleteInput);

		ROL originalRol = ROL.newBuilder().setProgram(rolProgramDeleteInput).build();
		
		ROL rolDeserialized = ROL.parseFrom(originalRol.toByteArray());

		// interpret and execute the ROL program
		RolLexer lexer = new RolLexer(new ANTLRInputStream(rolDeserialized.getProgram()));

		CommonTokenStream tokens = new CommonTokenStream(lexer);

		RolParser parser = new RolParser(tokens);
		parser.setErrorHandler(new BailErrorStrategy());

		// parse the ROL program
		ParseTree rolParseTree = parser.program();

		requireNonNull(rolParseTree, "parsed ROL program");
		
		logger.debug("about to parse and visit ROL program "  + rolProgramDeleteInput);
	
		AtomicReference<String> inputName = new AtomicReference<>();

		String parseResult = new ClusterControlRolVisitor((resource, operation, parameters) -> {
			logger.debug(" evaluating " + operation + " on " + resource + " given " + parameters);

			if (parameters instanceof String && !Strings.isNullOrEmpty((String) parameters)) {

				switch(resource.toLowerCase()) {
				case "input":
					try {
						
						logger.debug("operation: " + operation + " parameters text: " + parameters);
						
						inputName.set((String) parameters);
						
					} catch (Exception e) {
						throw new TakException(e);
					}
					break;
				default:
					throw new UnsupportedOperationException("invalid resource for cluster control message: " + resource);
				};
			}

			return resource;
		}).visit(rolParseTree);

		logger.debug("parseResult: " + parseResult);
		
		assertEquals(inputName.get(), "myinput");
	}
	
	@Test
	public void rolModifyInput() throws XmlMappingException, IOException {
		
		String rolProgramModifyInput = "update input" + clusterMessageConverter.inputObjectToClusterMessage(getInput()) + ";";

		logger.debug("ROL update input: " + rolProgramModifyInput);

		ROL originalRol = ROL.newBuilder().setProgram(rolProgramModifyInput).build();
		
		ROL rolDeserialized = ROL.parseFrom(originalRol.toByteArray());

		// interpret and execute the ROL program
		RolLexer lexer = new RolLexer(new ANTLRInputStream(rolDeserialized.getProgram()));

		CommonTokenStream tokens = new CommonTokenStream(lexer);

		RolParser parser = new RolParser(tokens);
		parser.setErrorHandler(new BailErrorStrategy());

		// parse the ROL program
		ParseTree rolParseTree = parser.program();

		requireNonNull(rolParseTree, "parsed ROL program");
		
		logger.debug("about to parse and visit ROL program "  + rolProgramModifyInput);
	
		AtomicReference<String> op = new AtomicReference<>();
		AtomicReference<Input> modifiedInput = new AtomicReference<>();


		String parseResult = new ClusterControlRolVisitor((resource, operation, parameters) -> {
			logger.debug(" evaluating " + operation + " on " + resource + " given " + parameters);

			if (parameters instanceof String && !Strings.isNullOrEmpty((String) parameters)) {

				switch(resource.toLowerCase()) {
				case "input":
					try {
						
						logger.debug("operation: " + operation + " parameters text: " + parameters);
						
						modifiedInput.set(clusterMessageConverter.inputClusterMessageToInput(((String) parameters).getBytes()).getRight());

						
						op.set(operation);
						
					} catch (Exception e) {
						throw new TakException(e);
					}
					break;
				default:
					throw new UnsupportedOperationException("invalid resource for cluster control message: " + resource);
				};
			}

			return resource;
		}).visit(rolParseTree);

		assertEquals(op.get(), "update");
		assertEquals(modifiedInput.get().getName(), "myinput");
	}
}
