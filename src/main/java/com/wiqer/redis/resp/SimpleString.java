package com.wiqer.redis.resp;

import lombok.Getter;

/**
 * @author lilan
 */
@Getter
public class SimpleString implements Resp
{
    public static final SimpleString OK = new SimpleString("OK");
    private final       String       content;

    public SimpleString(String content)
    {
        this.content = content;
    }

}
