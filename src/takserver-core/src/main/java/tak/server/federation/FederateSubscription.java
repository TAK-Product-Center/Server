

package tak.server.federation;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.FederateHttpClientExecutor;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.dom4j.Attribute;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atakmap.Tak.FederatedEvent;
import com.bbn.marti.config.Configuration;
import com.bbn.marti.config.Federation.Federate;
import com.bbn.marti.groups.GroupFederationUtil;
import com.bbn.marti.nio.channel.connections.TcpChannelHandler;
import com.bbn.marti.nio.protocol.Protocol;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.service.RepositoryService;
import com.bbn.marti.service.Resources;
import com.bbn.marti.service.Subscription;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;

import tak.server.cluster.ClusterManager;
import tak.server.cot.CotEventContainer;

public class FederateSubscription extends Subscription {
    private static final Logger logger = LoggerFactory.getLogger(FederateSubscription.class);
    private static final long serialVersionUID = 3437435640686758857L;
    final Set<String> localContactUid = new HashSet<>();
    private boolean shareAlerts = true;
    private Protocol<FederatedEvent> protoEncoder;
    
    public void submit(final CotEventContainer toSend, long hitTime) {
    	
		// for benchmarking only - disable message dissemination to clients
		if (!config.getDissemination().isEnabled()) {
			return;
		}
    	
		totalSubmitted.incrementAndGet();
        
        if (toSend.hasContextKey(GroupFederationUtil.FEDERATE_ID_KEY)) {
        	if (logger.isDebugEnabled())
        		logger.debug("not federating federated message");
        	
            return;
        }
        
        // increment the hit time in the super class
        super.incHit(hitTime);

        if (toSend.getType().equals("b-f-t-r")) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("sending v1 fed mission package announce " + toSend);
        	}
            
            sendMissionPackage(toSend);
        } else if (toSend.getType().compareTo("t-x-d-d") == 0) {
            removeLocalContact(toSend.getUid());
            
            FederatedEvent fedEventDelContact = ProtoBufHelper.getInstance().delContact2protoBuf(toSend);
            
            if (logger.isDebugEnabled()) {
            	logger.debug("sending proto contact delete" + fedEventDelContact);
            }
            
            this.protoEncoder.write(fedEventDelContact, this.handler);
        } else {
            try {
                
            	if (logger.isDebugEnabled()) {
            		logger.debug("CoT to convert to proto: " + toSend.asXml());
            	}
                
                FederatedEvent fEvent = FederatedEvent.newBuilder()
                        .setEvent(ProtoBufHelper.getInstance().cot2protoBuf(toSend))
                        .build();
                
                if (logger.isDebugEnabled()) {
                	logger.debug("sending proto federated " + fEvent + " lat: " + fEvent.getEvent().getLat() + " lon: " + fEvent.getEvent().getLon() + " le: " + fEvent.getEvent().getLe());
                }
                
                this.protoEncoder.write(fEvent, this.handler);
            } catch (NumberFormatException e) {
                // ignore people putting NaN as their elevation
            } catch (IllegalArgumentException e) {
            	if (logger.isDebugEnabled()) {
            		logger.debug("invalid message", e);
            	}
            }
        }
        
        try {
        	ClusterManager.countMessageSent();
			
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception tracking clustered message sent count", e);
			}
		}
    }

    public void submitLocalContact(FederatedEvent e, long hitTime) {
        if (localContactUid.contains(e.getContact().getUid())) {
            return;
        } else {
            
        	localContactUid.add(e.getContact().getUid());

            super.incHit(hitTime);
            
            if (logger.isDebugEnabled()) {
            	logger.debug("send proto contact message: " + e + " operation: " + e.getContact().getOperation() + " " + e.getContact().getOperationValue());
            }
            
            this.protoEncoder.write(e, this.handler);

        }
    }
    
    public void removeLocalContact(String uid) {
        localContactUid.remove(uid);
    }

    @Override
    public String toString() {
        return "FederateSubscription [" + super.toString() + "]";
    }

    public void setProtoEncoder(Protocol<FederatedEvent> protoEncoder) {
        this.protoEncoder = protoEncoder;
    }

    public void setShareAlerts(boolean s) {
        shareAlerts = s;
    }

    public boolean getShareAlerts() {
        return shareAlerts;
    }

    protected void sendMissionPackage(final CotEventContainer toSend) {
        // process asynchronously
        Resources.fedMissionPackageExecutor.execute(new Runnable() { @Override public void run() {

            Element fileshare = DocumentHelper.makeElement(toSend.getDocument(), "event/detail/fileshare");
            
            if (fileshare != null) {
                
                Attribute hashAttribute = fileshare.attribute("sha256");
                Attribute filenameAttribute = fileshare.attribute("filename");
                
                if (hashAttribute == null || filenameAttribute == null) {
                    throw new TakException("hash or filename missing from mission package announcement - can't federate this " + fileshare);
                }
                
                final String hash = (String) hashAttribute.getData();
                final String filename = (String) filenameAttribute.getData();

                if (Strings.isNullOrEmpty(hash) || Strings.isNullOrEmpty(filename)) {
                    throw new TakException("invalid mission package announce CoT message - empty hash or filename");
                }

                final int port = DistributedConfiguration.getInstance().getRemoteConfiguration().getFederation().getFederationServer().getHttpsPort();

                String missionPackageUrl = "";

                try {
                    // this is an HTTP executor, not a thread pool executor
                    final Executor federateHttpConnectionPool = FederateHttpClientExecutor.newInstance();

                    if (!(FederateSubscription.this.getHandler() instanceof TcpChannelHandler)) {
                        throw new TakException("Invalid federate channel handler type");
                    }

                    final String remoteAddress = ((TcpChannelHandler) FederateSubscription.this.getHandler()).getConnectionInfo().getAddress();

                    String fedGetMissionPackageUrl = "https://" + remoteAddress + ":" + port + "/Marti/sync/missionquery?hash=" + hash;

                    if (logger.isDebugEnabled()) {
                    	logger.debug("mp hash: " + hash + " filename: " + filename + " address: " + remoteAddress + " port: " + port + " url: " + fedGetMissionPackageUrl);
                    }

                    missionPackageUrl = federateHttpConnectionPool.execute(
                            Request.Get(fedGetMissionPackageUrl).useExpectContinue().version(HttpVersion.HTTP_1_1))
                            .handleResponse(new ResponseHandler<String>() { @Override public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {

                                int code = response.getStatusLine().getStatusCode();
                                String reason = response.getStatusLine().getReasonPhrase();
                                String body = new String(ByteStreams.toByteArray(response.getEntity().getContent()));

                                if (logger.isDebugEnabled()) {
                                	logger.debug("code: " + code + " reason: " + reason + " body: " + body);
                                }

                                if (code / 100 == 4) {
                                	if (logger.isDebugEnabled()) {
                                		logger.debug("mission package for hash " + hash + " not found");
                                	}

                                    byte[] data = null;

                                    try {
                                        // access the local database directly to get the mission package contents
                                        data = RepositoryService.getInstance().getContentByHash(hash);
                                    } catch (SQLException | NamingException e) {
                                    	if (logger.isDebugEnabled()) {
                                    		logger.debug("exception mission package from local database " + e.getMessage(), e);
                                    	}
                                    }

                                    if (data == null) {
                                        throw new TakException("Local mission package not found for hash " + hash);
                                    }

                                    String fedPostMissionPackageUrl = "https://" + remoteAddress + ":" + port + "/Marti/sync/missionupload?hash=" + hash + "&filename=" + URLEncoder.encode(filename, "UTF-8");

                                    if (logger.isDebugEnabled()) {
                                    	logger.debug("mission package data: " + data.length + " posting mission package to federate url " + fedPostMissionPackageUrl);
                                    }

                                    ByteArrayBody bodyPart = new ByteArrayBody(data, ContentType.create("application/zip"), filename);

                                    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                                    builder.setContentType(ContentType.MULTIPART_FORM_DATA);
                                    builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                                    builder.addPart("assetfile", bodyPart); // use the same part name that android uses - "assetfile"
                                    HttpEntity entity = builder.build();

                                    // now post the mission package to the federate, as a multi-part mime file
                                    String result = federateHttpConnectionPool.execute(Request.Post(fedPostMissionPackageUrl)
                                            .useExpectContinue()
                                            .version(HttpVersion.HTTP_1_1)
                                            .body(entity)).handleResponse(new ResponseHandler<String>() { @Override public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                                        int code = response.getStatusLine().getStatusCode();
                                        String reason = response.getStatusLine().getReasonPhrase();
                                        String body = new String(ByteStreams.toByteArray(response.getEntity().getContent()));

                                        if (logger.isDebugEnabled()) {
                                        	logger.debug("code: " + code + " reason: " + reason + " body: " + body);
                                        }

                                        return body;
                                    }});

                                    return result;
                                }

                                else if (code == 200) {
                                    logger.debug("federate already has mission package - url: " + body);

                                    return body;
                                } else {
                                    logger.warn("unexpected error response from federate - code: " + code + " reason: " + reason + " body: " + body);

                                    return "";
                                }
                            }});

                    if (logger.isDebugEnabled()) {
                    	logger.debug("mission package url from federate: " + missionPackageUrl);
                    }
                } catch (Exception e) {
                    throw new TakException("exception processing federate mission package " + e.getMessage(), e);
                }

                if (logger.isDebugEnabled()) {
                	logger.debug("fed mission package url: " + missionPackageUrl);
                }

                // put the mission package url in the message
                fileshare.addAttribute("senderUrl", missionPackageUrl);

                // send the message to the federate now that mission package processing is complete
                try {
                    FederatedEvent fEvent = FederatedEvent.newBuilder()
                            .setEvent(ProtoBufHelper.getInstance().cot2protoBuf(toSend))
                            .build();
                    
                    if (logger.isDebugEnabled()) {
                    	logger.debug("sending proto mission package announce " + fEvent);
                    }
                    
                    FederateSubscription.this.protoEncoder.write(fEvent, FederateSubscription.this.handler);
                } catch (NumberFormatException e) {
                    // ignore people putting NaN as their elevation
                } catch (IllegalArgumentException e) {
                	if (logger.isDebugEnabled()) {
                		logger.debug("invalid message", e);
                	}
                }
            } else {
                logger.warn("mission package announce CoT invalid - detail/fileshare element not present");
            }
        }});
    }
    
    protected Configuration getConfiguration() {
    	return DistributedConfiguration.getInstance().getRemoteConfiguration();
    }
    
    //TODO maintain a local mapping structure for federate to federateId that gets reset when the cache updates so we dont have to loop the list each time
    protected Federate getFederate(String federateId) {
    	return getConfiguration()
    			.getFederation()
    			.getFederate()
    			.stream()
    			.filter(f -> f.getId().equals(federateId))
    			.findFirst()
    			.get();
    }
}
