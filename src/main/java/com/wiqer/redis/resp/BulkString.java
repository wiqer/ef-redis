package com.wiqer.redis.resp;

import com.wiqer.redis.datatype.BytesWrapper;
import lombok.Getter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Getter
public class BulkString implements Resp
{
    public static final BulkString NullBulkString = new BulkString(null);
    static final        Charset    CHARSET        = StandardCharsets.UTF_8;
    BytesWrapper content;

    public BulkString(BytesWrapper content)
    {
        this.content = content;
    }

}
