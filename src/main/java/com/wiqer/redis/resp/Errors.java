package com.wiqer.redis.resp;

import lombok.Getter;

@Getter
public class Errors implements Resp
{
    String content;

    public Errors(String content)
    {
        this.content = content;
    }

}
