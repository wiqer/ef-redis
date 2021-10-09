package com.wiqer.redis;

public interface RedisServer
{
    void start();

    void close();

    RedisCore getRedisCore();
}
