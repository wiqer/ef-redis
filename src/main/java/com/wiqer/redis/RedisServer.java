package com.wiqer.redis;

public interface RedisServer
{
    void start(int port);

    void close();

    RedisCore getRedisCore();
}
