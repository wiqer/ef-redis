package com.wiqer.redis.resp;

import com.wiqer.redis.datatype.RedisBaseData;

/**
 * @author lilan
 */
public class SimpleString implements Resp
{
    public static  SimpleString OK =new SimpleString("OK");
    private        String       content;

    public SimpleString(String content)
    {
        this.content = content;
    }
    public SimpleString() {}
    public String getContent()
    {
        return content;
    }
    public void setContent(String content)
    {
        this.content = content;
    }
}
