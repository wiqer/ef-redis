package com.wiqer.redis.resp;

import com.wiqer.redis.datatype.BytesWrapper;
import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

public interface Resp
{

    static void write(Resp resp, ByteBuf buffer)
    {
        if (resp instanceof SimpleString)
        {
            buffer.writeByte((byte) '+');
            String content   = ((SimpleString) resp).getContent();
            char[] charArray = content.toCharArray();
            for (char each : charArray)
            {
                buffer.writeByte((byte) each);
            }
            buffer.writeByte((byte) '\r');
            buffer.writeByte((byte) '\n');
        }
        else if (resp instanceof Errors)
        {
            buffer.writeByte((byte) '-');
            String content   = ((Errors) resp).getContent();
            char[] charArray = content.toCharArray();
            for (char each : charArray)
            {
                buffer.writeByte((byte) each);
            }
            buffer.writeByte((byte) '\r');
            buffer.writeByte((byte) '\n');
        }
        else if (resp instanceof RespInt)
        {
            buffer.writeByte((byte) ':');
            String content   = String.valueOf(((RespInt) resp).getValue());
            char[] charArray = content.toCharArray();
            for (char each : charArray)
            {
                buffer.writeByte((byte) each);
            }
            buffer.writeByte((byte) '\r');
            buffer.writeByte((byte) '\n');
        }
        else if (resp instanceof BulkString)
        {
            buffer.writeByte((byte) '$');
            BytesWrapper content = ((BulkString) resp).getContent();
            if (content == null)
            {
                buffer.writeByte((byte) '-');
                buffer.writeByte((byte) '1');
                buffer.writeByte((byte) '\r');
                buffer.writeByte((byte) '\n');
            }
            else if (content.getByteArray().length == 0)
            {
                buffer.writeByte((byte) '0');
                buffer.writeByte((byte) '\r');
                buffer.writeByte((byte) '\n');
                buffer.writeByte((byte) '\r');
                buffer.writeByte((byte) '\n');
            }
            else
            {
                String length    = String.valueOf(content.getByteArray().length);
                char[] charArray = length.toCharArray();
                for (char each : charArray)
                {
                    buffer.writeByte((byte) each);
                }
                buffer.writeByte((byte) '\r');
                buffer.writeByte((byte) '\n');
                buffer.writeBytes(content.getByteArray());
                buffer.writeByte((byte) '\r');
                buffer.writeByte((byte) '\n');
            }
        }
        else if (resp instanceof RespArray)
        {
            buffer.writeByte((byte) '*');
            Resp[] array     = ((RespArray) resp).getArray();
            String length    = String.valueOf(array.length);
            char[] charArray = length.toCharArray();
            for (char each : charArray)
            {
                buffer.writeByte((byte) each);
            }
            buffer.writeByte((byte) '\r');
            buffer.writeByte((byte) '\n');
            for (Resp each : array)
            {
                write(each, buffer);
            }
        }
        else
        {
            throw new IllegalArgumentException()
                    ;
        }
    }

    static Resp decode(ByteBuf buffer)
    {
        char c = (char) buffer.readByte();
        if (c == '+')
        {
            return new SimpleString(getString(buffer));
        }
        else if (c == '-')
        {
            return new Errors(getString(buffer));
        }
        else if (c == ':')
        {
            int value = getNumber(buffer);
            return new RespInt(value);
        }
        else if (c == '$')
        {
            int length = getNumber(buffer);
            if (buffer.readableBytes() < length + 2)
            {
                throw new IllegalStateException("没有读取到完整的命令");
            }
            byte[] content;
            if (length == -1)
            {
                content = null;
            }
            else
            {
                content = new byte[length];
                buffer.readBytes(content);
            }
            if (buffer.readByte() != '\r' || buffer.readByte() != '\n')
            {
                throw new IllegalStateException("没有读取到完整的命令");
            }
            return new BulkString(new BytesWrapper(content));
        }
        else if (c == '*')
        {
            int    numOfElement = getNumber(buffer);
            Resp[] array        = new Resp[numOfElement];
            for (int i = 0; i < numOfElement; i++)
            {
                array[i] = decode(buffer);
            }
            return new RespArray(array);
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }
    static int getNumber(ByteBuf buffer)
    {
        char t;
        t = (char) buffer.readByte();
        boolean positive = true;
        int     value    = 0;
        // 错误（Errors）： 响应的首字节是 "-"
        if (t == '-')
        {
            positive = false;
        }
        else
        {
            value = t - '0';
        }
        while (buffer.readableBytes() > 0 && (t = (char) buffer.readByte()) != '\r')
        {
            value = value * 10 + (t - '0');
        }
        if (buffer.readableBytes() == 0 || buffer.readByte() != '\n')
        {
            throw new IllegalStateException("没有读取到完整的命令");
        }
        if (!positive)
        {
            value = -value;
        }
        return value;
    }

    static String getString(ByteBuf buffer)
    {
        char          c;
        StringBuilder builder = new StringBuilder();
        while (buffer.readableBytes() > 0 && (c = (char) buffer.readByte()) != '\r')
        {
            builder.append(c);
        }
        if (buffer.readableBytes() == 0 || buffer.readByte() != '\n')
        {
            throw new IllegalStateException("没有读取到完整的命令");
        }
        return builder.toString();
    }
}
