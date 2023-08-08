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
    private Long createdThreadId;

    public void setCreatedThreadId(Long createdThreadId){
        this.createdThreadId = createdThreadId;
    }
    @Override
    public Long getCreatedThreadId(){
        return createdThreadId;
    }

}
