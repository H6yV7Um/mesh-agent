package com.alibaba.mesh.common.serialize.fastjson;

import com.alibaba.mesh.common.serialize.ObjectOutput;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;

public class FastJsonObjectOutput implements ObjectOutput {

    private final ByteBuf writer;

    public static final Charset ascii = Charset.forName("US-ASCII");
    public static final byte lineSeparator = System.getProperty("line.separator").getBytes(ascii)[0];
    public static final byte doubleQuotation = "\"".getBytes(ascii)[0];
    public static final byte lBracket = "{".getBytes(ascii)[0];
    public static final byte rBracket = "}".getBytes(ascii)[0];
    public static final byte comma = ",".getBytes(ascii)[0];
    public static final byte colon = ":".getBytes(ascii)[0];

    public FastJsonObjectOutput(ByteBuf output) {
        this.writer = output;
    }

    @Override
    public void writeBool(boolean v) throws IOException {
        writer.writeBoolean(v);
        writer.writeByte(lineSeparator);
    }

    @Override
    public void writeByte(byte v) throws IOException {
        writer.writeByte(v);
        writer.writeByte(lineSeparator);
    }

    @Override
    public void writeShort(short v) throws IOException {
        writer.writeShort(v);
        writer.writeByte(lineSeparator);
    }

    @Override
    public void writeInt(int v) throws IOException {
        writer.writeInt(v);
        writer.writeByte(lineSeparator);
    }

    @Override
    public void writeLong(long v) throws IOException {
        writer.writeLong(v);
        writer.writeByte(lineSeparator);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        writer.writeFloat(v);
        writer.writeByte(lineSeparator);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        writer.writeDouble(v);
        writer.writeByte(lineSeparator);
    }

    @Override
    public void writeUTF(String v) throws IOException {
        writer.writeByte(doubleQuotation);
        writer.writeCharSequence(v, ascii);
        writer.writeByte(doubleQuotation);
        writer.writeByte(lineSeparator);
    }

    @Override
    public void writeBytes(byte[] b) throws IOException {
        writer.writeBytes(b);
        writer.writeByte(lineSeparator);
    }

    @Override
    public void writeBytes(byte[] b, int off, int len) throws IOException {
        writer.writeBytes(b, off, len);
        writer.writeByte(lineSeparator);
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        Class<?> clazz = obj.getClass();
        if (Map.class.isAssignableFrom(clazz)) {
            writer.writeByte(lBracket);
            {
                Map map = (Map) obj;

                Iterator iterator = map.entrySet().iterator();
                int index = 0, size = map.size();
                while (iterator.hasNext()) {
                    Map.Entry<String, String> item = (Map.Entry<String, String>) iterator.next();

                    writer.writeByte(doubleQuotation);
                    writer.writeCharSequence(item.getKey(), ascii);
                    writer.writeByte(doubleQuotation);

                    writer.writeByte(colon);

                    writer.writeByte(doubleQuotation);
                    writer.writeCharSequence(item.getValue(), ascii);
                    writer.writeByte(doubleQuotation);

                    ++index;
                    if (index > 0 && index < size) {
                        writer.writeByte(comma);
                    }
                }
            }
            writer.writeByte(rBracket);
        } else if (String.class.isAssignableFrom(clazz)) {
            String value = (String) obj;
            writer.writeByte(doubleQuotation);
            writer.writeCharSequence(value, ascii);
            writer.writeByte(doubleQuotation);
            writer.writeByte(lineSeparator);
        }
    }

    @Override
    public void flushBuffer() throws IOException {
        // writer.flush();
    }

}
