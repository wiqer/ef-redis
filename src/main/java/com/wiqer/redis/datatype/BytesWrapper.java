package com.wiqer.redis.datatype;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author lilan
 */
public class BytesWrapper implements Comparable<BytesWrapper>, RedisBaseData
{
    static final Charset CHARSET = StandardCharsets.UTF_8;
    public static final BytesWrapper ZERO = new BytesWrapper("0".getBytes(CHARSET));
    public static final BytesWrapper ONE = new BytesWrapper("1".getBytes(CHARSET));
    private  byte[] content;

    public BytesWrapper() {
    }

    public BytesWrapper(byte[] content) {this.content = content;}

    public byte[] getByteArray()
    {
        return content;
    }

    public void setByteArray(byte[] content) {this.content = content;}

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


    @Override
    public int compareTo(BytesWrapper o) {
        int len1 = content.length;
        int len2 = o.getByteArray().length;
        int lim = Math.min(len1, len2);
        byte v1[] = content;
        byte v2[] = o.getByteArray();

        int k = 0;
        while (k < lim) {
            byte c1 = v1[k];
            byte c2 = v2[k];
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
        }
        return len1 - len2;
    }

    @Override
    public void clear() {
        content = null;
    }

}
