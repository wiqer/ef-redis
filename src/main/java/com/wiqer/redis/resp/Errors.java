package com.wiqer.redis.resp;

public class Errors implements Resp
{
    String content;

    public Errors(String content)
    {
        this.content = content;
    }

    public String getContent()
    {
        return content;
    }
}
