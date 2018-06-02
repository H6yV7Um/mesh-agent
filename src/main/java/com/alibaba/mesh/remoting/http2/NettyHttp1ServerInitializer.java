package com.alibaba.mesh.remoting.http2;

import com.alibaba.mesh.common.Constants;
import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.utils.StringUtils;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.CertificateException;

/**
 * @author yiji
 */
public class NettyHttp1ServerInitializer extends ChannelInitializer<NioSocketChannel> {

    private static final Logger logger = LoggerFactory.getLogger(NettyHttp1ServerInitializer.class);
    private SslContext sslCtx;
    private int maxHttpContentLength;
    private URL url;

    public NettyHttp1ServerInitializer(URL url) throws SSLException, CertificateException {
        this.url = url;
        this.maxHttpContentLength = url.getParameter(Constants.MAX_HTTP_CONTENT_BYTES_KEY, Constants.MAX_HTTP_CONTENT_BYTES);
    }

    private boolean detectSSL(String certificate, String privateKey) {
        return StringUtils.isNotEmpty(certificate)
                && StringUtils.isNotEmpty(privateKey)
                && Files.exists(Paths.get(certificate))
                && Files.exists(Paths.get(privateKey));
    }

    @Override
    public void initChannel(NioSocketChannel ch) {
        configureClearText(ch);
    }

    /**
     * Configure the pipeline for a cleartext upgrade from HTTP to HTTP/2.0
     */
    private void configureClearText(NioSocketChannel ch) {

        final ChannelPipeline p = ch.pipeline();
        p.addLast(new HttpServerCodec(4096, 8192, 8192, false));
        p.addLast(new HttpObjectAggregator(65536));
        p.addLast(new NettyHttp1ServerHandler());
    }

}
