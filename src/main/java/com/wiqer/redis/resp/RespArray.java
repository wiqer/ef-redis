package com.wiqer.redis.resp;

public class RespArray implements Resp
{

    Resp[] array;

    public RespArray() {
    }

    public RespArray(Resp[] array)
    {
        this.array = array;
    }

    public Resp[] getArray()
    {
        return array;
    }
    public void setArray(Resp[] array)
    {
        this.array = array;
    }

    @Override
    public void clear() {
        array = null;
    }
}
