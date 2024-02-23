package com.bbn.roger.fig;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import io.grpc.Attributes;
import io.grpc.ChannelLogger;
import io.grpc.Grpc;
import io.grpc.InternalChannelz.Security;
import io.grpc.InternalChannelz.Tls;
import io.grpc.SecurityLevel;
import io.grpc.internal.GrpcAttributes;
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.GrpcHttp2ConnectionHandler;
import io.grpc.netty.InternalProtocolNegotiationEvent;
import io.grpc.netty.InternalProtocolNegotiator.ProtocolNegotiator;
import io.grpc.netty.InternalProtocolNegotiators;
import io.grpc.netty.InternalProtocolNegotiators.ProtocolNegotiationHandler;
import io.grpc.netty.InternalWriteBufferingAndExceptionHandlerUtils;
import io.grpc.netty.ProtocolNegotiationEvent;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.OpenSslSessionContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.util.AsciiString;

import javax.net.ssl.SSLSessionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

/*
 *
 * Adapted from gRPC internals. Pulling out key pieces of the puzzle so that we can unlock access to the SSLSession,
 * which lets us get the server's X.509 cert, so that we can identify it and match it up with a federate configuration.
 *
 */
public class FigProtocolNegotiator {

    private final Logger logger = LoggerFactory.getLogger(FigProtocolNegotiator.class);

    private final Propagator<X509Certificate[]> propagator;

    // gRPC identifier for HTTP/2 over TLS
    static final String HTTP2_VERSION = "h2";

    //From https://github.com/grpc/grpc-java/blob/master/netty/src/main/java/io/grpc/netty/GrpcSslContexts.java
    /*
     * The experimental "grpc-exp" string identifies gRPC (and by implication
     * HTTP/2) when used over TLS. This indicates to the server that the client
     * will only send gRPC traffic on the h2 connection and is negotiated in
     * preference to h2 when the client and server support it, but is not
     * standardized. Support for this may be removed at any time.
     */
    private static final String GRPC_EXP_VERSION = "grpc-exp";

    public FigProtocolNegotiator(Propagator<X509Certificate[]> propagator) {

        checkNotNull(propagator, "Null cert propatgator callback");

        this.propagator = propagator;
    }

    public ProtocolNegotiator figTlsProtocolNegotiator(SslContext sslContext, String authority) {
        checkNotNull(sslContext, "sslContext");
        URI uri = GrpcUtil.authorityToUri(checkNotNull(authority, "authority"));
        String host;
        int port;
        if (uri.getHost() != null) {
            host = uri.getHost();
            port = uri.getPort();
        } else {
            /* From gRPC:
             *
             * Implementation note: We pick -1 as the port here rather than deriving it from the original
             * socket address.  The SSL engine doens't use this port number when contacting the remote
             * server, but rather it is used for other things like SSL Session caching.  When an invalid
             * authority is provided (like "bad_cert"), picking the original port and passing it in would
             * mean that the port might used under the assumption that it was correct.   By using -1 here,
             * it forces the SSL implementation to treat it as invalid.
             */
            host = authority;
            port = -1;
        }

        return new FigClientTlsNegotiator(sslContext, host, port);
    }

    private class FigClientTlsNegotiator implements ProtocolNegotiator {
        private final SslContext sslContext;
        private final String host;
        private final int port;

        FigClientTlsNegotiator(SslContext sslContext, String host, int port) {
            this.sslContext = checkNotNull(sslContext);
            this.host = checkNotNull(host);
            this.port = port;
        }

        @VisibleForTesting
        String getHost() {
            return host;
        }

        @VisibleForTesting
        int getPort() {
            return port;
        }

        /**
        @Override
        public Handler newHandler(Http2ConnectionHandler handler) {
            ChannelHandler sslBootstrap = new ChannelHandlerAdapter() {
                @Override
                public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
                    SSLEngine sslEngine = sslContext.newEngine(ctx.alloc(), host, port);
                    SSLParameters sslParams = new SSLParameters();
                    sslParams.setEndpointIdentificationAlgorithm("HTTPS");
                    sslEngine.setSSLParameters(sslParams);
                    ctx.pipeline().replace(this, null, new SslHandler(sslEngine, false));
                }
            };
            return new FigClientBufferUntilTlsNegotiatedHandler(sslBootstrap, handler);
        }
        **/
        @Override
        public ChannelHandler newHandler(GrpcHttp2ConnectionHandler grpcHandler) {
            ChannelHandler gnh = InternalProtocolNegotiators.grpcNegotiationHandler(grpcHandler);
            ChannelHandler figClientTLSNegotiatedHandler = new FigClientBufferUntilTlsNegotiatedHandler(gnh, sslContext, host, port,
                    grpcHandler.getNegotiationLogger());
            ChannelHandler activeHandler = InternalProtocolNegotiators.waitUntilActiveHandler(figClientTLSNegotiatedHandler,
                    grpcHandler.getNegotiationLogger());
            return activeHandler;

        }

        @Override
        public void close() {}

        @Override
        public AsciiString scheme() {
            return AsciiString.of("https");
        }
    }

    private class FigClientBufferUntilTlsNegotiatedHandler extends ProtocolNegotiationHandler {
        SslContext sslContext;
        String host;
        int port;

        FigClientBufferUntilTlsNegotiatedHandler(ChannelHandler next, SslContext sslContext, String host, int port, ChannelLogger logger) {
            super(next, logger);
            this.sslContext = sslContext;
            this.host = host;
            this.port = port;
        }

        @Override
        public void handlerAdded0(ChannelHandlerContext ctx) {
            SSLEngine sslEngine = sslContext.newEngine(ctx.alloc(), host, port);
            SSLParameters sslParams = sslEngine.getSSLParameters();
            sslParams.setEndpointIdentificationAlgorithm("HTTPS");
            sslEngine.setSSLParameters(sslParams);
            ctx.pipeline().addBefore(ctx.name(), null,  new SslHandler(sslEngine, false));
        }

        @Override
        public void userEventTriggered0(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof SslHandshakeCompletionEvent) {
                SslHandshakeCompletionEvent handshakeEvent = (SslHandshakeCompletionEvent) evt;
                if (handshakeEvent.isSuccess()) {
                    SslHandler handler = ctx.pipeline().get(SslHandler.class);
                    
                    if (logger.isDebugEnabled()) {
                    	logger.debug("handler application protocol: " + handler.applicationProtocol());
                    }
                    
                    //Check for grpc-exp too. Need it in order to work with new GRPC (grpc-exp still is HTTP/2)
                    if (HTTP2_VERSION.equals(handler.applicationProtocol()) || GRPC_EXP_VERSION.equals(handler.applicationProtocol())) {
                        // Successfully negotiated the protocol.
                        if (logger.isDebugEnabled()) {
                            logger.debug("FIG: gGRPC Netty HTTP/2 TLS negotiation succeeded.");
                        }

//                        SSLSessionContext sessionContext = sslContext.sessionContext();
//                        if (sessionContext instanceof OpenSslSessionContext) {
//                            logger.debug("Disabling cache");
//                            OpenSslSessionContext ossc = (OpenSslSessionContext) sessionContext;
//                            ossc.setSessionCacheEnabled(false);
//                            logger.debug("Cache disabled");
//                        } else {
//                            logger.info("Cannot disable cache");
//                        }

                        // validate as we go
                        SSLSession sslSession = checkNotNull(checkNotNull(checkNotNull(handler).engine(), "FIG Netty handler SSLEngine").getSession(), "FIG server SSL Session");

                        checkNotNull(sslSession.getPeerCertificates());
                        checkState(sslSession.getPeerCertificates().length > 0);

                        X509Certificate[] certs = new X509Certificate[sslSession.getPeerCertificates().length];
                        for (int i = 0; i < sslSession.getPeerCertificates().length; i++) {
                        	certs[i] = (X509Certificate) sslSession.getPeerCertificates()[i];
                        }
                        
                        checkNotNull(certs[0], "FIG server cert");
                        checkNotNull(certs[1], "FIG ca cert");
       
                        if (logger.isDebugEnabled()) {
                            logger.debug("FIG server cert: " + certs[0]);
                            logger.debug("FIG ca cert: " + certs[1]);
                        }
                        
                        // pass the certs to the callback
                        propagator.propogate(certs);
                        this.propagateTlsComplete(ctx, sslSession);
                        InternalWriteBufferingAndExceptionHandlerUtils.writeBufferingAndRemove(ctx.channel());
                    } else {
                        String message = "FIG: failed ALPN negotiation: Unable to find compatible protocol for handler " + handler.applicationProtocol();
                        logger.info(message);
                        ctx.fireExceptionCaught(new Exception(message));
                    }
                } else {
                    ctx.fireExceptionCaught(handshakeEvent.cause());
                }
            }
            super.userEventTriggered0(ctx, evt);
        }

        private void propagateTlsComplete(ChannelHandlerContext ctx, SSLSession session) {
            Security security = new Security(new Tls(session));
            Attributes attrs = InternalProtocolNegotiationEvent.getAttributes(getProtocolNegotiationEvent()).toBuilder()
                    .set(GrpcAttributes.ATTR_SECURITY_LEVEL, SecurityLevel.PRIVACY_AND_INTEGRITY)
                    .set(Grpc.TRANSPORT_ATTR_SSL_SESSION, session)
                    .build();
            ProtocolNegotiationEvent existingPne = InternalProtocolNegotiationEvent.withAttributes(getProtocolNegotiationEvent(), attrs);
            this.replaceProtocolNegotiationEvent(InternalProtocolNegotiationEvent.withSecurity(existingPne, security));
            this.fireProtocolNegotiationEvent(ctx);
        }
    }
}



