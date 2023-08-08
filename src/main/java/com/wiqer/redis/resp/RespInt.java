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
    public void setValue(int value)
    {
        this.value = value;
    }

    private Long createdThreadId;

    public void setCreatedThreadId(Long createdThreadId){
        this.createdThreadId = createdThreadId;
    }
    @Override
    public Long getCreatedThreadId(){
        return createdThreadId;
    }
}
