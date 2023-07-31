package com.wiqer.redis.datatype;

/**
 * @author lilan
 */
public class RedisString implements RedisData
{
    public final static  RedisString ZERO =  new RedisString(BytesWrapper.ZERO);
    private volatile long timeout;

    private BytesWrapper value;
    public RedisString(BytesWrapper value){
        this.value = value;
        this.timeout = -1;
    }
    public RedisString(){
        this.timeout = -1;
    }
    public BytesWrapper getValue()
    {
        return value;
    }

    public void setValue(BytesWrapper value)
    {
        if(this.value != null){
            this.value.recovery();
        }
        this.value = value;
    }

    @Override
    public long timeout()
    {
        return timeout;
    }

    @Override
    public void setTimeout(long timeout)
    {
        this.timeout = timeout;
    }

    @Override
    public void clear() {
        this.value = null;
        this.timeout = -1;
    }

}
