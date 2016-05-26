/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.netty.example.http2.helloworld.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import java.util.Arrays;

import static io.netty.handler.logging.LogLevel.INFO;

/**
 * A HTTP/2 Server that responds to requests with a Hello World. Once started, you can test the
 * server with the example client.
 */
public final class Http2DebugServer {
    static final int PORT = 443;

    public static void main(String[] args) throws Exception {
        // Configure SSL.
        SslProvider provider = OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK;
//        provider = SslProvider.JDK;
        SelfSignedCertificate ssc = new SelfSignedCertificate("scottmitch.ddns.net");
        final SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .sslProvider(provider)
                /* NOTE: the cipher filter may not include all ciphers required by the HTTP/2 specification.
                 * Please refer to the HTTP/2 specification for cipher requirements. */
                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .applicationProtocolConfig(new ApplicationProtocolConfig(
                        Protocol.ALPN,
                        // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
                        SelectorFailureBehavior.NO_ADVERTISE,
                        // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
                        SelectedListenerFailureBehavior.ACCEPT,
                        ApplicationProtocolNames.HTTP_2,
                        ApplicationProtocolNames.HTTP_1_1))
                .clientAuth(ClientAuth.OPTIONAL)
                .ciphers(Arrays.asList(new String[] {
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                        "TLS_EMPTY_RENEGOTIATION_INFO_SCSV"}))
                .build();
        // Configure the server.
        EventLoopGroup group = new EpollEventLoopGroup(4);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(group)
             .channel(EpollServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<Channel>() {
                 @Override
                 protected void initChannel(Channel ch) throws Exception {
                     ch.pipeline().addLast(new LoggingHandler(Http2DebugServer.class, LogLevel.TRACE));
                     SslHandler sslHandler = sslCtx.newHandler(ch.alloc());
                     SSLEngine engine = sslHandler.engine();
                     engine.setEnabledProtocols(new String[] { "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2" });
                     ch.pipeline().addLast(sslHandler);
                     ch.pipeline().addLast(new DebugHttp2HandlerBuilder().build());
                 }
             });

            Channel ch = b.bind(PORT).sync().channel();

            System.err.println("Open your HTTP/2-enabled web browser and navigate to https://127.0.0.1:" + PORT + '/');

            ch.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    private static final class DebugHttp2HandlerBuilder
            extends AbstractHttp2ConnectionHandlerBuilder<Http2ConnectionHandler, DebugHttp2HandlerBuilder> {

        private static final Http2FrameLogger logger = new Http2FrameLogger(INFO, DebugHttp2HandlerBuilder.class);

        public DebugHttp2HandlerBuilder() {
            frameLogger(logger);
            server(true);
        }

        @Override
        public Http2ConnectionHandler build() {
            return super.build();
        }

        @Override
        protected Http2ConnectionHandler build(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder,
                                               Http2Settings initialSettings) {

            DebugHttp2Handler handler = new DebugHttp2Handler(decoder, encoder, initialSettings);
            frameListener(handler);
            return handler;
        }
    }

    private static final class DebugHttp2Handler extends Http2ConnectionHandler implements Http2FrameListener {
        DebugHttp2Handler(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder,
                               Http2Settings initialSettings) {
            super(decoder, encoder, initialSettings);
        }

        @Override
        public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream) throws Http2Exception {
            return data.readableBytes() + padding;
        }

        @Override
        public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endOfStream) throws Http2Exception {
            encoder().writeHeaders(ctx, streamId, new DefaultHttp2Headers(false).status(HttpResponseStatus.OK.codeAsText()), 0, true, ctx.newPromise());
        }

        @Override
        public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream) throws Http2Exception {
            encoder().writeHeaders(ctx, streamId, new DefaultHttp2Headers(false).status(HttpResponseStatus.OK.codeAsText()), 0, true, ctx.newPromise());
        }

        @Override
        public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency, short weight, boolean exclusive) throws Http2Exception {

        }

        @Override
        public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) throws Http2Exception {

        }

        @Override
        public void onSettingsAckRead(ChannelHandlerContext ctx) throws Http2Exception {

        }

        @Override
        public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) throws Http2Exception {

        }

        @Override
        public void onPingRead(ChannelHandlerContext ctx, ByteBuf data) throws Http2Exception {

        }

        @Override
        public void onPingAckRead(ChannelHandlerContext ctx, ByteBuf data) throws Http2Exception {

        }

        @Override
        public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId, int promisedStreamId, Http2Headers headers, int padding) throws Http2Exception {

        }

        @Override
        public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData) throws Http2Exception {

        }

        @Override
        public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement) throws Http2Exception {

        }

        @Override
        public void onUnknownFrame(ChannelHandlerContext ctx, byte frameType, int streamId, Http2Flags flags, ByteBuf payload) throws Http2Exception {

        }
    }
}
