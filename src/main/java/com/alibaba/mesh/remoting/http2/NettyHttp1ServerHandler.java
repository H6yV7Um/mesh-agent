package com.alibaba.mesh.remoting.http2;

import com.alibaba.mesh.remoting.WriteQueue;
import com.alibaba.mesh.remoting.exchange.Response;
import com.alibaba.mesh.remoting.exchange.ResponseCallback;
import com.alibaba.mesh.rpc.RpcContext;
import com.alibaba.mesh.rpc.RpcResult;
import com.alibaba.mesh.rpc.service.GenericService;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.ByteProcessor;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author yiji
 */
public class NettyHttp1ServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger logger = LoggerFactory.getLogger(NettyHttp1ServerHandler.class);

    private String establishApproach;

    private WriteQueue responseQueue;

    private GenericService delegate;

    static Charset utf8 = Charset.forName("utf-8");

    static String[] parameterType = new String[]{"Ljava/lang/String;"};

    private Thread monitor;

    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    public static AtomicInteger received = new AtomicInteger();
    public static AtomicInteger responsed = new AtomicInteger();

    public NettyHttp1ServerHandler() {
        // this.establishApproach = "Support http/2 protocol only, please upgrade your http client.";
        this.monitor = new Thread("monitor-endpoint-received.") {
            @Override
            public void run() {
                if (responseQueue.queue != null && responseQueue.queue.size() > 0)
                    System.out.println("response queue size: " + responseQueue.queue.size());
            }
        };

        scheduledExecutorService.scheduleAtFixedRate(this.monitor, 0, 50, TimeUnit.MILLISECONDS);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.responseQueue = new WriteQueue(ctx.channel());
        this.delegate = BeanLookup.find(GenericService.class, "delegate");
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        if (HttpUtil.is100ContinueExpected(request)) {
            ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
        }

        ByteBuf body = request.content();

        if (body.isReadable()) {
            int readableBytes = body.readableBytes();
            int index = body.readerIndex();

//            String str = decodeString(body, body.readerIndex(), body.readableBytes(), Charset.defaultCharset());
//
//            String value = URLDecoder.decode(str);
//
//            value = value.substring(value.indexOf("parameter=") + 10);

            int i = body.forEachByte(new ByteProcessor() {

                byte p, a, r, a0, m, e, t, e0, r0;
                boolean next;

                int offset = 0;

                @Override
                public boolean process(byte b) throws Exception {

                    if (++offset < 5) return true;

                    p = a;
                    a = r;
                    r = a0;
                    a0 = m;
                    m = e;
                    e = t;
                    t = e0;
                    e0 = r0;
                    r0 = b;

                    if (p == 'p'
                            && a == 'a'
                            && r == 'r'
                            && a0 == 'a'
                            && m == 'm'
                            && e == 'e'
                            && t == 't'
                            && e0 == 'e'
                            && b == 'r') {
                        if (next) {
                            return false;
                        }
                        next = true;
                    }

                    return true;
                }
            });

            body.readerIndex(i + 2);

            Object[] parameterValue = new Object[1];

            parameterValue[0] = body.readCharSequence(body.readableBytes(), utf8);

            if (logger.isDebugEnabled()) {
                logger.debug("received parameter:" + parameterValue[0]);
            }

//            if(!parameterValue[0].equals(value)){
//                throw new IllegalArgumentException("expected args:" + value +
//                "   actual: " + parameterValue[0]);
//            }

            // internel alreay used queue, we alse use response queue
            delegate.$invoke("hash", parameterType, parameterValue);
            received.incrementAndGet();
            RpcContext.getContext().getResponseFuture()
                    .setCallback(new ResponseCallbackImpl(ctx, request, (String) parameterValue[0]));
        }
    }

    private static final FastThreadLocal<CharBuffer> CHAR_BUFFERS0 = new FastThreadLocal<CharBuffer>() {
        @Override
        protected CharBuffer initialValue() throws Exception {
            return CharBuffer.allocate(1024);
        }
    };

    public static String decodeString(ByteBuf src, int readerIndex, int len, Charset charset) {
        if (len == 0) {
            return StringUtil.EMPTY_STRING;
        }
        final CharsetDecoder decoder = CharsetUtil.decoder(charset);
        final int maxLength = (int) ((double) len * decoder.maxCharsPerByte());
        CharBuffer dst = CHAR_BUFFERS0.get();
        if (dst.length() < maxLength) {
            dst = CharBuffer.allocate(maxLength);
            if (maxLength <= 16 * 1024) {
                CHAR_BUFFERS0.set(dst);
            }
        } else {
            dst.clear();
        }
        if (src.nioBufferCount() == 1) {
            decodeString(decoder, src.nioBuffer(readerIndex, len), dst);
        } else {
            // We use a heap buffer as CharsetDecoder is most likely able to use a fast-path if src and dst buffers
            // are both backed by a byte array.
            ByteBuf buffer = src.alloc().heapBuffer(len);
            try {
                buffer.writeBytes(src, readerIndex, len);
                // Use internalNioBuffer(...) to reduce object creation.
                decodeString(decoder, buffer.internalNioBuffer(buffer.readerIndex(), len), dst);
            } finally {
                // Release the temporary buffer again.
                buffer.release();
            }
        }
        return dst.flip().toString();
    }

    private static void decodeString(CharsetDecoder decoder, ByteBuffer src, CharBuffer dst) {
        try {
            CoderResult cr = decoder.decode(src, dst, true);
            if (!cr.isUnderflow()) {
                cr.throwException();
            }
            cr = decoder.flush(dst);
            if (!cr.isUnderflow()) {
                cr.throwException();
            }
        } catch (CharacterCodingException x) {
            throw new IllegalStateException(x);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        // writeQueue.scheduleFlush();
    }

    class InvokeMethodResponseCommand implements WriteQueue.QueuedCommand {

        ChannelHandlerContext ctx;
        FullHttpRequest request;
        ChannelPromise promise;

        FullHttpResponse response;

        String parameter;

        public InvokeMethodResponseCommand(ChannelHandlerContext ctx,
                                           FullHttpRequest request,
                                           @Nonnull ChannelPromise promise,
                                           FullHttpResponse response,
                                           String parameter) {
            this.ctx = ctx;
            this.request = request;
            this.promise = promise;
            this.response = response;
            this.parameter = parameter;
        }

        /**
         * Returns the promise beeing notified of the success/failure of the write.
         */
        @Override
        public ChannelPromise promise() {
            return promise;
        }

        /**
         * Sets the promise.
         *
         * @param promise
         */
        @Override
        public void promise(ChannelPromise promise) {
            promise = promise;
        }

        @Override
        public void run(Channel channel) {
            channel.write(response, promise);
        }
    }

    public final class ResponseCallbackImpl implements ResponseCallback {

        ChannelHandlerContext ctx;
        FullHttpRequest request;
        String parameter;

        public ResponseCallbackImpl(ChannelHandlerContext ctx,
                                    FullHttpRequest request,
                                    String parameter) {
            this.ctx = ctx;
            this.request = request;
            this.parameter = parameter;
        }

        @Override
        public void done(Object result) {

            if(result instanceof Response){
                Response response = (Response) result;

                Object ret = response.getResult();

                if(ret instanceof RpcResult){

                    RpcResult r = (RpcResult)ret;

                    responsed.incrementAndGet();

                    if (r.getValue() == null) {
                        logger.error("http1 response received null value!!");
                        return;
                    }

                    // response to http
                    boolean error = !Objects.equals(parameter.hashCode(), r.getValue());

                    ByteBuf payload = ctx.alloc().buffer();
                    String result0 = String.valueOf(r.getValue());
                    payload.writeCharSequence(result0, utf8);
                    FullHttpResponse httpResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, payload);
                    httpResponse.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
                    httpResponse.headers().set(CONNECTION, KEEP_ALIVE);
                    httpResponse.headers().setInt(CONTENT_LENGTH, payload.readableBytes());

                    if (error) {
                        logger.error("Http response send error !!!  expected: " + parameter.hashCode() + ", actual:" + r.getValue()
                                + "\n parameter:" + parameter);
                    }

                    NettyHttp1ServerHandler.this.responseQueue.enqueue(new InvokeMethodResponseCommand(ctx, request, ctx.voidPromise(), httpResponse, parameter), true);

                }else{
                    logger.warn("callback received(non rpc result):" + ret.toString());
                }
            }else {
                logger.warn("callback received:" + result.toString());
            }

        }

        @Override
        public void caught(Throwable exception) {
            logger.error("unkonwn error", exception);
        }
    }

}
