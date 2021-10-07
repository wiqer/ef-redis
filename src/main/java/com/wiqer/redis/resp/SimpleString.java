package com.wiqer.redis.resp;

public class SimpleString implements Resp
{
    public static final SimpleString OK = new SimpleString("OK");
    private final       String       content;

    public SimpleString(String content)
    {
        this.content = content;
    }

    public String getContent()
    {
        return content;
    }
}
