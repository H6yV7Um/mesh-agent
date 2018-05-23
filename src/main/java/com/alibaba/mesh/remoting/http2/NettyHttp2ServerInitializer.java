package com.alibaba.mesh.remoting.http2;

import com.alibaba.mesh.common.Constants;
import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.utils.StringUtils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.CertificateException;

/**
 * @author yiji
 */
public class NettyHttp2ServerInitializer extends ChannelInitializer<NioSocketChannel> {

    private SslContext sslCtx;
    private int maxHttpContentLength;
    private URL url;

    private static final Logger logger = LoggerFactory.getLogger(NettyHttp2ServerInitializer.class);

    private final HttpServerUpgradeHandler.UpgradeCodecFactory upgradeCodecFactory = new HttpServerUpgradeHandler.UpgradeCodecFactory() {
        @Override
        public HttpServerUpgradeHandler.UpgradeCodec newUpgradeCodec(CharSequence protocol) {
            if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
                return new Http2ServerUpgradeCodec(new NettyHttp2ServerHandlerBuilder().withURL(url).build());
            } else {
                return null;
            }
        }
    };

    public NettyHttp2ServerInitializer(URL url) throws SSLException, CertificateException {

        this.url = url;
        this.maxHttpContentLength = url.getParameter(Constants.MAX_HTTP_CONTENT_BYTES_KEY, Constants.MAX_HTTP_CONTENT_BYTES);

        if (url.getParameter(Constants.SSL_ENABLE_KEY, false)) {
            String certificate = url.getParameter(Constants.SSL_CERTIFICATE_KEY);
            String privateKey = url.getParameter(Constants.SSL_PRIVATE_KEY);
            SslContextBuilder sslContextBuilder = null;
            if (detectSSL(certificate, privateKey)) {
                sslContextBuilder = SslContextBuilder.forServer(new File(certificate), new File(privateKey), url.getParameter(Constants.SSL_PRIVATE_KEY_PASSWORD));
            } else {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslContextBuilder = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey());
                logger.warn("generates a temporary self-signed certificate for testing purposes because of not found certificate or private key file.");
            }
            this.sslCtx = sslContextBuilder.sslProvider(OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK)
                    /* NOTE: the cipher filter may not include all ciphers required by the HTTP/2 specification. */
                    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                    .applicationProtocolConfig(new ApplicationProtocolConfig(
                            ApplicationProtocolConfig.Protocol.ALPN,
                            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                            ApplicationProtocolNames.HTTP_2,
                            ApplicationProtocolNames.HTTP_1_1)).build();
        }
    }

    private boolean detectSSL(String certificate, String privateKey) {
        return StringUtils.isNotEmpty(certificate)
                && StringUtils.isNotEmpty(privateKey)
                && Files.exists(Paths.get(certificate))
                && Files.exists(Paths.get(privateKey));
    }

    @Override
    public void initChannel(NioSocketChannel ch) {
        if (sslCtx != null) {
            configureSsl(ch);
        } else {
            configureClearText(ch);
        }
    }

    /**
     * Configure the pipeline for TLS NPN negotiation to HTTP/2.
     */
    private void configureSsl(SocketChannel ch) {
        ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()), new NettyHttp2NegotiationHandler(url));
    }

    /**
     * Configure the pipeline for a cleartext upgrade from HTTP to HTTP/2.0
     */
    private void configureClearText(SocketChannel ch) {

        final ChannelPipeline p = ch.pipeline();
        p.addLast(new HttpServerCodec());
        p.addLast(new HttpObjectAggregator(65536));
        p.addLast(new NettyHttp1ServerHandler());
    }

}
