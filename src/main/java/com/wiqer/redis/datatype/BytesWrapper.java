package com.wiqer.redis.datatype;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author lilan
 */
public class BytesWrapper
{
    static final Charset CHARSET = StandardCharsets.UTF_8;
    private final byte[] content;

    public BytesWrapper(byte[] content) {this.content = content;}

    public byte[] getByteArray()
    {
        return content;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        BytesWrapper that = (BytesWrapper) o;
        return Arrays.equals(content, that.content);
    }

    public String toUtf8String()
    {
        return new String(content, CHARSET);
    }

    @Override
    public int hashCode()
    {
        return Arrays.hashCode(content);
    }
}
