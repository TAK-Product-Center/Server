package tak.server.federation.hub.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class FederationHubDatabase {
	private static final Logger logger = LoggerFactory.getLogger(FederationHubDatabase.class);

	private AbstractMongoClientConfiguration mongo;
	private MongoClient mongoClient;
	private MongoDatabase cotDatabase;
	private boolean dbIsConnected = false;
	
	// no-op for when db is disabled
	public FederationHubDatabase() {}
	
	public FederationHubDatabase(String username, String password, String host, int port) {
		try {
			mongo = new AbstractMongoClientConfiguration() {

				@Override
				protected String getDatabaseName() {
					return "cot";
				}
				
				@Override
				public MongoClient mongoClient() {
				    MongoClient client = MongoClients.create("mongodb://" + username + ":" + password + "@" + host + ":" + port + "/?serverSelectionTimeoutMS=5000&connectTimeoutMS=5000");
				    
				    return client;
				}
				
			};
			
			mongoClient = mongo.mongoClient();
			mongoClient.getDatabase("cot").runCommand(new BasicDBObject("ping", "1"));
			dbIsConnected = true;
		} catch (Exception e) {
			dbIsConnected = false;
			logger.error("Could not initialize mongo connection", e);
		}
	}
	
	public boolean isDBConnected() {
		return dbIsConnected;
	}
	
	public MongoDatabase getDB() {
		if (cotDatabase == null) {
			cotDatabase = mongoClient.getDatabase("cot");
		}
		
		return cotDatabase;
	}

	public MongoClient getClient() {
		return mongoClient;
	}
}
