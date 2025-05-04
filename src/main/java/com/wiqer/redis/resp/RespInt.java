package com.wiqer.redis.resp;

import lombok.Getter;

@Getter
public class RespInt implements Resp
{
    int value;

    public RespInt(int value)
    {
        this.value = value;
    }

}
