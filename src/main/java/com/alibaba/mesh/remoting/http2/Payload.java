package com.alibaba.mesh.remoting.http2;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Stream;

/**
 * @author yiji
 */
public class Payload {

    private Http2Stream stream;
    private Http2Headers headers;
    private ByteBuf data;
    private boolean endOfStream;

    public Payload(Http2Stream stream, Http2Headers headers) {
        this.stream = stream;
        this.headers = headers;
        this.data = data;
    }

    public Payload stream(Http2Stream stream) {
        this.stream = stream;
        return this;
    }

    public Payload headers(Http2Headers headers) {
        this.headers = headers;
        return this;
    }

    public Payload data(ByteBuf data) {
        this.data = data;
        return this;
    }

    public Http2Stream stream() {
        return this.stream;
    }

    public Http2Headers headers() {
        return this.headers;
    }

    public ByteBuf data() {
        return this.data;
    }

    public Payload endOfStream(boolean endOfStream) {
        this.endOfStream = endOfStream;
        return this;
    }

    public boolean endOfStream() {
        return this.endOfStream;
    }
}
