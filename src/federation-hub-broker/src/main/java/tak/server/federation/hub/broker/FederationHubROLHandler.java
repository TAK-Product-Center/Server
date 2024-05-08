package tak.server.federation.hub.broker;

import static java.util.Objects.requireNonNull;

import java.rmi.RemoteException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atakmap.Tak.ROL;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import mil.af.rl.rol.FederationProcessor;
import mil.af.rl.rol.Resource;
import mil.af.rl.rol.ResourceOperationParameterEvaluator;
import mil.af.rl.rol.RolLexer;
import mil.af.rl.rol.RolParser;
import tak.server.federation.hub.FederationHubResources;
import tak.server.federation.hub.broker.db.FederationHubMissionDisruptionManager;
import tak.server.federation.rol.MissionEnterpriseSyncRolVisitor;

public class FederationHubROLHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(FederationHubROLHandler.class);
	
	private FederationHubMissionDisruptionManager federationHubMissionDisruptionManager;
	
	public FederationHubROLHandler(FederationHubMissionDisruptionManager federationHubMissionDisruptionManager) {
		this.federationHubMissionDisruptionManager = federationHubMissionDisruptionManager;
	}
	
	public void onNewEvent(ROL rol, String streamKey, String federateServerId) throws RemoteException {
		FederationHubResources.rolExecutor.execute(() -> {
			if (rol == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("skipping null ROL message");
				}
				return;
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Got ROL message: " + rol.getProgram() + " for federateServerId " + federateServerId);
			}
			
			// interpret and execute the ROL program
			RolLexer lexer = new RolLexer(new ANTLRInputStream(rol.getProgram()));

			CommonTokenStream tokens = new CommonTokenStream(lexer);

			RolParser parser = new RolParser(tokens);
			parser.setErrorHandler(new BailErrorStrategy());

			// parse the ROL program
			ParseTree rolParseTree = parser.program();

			requireNonNull(rolParseTree, "parsed ROL program");

			final AtomicReference<String> res = new AtomicReference<>();
			final AtomicReference<String> op = new AtomicReference<>();
			final AtomicReference<Object> parameters = new AtomicReference<>();

			new MissionEnterpriseSyncRolVisitor(new ResourceOperationParameterEvaluator<Object, String>() {
				@Override
				public String evaluate(String resource, String operation, Object params) {
					if (logger.isDebugEnabled()) {
						logger.debug(" evaluating " + operation + " on " + resource + " given " + params);
					}

					res.set(resource);
					op.set(operation);
					parameters.set(params);

					return resource;
				}
			}).visit(rolParseTree);

			try {
				new FederationProcessorFactory().newProcessor(res.get(), op.get(), parameters.get(), streamKey, federateServerId).process(rol);
			} catch (Exception e) {
				logger.warn("exception processing incoming ROL", e);
			}
		});
	}
	
	class FederationProcessorFactory {
		FederationProcessor<ROL> newProcessor(String resource, String operation, Object parameters, String streamKey, String federateServerId) {			
			switch (Resource.valueOf(resource.toUpperCase())) {
			case PACKAGE:
				throw new UnsupportedOperationException(
						"federated mission package processing occurs in core - this ROL should not have been sent");
			case MISSION:
				return new FederationMissionProcessor(resource, operation, parameters, streamKey, federateServerId);
			case DATA_FEED:
				return new FederationDataFeedProcessor(resource, operation, parameters, streamKey, federateServerId);
			case RESOURCE:
				return new FederationSyncResourceProcessor(resource, operation, parameters, streamKey,
						federateServerId);
			default:
				throw new IllegalArgumentException("invalid federation processor kind " + resource);
			}
		}
	}

	private class FederationMissionProcessor implements FederationProcessor<ROL> {

		private final String res;
		private final String op;
		private Map<String, Object> parameters;
		private final String streamKey;
		private final String federateServerId;

		FederationMissionProcessor(String res, String op, Object parameters, String streamKey, String federateServerId) {
			this.res = res;
			this.op = op;
			this.parameters = objectMapper().convertValue(parameters, Map.class);
			Iterables.removeIf(this.parameters.values(), Predicates.isNull());
			this.streamKey = streamKey;
			this.federateServerId = federateServerId;
		}

		@Override
		public void process(ROL source) {
			federationHubMissionDisruptionManager.storeRol(source, res, op, parameters, federateServerId);
		}
	}

	private class FederationDataFeedProcessor implements FederationProcessor<ROL> {

		private final String res;
		private final String op;
		private Map<String, Object> parameters;
		private final String streamKey;
		private final String federateServerId;

		FederationDataFeedProcessor(String res, String op, Object parameters, String streamKey, String federateServerId) {
			this.res = res;
			this.op = op;
			this.parameters = objectMapper().convertValue(parameters, Map.class);
			Iterables.removeIf(this.parameters.values(), Predicates.isNull());
			this.streamKey = streamKey;
			this.federateServerId = federateServerId;
		}

		@Override
		public void process(ROL source) {
			federationHubMissionDisruptionManager.storeRol(source, res, op, parameters, federateServerId);
		}
	}

	private class FederationSyncResourceProcessor implements FederationProcessor<ROL> {

		private final String res;
		private final String op;
		private Map<String, Object> parameters;
		private final String streamKey;
		private final String federateServerId;

		FederationSyncResourceProcessor(String res, String op, Object parameters, String streamKey, String federateServerId) {
			this.res = res;
			this.op = op;
			this.parameters = objectMapper().convertValue(parameters, Map.class);
			Iterables.removeIf(this.parameters.values(), Predicates.isNull());
			this.streamKey = streamKey;
			this.federateServerId = federateServerId;
		}

		@Override
		public void process(ROL source) {
			switch (op.toLowerCase(Locale.ENGLISH)) {
			case "create": {
				if (source.getPayloadList().isEmpty()) {
					logger.info("empty resource payload");
					return;
				}

				byte[] content = source.getPayload(0).getData().toByteArray();
				ObjectId resId = federationHubMissionDisruptionManager.addResource(content, parameters);
				federationHubMissionDisruptionManager.storeRol(source, res, op, parameters, federateServerId, resId);
			}
				break;
			default:
				federationHubMissionDisruptionManager.storeRol(source, res, op, parameters, federateServerId);
			}
		}
	}
    
    private ThreadLocal<ObjectMapper> objectMapper = new ThreadLocal<>();
	
	private ObjectMapper objectMapper() {
		if (objectMapper.get() == null) {
			objectMapper.set(new ObjectMapper());
		}
		
		return objectMapper.get();
	}
}
