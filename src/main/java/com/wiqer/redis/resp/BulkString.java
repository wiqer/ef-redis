package com.wiqer.redis.resp;

import com.wiqer.redis.datatype.BytesWrapper;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class BulkString implements Resp
{
    public final static BulkString NullBulkString = new BulkString(null);
    static final        Charset    CHARSET        = StandardCharsets.UTF_8;
    BytesWrapper content;

    public BulkString() {
    }

    public BulkString(BytesWrapper content)
    {
        this.content = content;
    }

    public BytesWrapper getContent()
    {
        return content;
    }
    public void setContent(BytesWrapper content)
    {
        this.content = content;
    }
}
