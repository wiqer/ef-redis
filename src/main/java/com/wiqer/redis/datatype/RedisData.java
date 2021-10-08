package com.wiqer.redis.datatype;

/**
 * @author lilan
 */
public interface RedisData
{
    long timeout();

    void setTimeout(long timeout);
}
