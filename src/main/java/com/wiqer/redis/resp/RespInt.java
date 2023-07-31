package com.wiqer.redis.resp;

public class RespInt implements Resp
{
    int value;

    public RespInt(Integer value)
    {
        this.value = value;
    }
    public RespInt(){}

    public int getValue()
    {
        return value;
    }
    public void getValue(int value)
    {
        this.value = value;
    }
}
