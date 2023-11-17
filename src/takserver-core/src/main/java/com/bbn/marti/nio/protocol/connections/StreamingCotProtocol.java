

package com.bbn.marti.nio.protocol.connections;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.cot.CotParserCreator;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.protocol.ProtocolInstantiator;
import com.bbn.marti.nio.protocol.base.AbstractBroadcastingProtocol;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.google.common.base.Charsets;

import tak.server.cot.CotEventContainer;
import tak.server.cot.CotParser;

/**
 * Cursor-on-Target (CoT) protocol that re-uses a socket instead of opening and closing it between each message.
 *
 * In the network -{@literal >} application direction, we decode a series of CoT messages laid head-to-tail, without any
 * headers or delimiters that might indicate message boundaries and potentially make our parsing methodology
 * far more efficient. Such is life.
 *
 * In the application -{@literal >} network direction, we encode a single message to its byte representation and pass
 * it onward, to the channel handler.
 *
 */
public class StreamingCotProtocol extends AbstractBroadcastingProtocol<CotEventContainer> {
	private final static Logger log = LoggerFactory.getLogger(StreamingCotProtocol.class);

	/**
	 * A static, inner factory class that returns a new instance of the outer.
	 */
	public final static ProtocolInstantiator<CotEventContainer> streamingCotInstantiator = new ProtocolInstantiator<CotEventContainer>() {
		@Override
		public StreamingCotProtocol newInstance(ChannelHandler handler) { return new StreamingCotProtocol(); }
		public String toString() { return "CoT_streaming_protocol_builder"; }
	};

	protected final static Charset charset = Charsets.UTF_8; // byte encoding/decoding character set
	private final static int INDIVIDUAL_COT_MSG_SIZE_LIMIT = 8388608; // 8MB of characters -- TODO: check -- point at which we clear the string builder
	private final static String START_OF_COT_MSG_STR = "<event";     // string that we search for, marks a message start
	private final static String END_OF_COT_MSG_STR = "</event>";      // string that we search for, marks a message end
	private final static int BUFFER_TRIM_SIZE = 1000000; 			  // 1MB of characters -- point at which we trim the string builder

	private volatile StringBuffer messageStringBuffer; // string builder for aggregating a message received over multiple reads
	private volatile CotParser parser;      // parser for converting string to cot
	private volatile boolean outboundClosed = false;

	/**
	 * Called when the handler first connects.
	 *
	 * Instantiate our parser/message buffer.
	 */
	@Override
	public void onConnect(ChannelHandler handler) {
		Assertion.notNull(handler);

		if (log.isTraceEnabled()) {
			log.trace(String.format(
					"%s received connect signal -- handler: %s",
					this,
					handler
					));
		}

		this.messageStringBuffer = new StringBuffer();
		this.parser = CotParserCreator.newInstance();

		// notify our listeners
		super.broadcastOnConnect(handler, this);
	}

	/**
	 * Called when the handler receives data from the network.
	 *
	 * Append data to the buffer and try to parse, sending any complete
	 * messages onto the protocol listeners.
	 */
	@Override
	public void onDataReceived(ByteBuffer buffer, ChannelHandler handler) {
		Assertion.areNotNull(buffer, handler);
		Assertion.condition(buffer.hasRemaining(), "Received trivial data container from network");

		// save length for trace message
		int binaryLength = buffer.remaining();

		CharSequence newData = charset.decode(buffer);


		StringBuffer messageStringBuilder = (this.messageStringBuffer == null ? new StringBuffer() : this.messageStringBuffer);
		CotParser parser = (this.parser == null ? CotParserCreator.newInstance() : this.parser);

		List<CotEventContainer> msgs = add(messageStringBuilder, newData, parser, handler);



		if (log.isTraceEnabled()) {
			log.trace(String.format(
					"%s received network data -- handler: %s binary_data_length: %d char_data_length: %d messages_produced: %d",
					this,
					handler,
					binaryLength,
					newData.length(),
					msgs.size()
					));
		}

		if (msgs.size() > 0) {
			super.broadcastDataReceived(msgs, handler, this);
		}

	}

	/**
	 * Called when the application wants to send data to the network
	 *
	 * Convert data to its byte format, hand data to the channel's handler.
	 */
	@Override
	public AsyncFuture<Integer> write(CotEventContainer data, ChannelHandler handler) {
		Assertion.condition(!outboundClosed);
		Assertion.areNotNull(data, handler);

		ByteBuffer binaryData = data.getOrInstantiateBufferEncoding(this.charset);

		if (log.isTraceEnabled()) {
			log.trace(String.format(
					"%s writing application data -- handler: %s data_length: %d data: %s",
					this,
					handler,
					binaryData.remaining(),
					data.partial()
					));
		}

		return handler.write(binaryData);
	}

	/**
	 * Called when the handler has finished propagating
	 * data coming from the network, signifies an EOS
	 *
	 * void out our buffered structures
	 */
	@Override
	public void onInboundClose(ChannelHandler handler) {

		if (this.messageStringBuffer != null && this.messageStringBuffer.length() > 0) {
			log.warn("Received EOS notification with partial message (" + messageStringBuffer.length() + " bytes) in decode buffer for " + handler + ". Dumping data mercilessly.");
		}

		// void out parser and builder
		this.messageStringBuffer = null;
		this.parser = null;

		if (log.isTraceEnabled()) {
			log.trace(String.format(
					"%s received network close signal -- handler: %s",
					this,
					handler
					));
		}

		// notify the listeners
		super.broadcastInboundClose(handler);
	}

	/**
	 * Called when the handler would like to
	 */
	@Override
	public void onOutboundClose(ChannelHandler handler) {
		Assertion.notNull(handler);

		this.outboundClosed = true;

		if (log.isTraceEnabled()) {
			log.trace(String.format(
					"%s received application close signal -- handler: %s",
					this,
					handler
					));
		}

		// notify listeners
		super.broadcastOutboundClose(handler);
	}

	/**
	 * A static function that appends the given data to an existing
	 * buffer, and checks to see if any new messages can be parsed with
	 * the new buffer state.
	 *
	 * Parses as many complete messages as possible out of the buffer.
	 */
	public static List<CotEventContainer> add(StringBuffer messageStringBuffer, CharSequence newData, CotParser parser, ChannelHandler handler) {

		List<CotEventContainer> results = new LinkedList<CotEventContainer>();

		if (log.isDebugEnabled()) {
			log.debug("StreamingCotProtocol data to add: " + newData);
		}

		// init search pointer (0 for all other iterations, but we start searching after the end of the message)
		// note: subtract the end-tag length to cover end tag spanning multiple frames
		int prevLen = Math.max(0, messageStringBuffer.length() - END_OF_COT_MSG_STR.length());
		messageStringBuffer.append(newData);

		int indexOfEnd = -1;
		while ((indexOfEnd = messageStringBuffer.indexOf(END_OF_COT_MSG_STR, prevLen)) >= 0) {

			// skip anything received ahead of the first cot event (such as <auth> messages sent to anonymous ports)
			int openIndex = messageStringBuffer.indexOf(START_OF_COT_MSG_STR, 0);

			// have some end tag to seek to
			int closeIndex = indexOfEnd + END_OF_COT_MSG_STR.length();
			final String msg = messageStringBuffer.substring(openIndex, closeIndex);

			if (msg.length() <= INDIVIDUAL_COT_MSG_SIZE_LIMIT) {
				// have a message window to parse, within size limits
				try {

					// try to parse this message
					if (log.isTraceEnabled()) {
						log.trace("possible message: " + msg);
					}

					CotEventContainer tmp = new CotEventContainer(parser.parse(msg));
					results.add(tmp);
				} catch (Exception e) {
					// TODO: try to close
					log.warn("Error parsing cot message ", e);
				}
				// delete up to close tag of what we tried to (maybe successfully) parsed
			} else {
				// message closure is too long, even if we could parse it -- chuck out
				if (log.isTraceEnabled()) {
					log.trace("Error parsing CoT message: message too long to parse: " + messageStringBuffer.substring(0, closeIndex));
				}
			}

			// TODO: change so that we don't delete parsed data until the end of our parse attempt...
			// string builder implementation will move all of the contained data to the zero index
			messageStringBuffer.delete(0, closeIndex);

			// END OF WHILE -- have cleared or parsed past the end tag that we found
			prevLen = 0; // reset search finger
		}

		if (messageStringBuffer.length() > INDIVIDUAL_COT_MSG_SIZE_LIMIT) {
			messageStringBuffer.delete(0, messageStringBuffer.length());
		}

		if (messageStringBuffer.capacity() > BUFFER_TRIM_SIZE) {
			// trim buffer size down if we've grown beyond a meg -- don't want to hold on to a giant buffer if we only occasionally get a large message
			messageStringBuffer.trimToSize();
		}

		return results;
	}

	/**
	 * DO NOT put the channel handler in the string -- typically prints out its listener as
	 * part of its toString method
	 */
	@Override
	public String toString() {
		return "server_streaming_CoT";
	}
}
