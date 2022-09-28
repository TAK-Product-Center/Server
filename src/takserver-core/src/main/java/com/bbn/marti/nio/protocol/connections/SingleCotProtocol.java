

package com.bbn.marti.nio.protocol.connections;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;

import com.bbn.cot.CotParserCreator;
import com.bbn.cot.filter.DataFeedFilter;
import com.bbn.marti.config.DataFeed;
import com.bbn.marti.config.Input;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.nio.protocol.ProtocolInstantiator;
import com.bbn.marti.nio.protocol.base.AbstractBroadcastingProtocol;
import com.bbn.marti.util.concurrent.future.AsyncFuture;

import tak.server.cot.CotEventContainer;
import tak.server.cot.CotParser;

/**
 * Cursor-on-Target (CoT) protocol that parses a single message from a packet. Intended for connectionless protocols - no context is kept for any input handlers,
 * and onConnect and onClose are meaningless calls w.r.t. this object -- they are still passed onwards, to protocol listeners.
 * 	
 */
public class SingleCotProtocol extends AbstractBroadcastingProtocol<CotEventContainer> {
	
	private ThreadLocal<CotParser> cotParser = new ThreadLocal<>();
	
    private final static Logger log = Logger.getLogger(SingleCotProtocol.class);
    
	/**
	* A static inner class that statically returns an instance of the 
	* outer class.
	*/
	public final static ProtocolInstantiator<CotEventContainer> singleCotInstantiator = new ProtocolInstantiator<CotEventContainer>() {
		public SingleCotProtocol newInstance(ChannelHandler handler) { return new SingleCotProtocol(); }
        public String toString() { return "CoT_packet_protocol_builder"; }
	};

	private final static Charset charset = Charset.forName("UTF-8");

    @Override
	public void onDataReceived(ByteBuffer buffer, ChannelHandler handler) {
    	if (cotParser.get() == null) {
			cotParser.set(CotParserCreator.newInstance());
		}
    	
    	CotEventContainer cot = byteBufToCot(buffer, handler, cotParser.get());
			
    	if (cot != null) {
    		broadcastDataReceived(cot, handler);
    	}
	}

    @Override
	public AsyncFuture<Integer> write(CotEventContainer cot, ChannelHandler handler) {
		return handler.write(cot.getOrInstantiateBufferEncoding(this.charset));
	}

    @Override
	public void onConnect(ChannelHandler handler) {
        log.trace(String.format(
            "%s received connect signal -- handler: %s", 
            this,
            handler
        ));
    
		broadcastOnConnect(handler);
	}

    @Override
	public void onInboundClose(ChannelHandler handler) {
        log.trace(String.format(
            "%s received network close signal -- handler: %s", 
            this,
            handler
        ));    
    
		broadcastInboundClose(handler);
	}

    @Override
	public void onOutboundClose(ChannelHandler handler) {
        log.trace(String.format(
            "%s received application close signal -- handler: %s", 
            this,
            handler
        ));
            
		broadcastOutboundClose(handler);
	}

    @Override
	public String toString() {
        return "server_packet_CoT";
	}
    
    public static CotEventContainer byteBufToCot(ByteBuffer buffer, ChannelHandler handler, CotParser cotParser) {
        int binaryLength = buffer.remaining();
		String strData = charset.decode(buffer).toString();

		if (log.isTraceEnabled()) {
			log.trace(
					String.format("%s received network data -- handler: %s binary_data_length: %d char_data_length: %d",
							SingleCotProtocol.class, handler, binaryLength, strData.length()));
		}
        
		Document doc = null;
		try {			
			doc = cotParser.parse(strData);
		} catch (DocumentException e) {
			// TODO: notify somebody... may want to filter out messages from a given party if we get too many
		    if (log.isTraceEnabled()) {
		        log.trace("received single message packet data: " + strData);
		    }
			log.debug("Error attempting to parse single message packet from " + handler.toString());
		}
		
		if (doc != null) {
			CotEventContainer cot = new CotEventContainer(doc);
			
			if (handler instanceof AbstractBroadcastingChannelHandler) {
            	Input input = ((AbstractBroadcastingChannelHandler) handler).getInput();
            	if (input != null && input instanceof DataFeed) {
            		DataFeedFilter.getInstance().filter(cot, (DataFeed) input);
            	}
            }
			return cot;
		}

		return null;
    }
}
