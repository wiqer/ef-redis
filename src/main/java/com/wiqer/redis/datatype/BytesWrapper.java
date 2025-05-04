package com.wiqer.redis.datatype;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author lilan
 */
public class BytesWrapper implements Comparable<BytesWrapper> {
    static final Charset CHARSET = StandardCharsets.UTF_8;
    private final byte[] content;

    public BytesWrapper(byte[] content) {
        this.content = content;
    }

    public byte[] getByteArray() {
        return content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BytesWrapper that = (BytesWrapper) o;
        return Arrays.equals(content, that.content);
    }

    public String toUtf8String() {
        return new String(content, CHARSET);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(content);
    }


    @Override
    public int compareTo(BytesWrapper o) {
        final int len1 = content.length;
        final int len2 = o.getByteArray().length;
        final int lim = Math.min(len1, len2);
        byte[] v2 = o.getByteArray();

        int k = 0;
        while (k < lim) {
            final byte c1 = content[k];
            final byte c2 = v2[k];
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
        }
        return len1 - len2;
    }
}
