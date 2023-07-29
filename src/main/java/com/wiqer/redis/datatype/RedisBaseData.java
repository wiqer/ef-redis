package com.wiqer.redis.datatype;

import com.wiqer.redis.memory.RedisCache;

/**
 * @author lilan
 */
public interface RedisBaseData
{
    RedisCache REDIS_CACHE = new RedisCache();

    void clear();

    default void recovery() {
        clear();
        REDIS_CACHE.addRedisDataToCache(this);
    }
}
