package com.wiqer.redis.resp;

public class Errors implements Resp
{
    String content;

    public Errors() {
    }

    public Errors(String content)
    {
        this.content = content;
    }

    public String getContent()
    {
        return content;
    }
    public void setContent(String content)
    {
        this.content = content;
    }
    @Override
    public void clear() {

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
