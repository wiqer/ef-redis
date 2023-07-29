package com.wiqer.redis.datatype;

/**
 * @author lilan
 */
public interface RedisData extends RedisBaseData
{
    long timeout();

    void setTimeout(long timeout);
}
