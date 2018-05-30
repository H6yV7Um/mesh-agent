package com.alibaba.mesh.common.serialize.fastjson;

import com.alibaba.mesh.common.serialize.ObjectInput;

import io.netty.buffer.ByteBuf;
import io.netty.util.ByteProcessor;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

public class FastJsonObjectInput implements ObjectInput {

    private final ByteBuf reader;

    public static final Charset ascii = Charset.forName("US-ASCII");
    public static final byte lineSeparator = System.getProperty("line.separator").getBytes(ascii)[0];
    public static final byte doubleQuotation = "\"".getBytes(ascii)[0];

    public FastJsonObjectInput(ByteBuf reader) {
        this.reader = reader;
    }

    @Override
    public boolean readBool() throws IOException {
        boolean v = reader.readBoolean();
        reader.readerIndex(reader.readerIndex() + 1);
        //verifyLine();
        return v;
    }

    @Override
    public byte readByte() throws IOException {
        byte v = reader.readByte();
        //verifyLine();
        reader.readerIndex(reader.readerIndex() + 1);
        return v;
    }

    @Override
    public short readShort() throws IOException {
        short v = reader.readShort();
        // verifyLine();
        reader.readerIndex(reader.readerIndex() + 1);
        return v;
    }

    @Override
    public int readInt() throws IOException {
        int v = reader.readInt();
        // verifyLine();
        reader.readerIndex(reader.readerIndex() + 1);
        return v;
    }

    @Override
    public long readLong() throws IOException {
        long v = reader.readLong();
        // verifyLine();
        reader.readerIndex(reader.readerIndex() + 1);
        return v;
    }

    @Override
    public float readFloat() throws IOException {
        float v = reader.readFloat();
        // verifyLine();
        reader.readerIndex(reader.readerIndex() + 1);
        return v;
    }

    @Override
    public double readDouble() throws IOException {
        double v = reader.readDouble();
        // verifyLine();
        reader.readerIndex(reader.readerIndex() + 1);
        return v;
    }

    @Override
    public String readUTF() throws IOException {
        // verifyQuotation();
        reader.readerIndex(reader.readerIndex() + 1);
        int i = reader.forEachByte(reader.readerIndex(), reader.readableBytes(), new ByteProcessor() {
            byte prev;

            @Override
            public boolean process(byte value) throws Exception {
                if (prev == doubleQuotation && value == lineSeparator)
                    return false;
                prev = value;
                return true;
            }
        });
        String v = reader.readCharSequence(i - reader.readerIndex() - 1, ascii).toString();
        // verifyQuotation();
        // verifyLine();
        reader.readerIndex(reader.readerIndex() + 2);
        return v;
    }

    @Override
    public byte[] readBytes() throws IOException {
        int i = reader.forEachByte(reader.readerIndex(), reader.readableBytes(), new ByteProcessor() {
            @Override
            public boolean process(byte value) throws Exception {
                if (value == lineSeparator)
                    return false;
                return true;
            }
        });
        byte[] bytes = new byte[i - reader.readerIndex() - 1];
        reader.readBytes(bytes, 0, bytes.length);
        // verifyLine();
        reader.readerIndex(reader.readerIndex() + 1);
        return bytes;
    }

    @Override
    public Object readObject() throws IOException, ClassNotFoundException {

        reader.markReaderIndex();
        byte b = reader.readByte();
        // for string
        if (b == doubleQuotation) {
            int i = reader.forEachByte(reader.readerIndex(), reader.readableBytes(), new ByteProcessor() {
                byte prev;

                @Override
                public boolean process(byte value) throws Exception {
                    if (prev == doubleQuotation && value == lineSeparator)
                        return false;
                    prev = value;
                    return true;
                }
            });
            String v = reader.readCharSequence(i - reader.readerIndex() - 1, ascii).toString();
            // verifyQuotation();
            // verifyLine();
            reader.readerIndex(reader.readerIndex() + 2);
            return v;
        }

        reader.resetReaderIndex();
        int i = reader.forEachByte(reader.readerIndex(), reader.readableBytes(), new ByteProcessor() {
            @Override
            public boolean process(byte value) throws Exception {
                if (value == lineSeparator)
                    return false;
                return true;
            }
        });
        String v = reader.readCharSequence(i - reader.readerIndex(), ascii).toString();
        // verifyLine();
        reader.readerIndex(reader.readerIndex() + 1);
        return v;
    }

    @Override
    public <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException {
        return (T) readObject();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> cls, Type type) throws IOException, ClassNotFoundException {
        return (T) readObject();
    }

    private void verifyLine() {
        byte b = reader.readByte();
        if (lineSeparator != b) {
            throw new RuntimeException("Failed to deserialize object, expected '"
                    + lineSeparator + "'" + " actual '" + b + "'");
        }
    }

    private void verifyQuotation() {
        byte b = reader.readByte();
        if (doubleQuotation != b) {
            throw new RuntimeException("Failed to deserialize object, expected '"
                    + doubleQuotation + "'" + " actual '" + b + "'");
        }
    }
}
