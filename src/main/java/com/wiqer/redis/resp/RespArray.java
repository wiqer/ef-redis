package com.wiqer.redis.resp;

import lombok.Getter;

@Getter
public class RespArray implements Resp
{

    Resp[] array;

    public RespArray(Resp[] array)
    {
        this.array = array;
    }

}
